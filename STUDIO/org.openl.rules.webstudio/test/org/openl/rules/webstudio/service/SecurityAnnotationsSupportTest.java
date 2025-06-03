package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import org.openl.security.acl.permission.AclPermission;

@SpringJUnitConfig(classes = {DBTestConfiguration.class, AclServiceTestConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3"})
public class SecurityAnnotationsSupportTest {
    @Autowired
    MutableAclService aclService;

    @Autowired
    SecuredService securedService;

    @Test
    @WithMockUser(value = "oleg")
    public void hasRoleAnnotationsDenyTest() {
        assertThrows(AccessDeniedException.class, () -> {
            assertNotNull(securedService);
            securedService.save(new Foo(44L));
        });
    }

    @Test
    @WithMockUser(value = "oleg", authorities = "Developers")
    public void hasRoleAnnotationsAllowTest() {
        assertNotNull(securedService);
        securedService.save(new Foo(44L));
    }

    @Test
    @WithMockUser(value = "oleg")
    @Transactional
    public void hasPermissionAnnotationsTest() {
        assertNotNull(securedService);
        Foo foo = new Foo(45L);
        // Prepare the information we'd like in our access control entry (ACE)
        ObjectIdentity oi = new ObjectIdentityImpl(foo);

        // Create or update the relevant ACL
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        // Now grant some permissions via an access control entry (ACE)
        Sid sid = new PrincipalSid("oleg");
        acl.insertAce(acl.getEntries().size(), AclPermission.READ, sid, false);
        aclService.updateAcl(acl);
        try {
            securedService.read(foo);
            fail("Expected access deny exception");
        } catch (AccessDeniedException ignored) {
        }
        acl.deleteAce(acl.getEntries().size() - 1);
        acl.insertAce(acl.getEntries().size(), AclPermission.READ, sid, true);
        aclService.updateAcl(acl);
        securedService.read(foo);
    }
}
