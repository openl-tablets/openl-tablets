package org.openl.rules.rest.acl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.transaction.PlatformTransactionManager;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.rest.acl.model.SetAclRoleModel;
import org.openl.rules.rest.acl.service.AclRoleAuditor;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.security.CurrentUserInfo;

/**
 * The roles the ACL controllers give and take away are announced to the audit log.
 */
@ExtendWith(MockitoExtension.class)
class AclRoleChangesAuditTest {

    private static final PrincipalSid JDOE = new PrincipalSid("jdoe");
    private static final GrantedAuthoritySid TESTERS = new GrantedAuthoritySid("Testers");

    @Mock
    private RepositoryAclServiceProvider aclServiceProvider;
    @Mock
    private RepositoryAclService designAclService;
    @Mock
    private SimpleRepositoryAclService prodAclService;
    @Mock
    private PlatformTransactionManager txManager;

    private final List<AuditEvent> events = new ArrayList<>();
    private AclProjectsController projectsController;
    private AclRepositoriesController repositoriesController;

    @BeforeEach
    void setUp() {
        var auditor = new AclRoleAuditor(event -> events.add((AuditEvent) event));
        projectsController = new AclProjectsController(mock(SecureDesignTimeRepository.class),
                aclServiceProvider,
                mock(UserManagementService.class),
                mock(GroupManagementService.class),
                txManager,
                mock(CurrentUserInfo.class),
                auditor);
        repositoriesController = new AclRepositoriesController(aclServiceProvider,
                txManager,
                mock(SecureDesignTimeRepository.class),
                mock(SecureDeploymentRepositoryService.class),
                auditor);
    }

    @Test
    void projectRole_changedInOneTransaction() {
        var project = project();
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(designAclService);
        when(designAclService.listPermissions(project, TESTERS))
                .thenReturn(List.of(AclRole.VIEWER.getCumulativePermission()));

        projectsController.updateAclProjectRulesForSid(project, TESTERS, new SetAclRoleModel(AclRole.CONTRIBUTOR));

        var order = inOrder(txManager, designAclService);
        order.verify(txManager).getTransaction(any());
        order.verify(designAclService).removePermissions(project, TESTERS);
        order.verify(designAclService).addPermissions(project, TESTERS,
                AclRole.CONTRIBUTOR.getCumulativePermission());
        order.verify(txManager).commit(any());
        var event = single(AuditAction.ROLE_CHANGED);
        assertEquals(AuditTargetType.PROJECT, event.targetType());
        assertEquals("Pricing", event.target());
        assertEquals("Testers", event.subject());
        assertEquals(Map.of("repository", "design", "from", "VIEWER", "to", "CONTRIBUTOR"), event.details());
    }

    @Test
    void projectRole_revoked() {
        var project = project();
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(designAclService);
        when(designAclService.listPermissions(project, JDOE))
                .thenReturn(List.of(AclRole.MANAGER.getCumulativePermission()));

        projectsController.deleteAclProjectRulesForSid(project, JDOE);

        var event = single(AuditAction.ROLE_REVOKED);
        assertEquals("jdoe", event.subject());
        assertEquals(Map.of("repository", "design", "from", "MANAGER"), event.details());
    }

    @Test
    void projectRole_revokedFromNobody_announcesNothing() {
        var project = project();
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(designAclService);
        when(designAclService.listPermissions(project, JDOE)).thenReturn(List.of());

        projectsController.deleteAclProjectRulesForSid(project, JDOE);

        assertTrue(events.isEmpty());
    }

    @Test
    void repositoryRole_granted() {
        var repositoryId = AclRepositoryId.builder().type(AclRepositoryType.PROD).id("production").build();
        when(aclServiceProvider.getAclService("prod")).thenReturn(prodAclService);
        when(prodAclService.listPermissions("production", null, JDOE)).thenReturn(List.of());

        repositoriesController.updateAclRepositoryRulesForSid(repositoryId, JDOE,
                new SetAclRoleModel(AclRole.VIEWER));

        var event = single(AuditAction.ROLE_GRANTED);
        assertEquals(AuditTargetType.REPOSITORY, event.targetType());
        assertEquals("production", event.target());
        assertEquals(Map.of("type", "prod", "to", "VIEWER"), event.details());
    }

    @Test
    void repositoryRole_revoked() {
        var repositoryId = AclRepositoryId.builder().type(AclRepositoryType.DESIGN).id("design").build();
        when(aclServiceProvider.getAclService("design")).thenReturn(designAclService);
        when(designAclService.listPermissions("design", null, TESTERS))
                .thenReturn(List.of(AclRole.CONTRIBUTOR.getCumulativePermission()));

        repositoriesController.deleteAclRepositoryRulesForSid(repositoryId, TESTERS);

        var event = single(AuditAction.ROLE_REVOKED);
        assertEquals("design", event.target());
        assertEquals(Map.of("type", "design", "from", "CONTRIBUTOR"), event.details());
    }

    @Test
    void rootRole_changed() {
        var rootId = AclRepositoryId.builder().type(AclRepositoryType.DESIGN).build();
        when(aclServiceProvider.getAclService("design")).thenReturn(designAclService);
        when(designAclService.listRootPermissions(TESTERS))
                .thenReturn(List.of(AclRole.VIEWER.getCumulativePermission()));

        repositoriesController.updateAclRepositoryRulesForRoot(rootId, new SetAclRoleModel(AclRole.MANAGER), TESTERS);

        var event = single(AuditAction.ROLE_CHANGED);
        assertEquals(AuditTargetType.ROOT, event.targetType());
        assertEquals("design", event.target());
        assertEquals(Map.of("from", "VIEWER", "to", "MANAGER"), event.details());
    }

    @Test
    void rootRole_revoked() {
        var rootId = AclRepositoryId.builder().type(AclRepositoryType.PROD).build();
        when(aclServiceProvider.getAclService("prod")).thenReturn(prodAclService);
        when(prodAclService.listRootPermissions(JDOE)).thenReturn(List.of(AclRole.VIEWER.getCumulativePermission()));

        repositoriesController.deleteAclRepositoryRulesForRoot(rootId, JDOE);

        var event = single(AuditAction.ROLE_REVOKED);
        assertEquals("prod", event.target());
        assertEquals(Map.of("from", "VIEWER"), event.details());
    }

    private static AProject project() {
        var project = mock(AProject.class);
        var repository = mock(Repository.class);
        when(project.getBusinessName()).thenReturn("Pricing");
        when(project.getRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        return project;
    }

    private AuditEvent single(AuditAction action) {
        assertEquals(1, events.size());
        var event = events.getFirst();
        assertEquals(action, event.action());
        return event;
    }
}
