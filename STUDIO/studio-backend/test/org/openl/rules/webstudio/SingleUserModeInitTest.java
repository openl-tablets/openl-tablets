package org.openl.rules.webstudio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;

/**
 * Unit tests for {@link SingleUserModeInit}.
 *
 * @author Yury Molchan
 */
@ExtendWith(MockitoExtension.class)
class SingleUserModeInitTest {

    @Mock
    private UserManagementService userManagementService;

    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        environment.setProperty("security.single.username", "jdoe");
        environment.setProperty("security.single.email", "jdoe@example.com");
        environment.setProperty("security.single.first-name", "John");
        environment.setProperty("security.single.last-name", "Doe");
        environment.setProperty("security.single.display-name", "John Doe");
        Props.setEnvironment(environment);
    }

    @AfterAll
    static void tearDown() {
        Props.setEnvironment(null);
    }

    @Test
    void createsUserWithConfiguredInitialData() {
        when(userManagementService.existsByName("jdoe")).thenReturn(false);

        new SingleUserModeInit(userManagementService).init();

        verify(userManagementService).addUser("jdoe", "John", "Doe", null, "jdoe@example.com", "John Doe");
        verify(userManagementService).syncUserData("jdoe", null, null, null, null);
    }

    @Test
    void keepsExistingUserDataEditable() {
        when(userManagementService.existsByName("jdoe")).thenReturn(true);

        new SingleUserModeInit(userManagementService).init();

        verify(userManagementService, never()).addUser(any(), any(), any(), any(), any(), any());
        verify(userManagementService).syncUserData("jdoe", null, null, null, null);
    }

    @Test
    void appliesPinnedPropertiesAsExternalData() {
        // The properties are defined by more prioritized sources, such as Java system properties
        // or environment variables, so they cannot be edited.
        environment.setProperty("_sys_disable_security.single.email", "true");
        environment.setProperty("_sys_disable_security.single.display-name", "true");
        when(userManagementService.existsByName("jdoe")).thenReturn(true);

        new SingleUserModeInit(userManagementService).init();

        verify(userManagementService).syncUserData("jdoe", null, null, "jdoe@example.com", "John Doe");
    }
}
