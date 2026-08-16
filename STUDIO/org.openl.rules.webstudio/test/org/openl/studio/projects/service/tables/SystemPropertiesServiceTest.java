package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.webstudio.web.admin.AdministrationSettings;

class SystemPropertiesServiceTest {

    private final Environment environment = mock(Environment.class);
    private final SystemPropertiesService service = new SystemPropertiesService(environment);

    @AfterEach
    void forgetTheAuthor() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsNothingWhileTheAdministratorHasTurnedItOff() {
        signedInAs("jane");

        assertTrue(service.onCreate().isEmpty());
    }

    @Test
    void recordsWhoCreatedTheTableAndWhen() {
        recording();
        signedInAs("jane");

        var stamped = service.onCreate();

        // Only what a creation records: who last edited the table belongs to a save, not to a creation.
        assertEquals(List.of("createdBy", "createdOn"), List.copyOf(stamped.keySet()));
        assertEquals("jane", stamped.get("createdBy"));
        assertInstanceOf(Date.class, stamped.get("createdOn"));
    }

    @Test
    void leavesTheAuthorOutOfASingleUserInstallation() {
        recording();
        when(environment.getProperty("user.mode")).thenReturn("single");
        signedInAs("jane");

        assertEquals(List.of("createdOn"), List.copyOf(service.onCreate().keySet()));
    }

    private void recording() {
        when(environment.getProperty(AdministrationSettings.UPDATE_SYSTEM_PROPERTIES, Boolean.class))
                .thenReturn(true);
    }

    private static void signedInAs(String user) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "secret", List.of()));
    }
}
