package org.openl.rules.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.NotFoundException;

/**
 * Unit tests for the group users listing in {@link ManagementController}.
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

    private ManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new ManagementController(groupDao,
                groupManagementService,
                extGroupService,
                userManagementService,
                null,
                aclServiceProvider);
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
}
