package org.openl.rules.rest.acl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.model.AccessControlEntry;
import org.openl.rules.rest.acl.model.AclResourceAccess;
import org.openl.rules.rest.acl.model.AclResourceRef;
import org.openl.rules.rest.acl.model.AclSubject;
import org.openl.rules.rest.acl.model.BulkAclOverwriteRequest;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.security.acl.JdbcSidManagingAclService;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.studio.audit.model.AuditEvent;

@ExtendWith(MockitoExtension.class)
class BulkAclOverwriteServiceImplTest {

    private static final PrincipalSid JDOE = new PrincipalSid("jdoe");
    private static final GrantedAuthoritySid TESTERS = new GrantedAuthoritySid("Testers");

    @Mock
    private UserManagementService userManagementService;
    @Mock
    private GroupManagementService groupManagementService;
    @Mock
    private RepositoryAclServiceProvider aclServiceProvider;
    @Mock
    private RepositoryAclService designAclService;
    @Mock
    private SimpleRepositoryAclService prodAclService;
    @Mock
    private LockRegistry lockRegistry;
    @Mock
    private SecureDesignTimeRepository designTimeRepository;
    @Mock
    private SecureDeploymentRepositoryService deploymentRepositoryService;
    @Mock
    private JdbcSidManagingAclService aclService;

    @Test
    void overwrite_announcesTheRolesItChanged() throws InterruptedException {
        var lock = mock(Lock.class);
        when(lockRegistry.obtain("bulk-acl-overwrite-lock")).thenReturn(lock);
        when(lock.tryLock(30, TimeUnit.SECONDS)).thenReturn(true);
        when(groupManagementService.getGroupNames()).thenReturn(Set.of("Testers", "Analysts"));
        when(userManagementService.getUserNames()).thenReturn(Set.of("jdoe"));
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(designAclService);
        when(aclServiceProvider.getProdRepoAclService()).thenReturn(prodAclService);
        when(aclServiceProvider.getAclService("design")).thenReturn(designAclService);

        var designRepository = mock(Repository.class);
        when(designRepository.getId()).thenReturn("design");
        when(designRepository.supports()).thenReturn(new FeaturesBuilder(designRepository).build());
        when(designTimeRepository.getRepositories()).thenReturn(List.of(designRepository));
        var production = mock(RepositoryConfiguration.class);
        when(production.getId()).thenReturn("production");
        when(deploymentRepositoryService.getRepositories()).thenReturn(List.of(production));
        var project = mock(AProject.class);
        when(project.getBusinessName()).thenReturn("Pricing");
        when(project.getRepository()).thenReturn(designRepository);
        doReturn(List.of(project)).when(designTimeRepository).getProjects();

        // the roles before the overwrite, then after it
        when(designAclService.listRootPermissions()).thenReturn(roles(TESTERS, AclRole.VIEWER),
                roles(TESTERS, AclRole.CONTRIBUTOR));
        when(prodAclService.listRootPermissions()).thenReturn(Map.of(), Map.of());
        when(designAclService.listPermissions("design", null)).thenReturn(roles(JDOE, AclRole.MANAGER), Map.of());
        when(prodAclService.listPermissions("production", null)).thenReturn(Map.of(), Map.of());
        when(designAclService.listPermissions(project)).thenReturn(roles(JDOE, AclRole.MANAGER),
                roles(JDOE, AclRole.MANAGER));

        var events = new ArrayList<AuditEvent>();

        service(events).process(new BulkAclOverwriteRequest(List.of(
                access(AclResourceRef.builder().build(), "Testers", false, AclRole.CONTRIBUTOR),
                access(AclResourceRef.builder().repositoryId("design").projectName("Pricing").build(), "jdoe",
                        true, AclRole.MANAGER))));

        verify(aclService).deleteSid(new GrantedAuthoritySid("Analysts"));
        verify(designAclService).removePermissions(project);
        assertEquals(List.of("ROLE_CHANGED design Testers {from=VIEWER, to=CONTRIBUTOR}",
                "ROLE_REVOKED design jdoe {type=design, from=MANAGER}"), announced(events));
    }

    @Test
    void overwrite_tellsApartTheProjectsOfOneNameByTheirFolders() throws InterruptedException {
        var lock = mock(Lock.class);
        when(lockRegistry.obtain("bulk-acl-overwrite-lock")).thenReturn(lock);
        when(lock.tryLock(30, TimeUnit.SECONDS)).thenReturn(true);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(designAclService);
        when(aclServiceProvider.getProdRepoAclService()).thenReturn(prodAclService);
        var designRepository = mock(Repository.class);
        when(designRepository.getId()).thenReturn("design");
        when(designRepository.supports())
                .thenReturn(new FeaturesBuilder(designRepository).setMappedFolders(true).build());
        var sales = project("Pricing", designRepository, "sales/Pricing");
        var claims = project("Pricing", designRepository, "claims/Pricing");
        doReturn(List.of(sales, claims)).when(designTimeRepository).getProjects();
        when(designAclService.listPermissions(sales)).thenReturn(roles(JDOE, AclRole.VIEWER), Map.of());
        when(designAclService.listPermissions(claims)).thenReturn(roles(TESTERS, AclRole.MANAGER), Map.of());

        var events = new ArrayList<AuditEvent>();
        service(events).process(new BulkAclOverwriteRequest(List.of()));

        assertEquals(List.of("ROLE_REVOKED Pricing Testers {repository=design, path=claims/Pricing, from=MANAGER}",
                "ROLE_REVOKED Pricing jdoe {repository=design, path=sales/Pricing, from=VIEWER}"), announced(events));
    }

    private BulkAclOverwriteServiceImpl service(List<AuditEvent> events) {
        return new BulkAclOverwriteServiceImpl(userManagementService,
                groupManagementService,
                aclServiceProvider,
                lockRegistry,
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                designTimeRepository,
                deploymentRepositoryService,
                aclService,
                new AclRoleAuditor(event -> events.add((AuditEvent) event)));
    }

    private static AProject project(String name, Repository repository, String path) {
        var project = mock(AProject.class);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getRepository()).thenReturn(repository);
        when(project.getRealPath()).thenReturn(path);
        return project;
    }

    private static List<String> announced(List<AuditEvent> events) {
        return events.stream()
                .map(event -> "%s %s %s %s".formatted(event.action(), event.target(), event.subject(),
                        event.details()))
                .sorted()
                .toList();
    }

    private static Map<Sid, List<Permission>> roles(Sid sid, AclRole role) {
        return Map.of(sid, List.of(role.getCumulativePermission()));
    }

    private static AclResourceAccess access(AclResourceRef ref, String sid, boolean principal, AclRole role) {
        return AclResourceAccess.builder()
                .resourceRef(ref)
                .aces(List.of(AccessControlEntry.builder()
                        .sub(AclSubject.builder().sid(sid).principal(principal).build())
                        .role(role)
                        .build()))
                .build();
    }
}
