package org.openl.rules.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

import org.openl.rules.rest.acl.service.AclRoleAuditor;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.common.exception.NotFoundException;

/**
 * Unit tests for the groups of {@link ManagementController}: their users, their members and their roles.
 *
 * @author Yury Molchan
 */
@ExtendWith(MockitoExtension.class)
class ManagementControllerTest {

    @Mock
    private GroupDao groupDao;
    @Mock
    private GroupManagementService groupManagementService;
    @Mock
    private ExternalGroupService extGroupService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private RepositoryAclServiceProvider aclServiceProvider;
    @Mock
    private RepositoryAclService designAclService;
    @Mock
    private SimpleRepositoryAclService prodAclService;

    private final List<AuditEvent> events = new ArrayList<>();
    private ManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new ManagementController(groupDao,
                groupManagementService,
                extGroupService,
                userManagementService,
                null,
                aclServiceProvider,
                new AclRoleAuditor(event -> events.add((AuditEvent) event)));
    }

    @Test
    void getGroupUsers_returnsUsersOfTheGroup() {
        var group = new Group();
        group.setName("Analysts");
        when(groupDao.getGroupById(42L)).thenReturn(group);
        when(userManagementService.getUsersInGroup("Analysts")).thenReturn(List.of(
                SimpleUser.builder().setUsername("jdoe").setDisplayName("Joe Doe").build(),
                SimpleUser.builder().setUsername("jsmith").setDisplayName("John Smith").build()));

        var users = controller.getGroupUsers(42L);

        assertEquals(List.of(new ManagementController.UIGroupUser("jdoe", "Joe Doe"),
                new ManagementController.UIGroupUser("jsmith", "John Smith")), users);
    }

    @Test
    void getGroupUsers_throwsNotFoundForUnknownGroup() {
        assertThrows(NotFoundException.class, () -> controller.getGroupUsers(42L));
    }

    @Test
    void getGroups_countsDualMembershipUsersOnce() {
        var group = new Group();
        group.setName("Analysts");
        when(groupDao.getAllGroups()).thenReturn(List.of(group));
        when(groupManagementService.countUsersInGroup("Analysts")).thenReturn(2L);
        when(extGroupService.countUsersInGroup("Analysts")).thenReturn(1L);
        when(userManagementService.countUsersInGroup("Analysts")).thenReturn(2L);

        var numberOfMembers = controller.getGroups().get("Analysts").numberOfMembers;

        assertEquals(2, numberOfMembers.internal);
        assertEquals(1, numberOfMembers.external);
        assertEquals(2, numberOfMembers.total);
    }

    @Test
    void saveGroup_announcesTheRootRolesThatChanged() {
        var sid = new GrantedAuthoritySid("Testers");
        when(aclServiceProvider.getAclService("design")).thenReturn(designAclService);
        when(aclServiceProvider.getAclService("prod")).thenReturn(prodAclService);
        when(designAclService.listRootPermissions(sid)).thenReturn(List.of(AclRole.VIEWER.getCumulativePermission()));
        when(prodAclService.listRootPermissions(sid)).thenReturn(List.of(AclRole.VIEWER.getCumulativePermission()));

        controller.saveGroup("Testers", "Testers", null, AclRole.CONTRIBUTOR, AclRole.VIEWER, false);

        verify(designAclService).addRootPermissions(sid, AclRole.CONTRIBUTOR.getCumulativePermission());
        verify(prodAclService).addRootPermissions(sid, AclRole.VIEWER.getCumulativePermission());
        assertEquals(1, events.size());
        var event = events.getFirst();
        assertEquals(AuditAction.ROLE_CHANGED, event.action());
        assertEquals("design", event.target());
        assertEquals("Testers", event.subject());
        assertEquals(Map.of("from", "VIEWER", "to", "CONTRIBUTOR"), event.details());
    }

    @Test
    void saveGroup_withoutARole_revokesIt() {
        var sid = new GrantedAuthoritySid("Testers");
        when(aclServiceProvider.getAclService("design")).thenReturn(designAclService);
        when(aclServiceProvider.getAclService("prod")).thenReturn(prodAclService);
        when(prodAclService.listRootPermissions(sid)).thenReturn(List.of(AclRole.MANAGER.getCumulativePermission()));

        controller.saveGroup(null, "Testers", null, null, null, null);

        verify(prodAclService).listRootPermissions(sid);
        verify(prodAclService).removeRootPermissions(sid);
        verifyNoMoreInteractions(prodAclService);
        assertEquals(1, events.size());
        assertEquals(AuditAction.ROLE_REVOKED, events.getFirst().action());
        assertEquals("prod", events.getFirst().target());
    }
}
