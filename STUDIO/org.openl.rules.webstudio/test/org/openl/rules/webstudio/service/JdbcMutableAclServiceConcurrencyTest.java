package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.openl.security.acl.JdbcMutableAclService;

/**
 * Tests that concurrent ACL operations for the same SID on different projects
 * do not fail with DuplicateKeyException.
 *
 * @see JdbcMutableAclService#createOrRetrieveSidPrimaryKey(String, boolean, boolean)
 */
@SpringJUnitConfig(classes = {DBTestConfiguration.class, AclServiceTestConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:concurrency;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 10"})
class JdbcMutableAclServiceConcurrencyTest {

    @Autowired
    JdbcMutableAclService aclService;

    @Autowired
    PlatformTransactionManager txManager;

    /**
     * Simulates the real-world scenario: multiple async requests grant permissions
     * for the same user on different projects simultaneously. All threads try to
     * create the same SID row in acl_sid, triggering the race condition that
     * {@link JdbcMutableAclService#createOrRetrieveSidPrimaryKey} now handles.
     */
    @Test
    @WithMockUser(value = "admin", authorities = "Administrators")
    void concurrentGrantPermissionsForSameSid() throws Exception {
        int threadCount = 5;
        String runId = UUID.randomUUID().toString();
        Sid sid = new PrincipalSid("newUser-" + runId);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                final String projectId = "project-" + runId + "-" + i;
                futures.add(executor.submit(() -> {
                    // Propagate security context to child thread (each thread gets its own instance)
                    var ctx = SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(authentication);
                    SecurityContextHolder.setContext(ctx);
                    try {
                        // Wait until all threads are ready to maximize contention
                        assertDoesNotThrow(() -> barrier.await());

                        TransactionTemplate tx = new TransactionTemplate(txManager);
                        tx.execute(status -> {
                            ObjectIdentity oi = new ObjectIdentityImpl(Foo.class, projectId);
                            MutableAcl acl = aclService.createAcl(oi);
                            acl.insertAce(0, BasePermission.READ, sid, true);
                            aclService.updateAcl(acl);
                            return null;
                        });
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            }

            // Collect results — any DuplicateKeyException would surface here
            for (Future<?> future : futures) {
                assertDoesNotThrow(() -> future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        // Verify all ACLs were created successfully
        for (int i = 0; i < threadCount; i++) {
            ObjectIdentity oi = new ObjectIdentityImpl(Foo.class, "project-" + runId + "-" + i);
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.setReadOnly(true);
            MutableAcl acl = tx.execute(status -> (MutableAcl) aclService.readAclById(oi));
            assertEquals(1, acl.getEntries().size());
        }
    }

    /**
     * Verifies that concurrent grant for the same PrincipalSid on many projects
     * with higher thread count does not cause issues.
     */
    @Test
    @WithMockUser(value = "admin", authorities = "Administrators")
    void concurrentGrantPermissionsForMultipleSids() throws Exception {
        int threadCount = 8;
        String runId = UUID.randomUUID().toString();
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                final String projectId = "multi-project-" + runId + "-" + i;
                futures.add(executor.submit(() -> {
                    var ctx = SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(authentication);
                    SecurityContextHolder.setContext(ctx);
                    try {
                        assertDoesNotThrow(() -> barrier.await());

                        TransactionTemplate tx = new TransactionTemplate(txManager);
                        tx.execute(status -> {
                            ObjectIdentity oi = new ObjectIdentityImpl(Foo.class, projectId);
                            MutableAcl acl = aclService.createAcl(oi);
                            Sid sid = new PrincipalSid("sharedUser-" + runId);
                            acl.insertAce(0, BasePermission.WRITE, sid, true);
                            aclService.updateAcl(acl);
                            return null;
                        });
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            }

            for (Future<?> future : futures) {
                assertDoesNotThrow(() -> future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        // Verify all ACLs have the correct entry
        for (int i = 0; i < threadCount; i++) {
            ObjectIdentity oi = new ObjectIdentityImpl(Foo.class, "multi-project-" + runId + "-" + i);
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.setReadOnly(true);
            MutableAcl acl = tx.execute(status -> (MutableAcl) aclService.readAclById(oi));
            assertEquals(1, acl.getEntries().size());
        }
    }
}
