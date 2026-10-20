package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.openl.spring.env.DefaultPropertySource;
import org.openl.spring.env.SysEnvRefPropertySource;
import org.openl.spring.env.SysPropPropertySource;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;

class EnvironmentSettingsAuditorTest {

    /** Fingerprints made quickly; OpenL Studio makes them by bcrypt of the default strength. */
    private static final SettingFingerprints FINGERPRINTS = new SettingFingerprints(new BCryptPasswordEncoder(4));

    private final AuditService auditService = mock(AuditService.class);
    private final InMemoryStore store = new InMemoryStore();
    private Map<String, Object> variables;
    /** The repositories OpenL Studio is configured with. */
    private String repositories = "design";

    @BeforeEach
    void setUp() {
        when(auditService.isEnabled()).thenReturn(true);
        variables = new HashMap<>(Map.of("MAIL_URL", "smtp://mail.example.com",
                "REPOSITORY_DESIGN_PASSWORD", "s3cret",
                "OPENL_STUDIO_SERVICE_HOST", "10.0.0.1"));
    }

    @Test
    void theFirstStartKeepsTheSettingsOfTheDeploymentWithoutAnyValue() {
        start();

        verify(auditService, never()).recordEvent(any());
        var snapshot = store.snapshot;
        assertEquals(List.of("mail.url", "repository.design.password"), List.copyOf(snapshot.keySet()),
                "Only the settings OpenL Studio knows; a variable of Kubernetes is none");
        assertTrue(FINGERPRINTS.matches("s3cret", snapshot.get("repository.design.password")));
        assertFalse(snapshot.toString().contains("s3cret") || snapshot.toString().contains("mail.example.com"));
    }

    @Test
    void announcesTheSettingsTheDeploymentAddedChangedAndRemovedSinceTheLastStart() {
        start();
        variables.put("MAIL_URL", "smtp://other.example.com");
        variables.put("MAIL_USERNAME", "bot");
        variables.remove("REPOSITORY_DESIGN_PASSWORD");

        start();

        var event = recorded();
        assertEquals(AuditAction.SETTINGS_CHANGED, event.action());
        assertEquals(AuditTargetType.SETTINGS, event.targetType());
        assertEquals("mail.url, mail.username, repository.design.password", event.target());
        assertEquals(Map.of(AuditEvent.ORIGIN, AuditEvent.ENVIRONMENT,
                "added", List.of("mail.username"),
                "changed", List.of("mail.url"),
                "removed", List.of("repository.design.password")), event.details());
        assertFalse(event.toString().contains("example.com") || event.toString().contains("bot"),
                "No value of the environment is recorded");
        assertEquals(List.of("mail.url", "mail.username"), List.copyOf(store.snapshot.keySet()));
    }

    @Test
    void announcesNothingWhenTheDeploymentKeptItsSettings() {
        start();
        var fingerprints = Map.copyOf(store.snapshot);

        start();

        verify(auditService, never()).recordEvent(any());
        assertEquals(fingerprints, store.snapshot, "A fingerprint of a value that stays is kept");
        assertEquals(1, store.saves, "A snapshot that stays is not written again");
    }

    @Test
    void aValueTheDefaultGivesIsNoSettingOfTheDeployment() {
        variables.put("MAIL_USERNAME", "admin");
        start();
        assertFalse(store.snapshot.containsKey("mail.username"), "A variable that repeats the default");

        variables.put("MAIL_USERNAME", "bot");
        start();
        variables.put("MAIL_USERNAME", "admin");
        start();

        var events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService, times(2)).recordEvent(events.capture());
        assertEquals(List.of("mail.username"), events.getAllValues().getFirst().details().get("added"));
        assertEquals(List.of("mail.username"), events.getAllValues().getLast().details().get("removed"),
                "Going back to the default removes the setting of the deployment");
    }

    @Test
    void comparesASettingAModelDeclaresOnAGetter() {
        variables.put("REPOSITORY_DESIGN_FACTORY", "repo-git");

        start();

        assertTrue(store.snapshot.containsKey("repository.design.factory"));
    }

    @Test
    void comparesTheSettingsOfARepositoryRemovedSinceTheLastStart() {
        repositories = "design,archive";
        variables.put("REPOSITORY_ARCHIVE_URI", "https://git.example.com/archive.git");
        start();
        assertTrue(store.snapshot.containsKey("repository.archive.uri"));

        repositories = "design";
        start();

        verify(auditService, never()).recordEvent(any());
        assertTrue(store.snapshot.containsKey("repository.archive.uri"), "The deployment sets it still");
    }

    @Test
    void comparesNothingWhileTheLogIsOff() {
        when(auditService.isEnabled()).thenReturn(false);

        start();

        assertNull(store.snapshot, "No snapshot is made");
    }

    @Test
    void keepsTheLastSnapshotWhenTheChangeCannotBeRecorded() {
        start();
        var fingerprints = Map.copyOf(store.snapshot);
        variables.put("MAIL_URL", "smtp://other.example.com");
        doThrow(new IllegalStateException("The database is down")).when(auditService).recordEvent(any());

        assertDoesNotThrow(this::start);

        assertEquals(fingerprints, store.snapshot, "The next start announces the change");
    }

    @Test
    void neverFailsTheStartForTheSnapshot() {
        store.failing = true;

        assertDoesNotThrow(this::start);

        verify(auditService, never()).recordEvent(any());
    }

    private void start() {
        var environment = new AbstractEnvironment() {
        };
        environment.getPropertySources().addLast(new SysPropPropertySource(Map.of("user.home", "/home/openl")));
        environment.getPropertySources().addLast(new SysEnvRefPropertySource(Map.copyOf(variables)));
        // The defaults name the repositories, as openl-default.properties does; they are no setting of the deployment.
        environment.getPropertySources().addLast(new MapPropertySource(DefaultPropertySource.PROPS_NAME,
                Map.of("design-repository-configs", repositories, "mail.username", "admin")));
        new EnvironmentSettingsAuditor(environment, store, auditService, FINGERPRINTS).compareWithLastStart();
    }

    private AuditEvent recorded() {
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).recordEvent(event.capture());
        return event.getValue();
    }

    /** A store that keeps the snapshot in memory, and can fail to read it. */
    private static final class InMemoryStore implements SettingsSnapshotStore {

        private @Nullable Map<String, String> snapshot;
        private boolean failing;
        private int saves;

        @Override
        public Optional<Map<String, String>> load() throws IOException {
            if (failing) {
                throw new IOException("The disk is gone");
            }
            return Optional.ofNullable(snapshot);
        }

        @Override
        public void save(Map<String, String> snapshot) {
            this.snapshot = snapshot;
            saves++;
        }
    }
}
