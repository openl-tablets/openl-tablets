package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;

class SettingsAuditorTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final SettingsAuditor auditor = new SettingsAuditor(eventPublisher);

    @Test
    void announcesEverySettingOneSaveChangedInOneEvent() {
        var before = values("user.mode", "single", "project.history.count", "100", "mail.username", "bot",
                "repository.design.password", "old-secret");
        var after = values("user.mode", "multi", "project.history.count", null, "mail.username", "bot",
                "repository.design.password", "new-secret");

        auditor.changed(before, after);

        var event = announced();
        assertEquals(AuditAction.SETTINGS_CHANGED, event.action());
        assertEquals(AuditTargetType.SETTINGS, event.targetType());
        assertEquals("user.mode, project.history.count, repository.design.password", event.target(),
                "The properties that changed, in the order of the save");
        assertEquals(Map.of(SettingsAuditor.CHANGED, Map.of(
                "user.mode", Map.of("from", "single", "to", "multi"),
                "project.history.count", Map.of("from", "100"),
                "repository.design.password", Map.of(SettingsAuditor.SECRET, true))), event.details(),
                "A setting without a value names none, and a secret one names no value at all");
    }

    @Test
    void hidesTheCredentialsOfAValueAndCutsALongValueShort() {
        auditor.changed(values("repository.design.uri", "https://bot:old-token@github.com/org/rules.git",
                        "repository.design.comment-template", null),
                values("repository.design.uri", "https://bot:new-token@github.com/org/rules.git",
                        "repository.design.comment-template", "x".repeat(300)));

        var changed = (Map<?, ?>) announced().details().get(SettingsAuditor.CHANGED);
        var hidden = "https://***@github.com/org/rules.git";
        assertEquals(Map.of("from", hidden, "to", hidden), changed.get("repository.design.uri"),
                "The token changed, which the event tells without the token");
        assertEquals(Map.of("to", "x".repeat(SettingsAuditor.VALUE_LENGTH - 1) + "…"),
                changed.get("repository.design.comment-template"));
    }

    @Test
    void keepsTheValuesOfEverySettingHoweverManyOneSaveChanged() {
        var before = values();
        var after = values();
        for (var i = 1; i <= 100; i++) {
            before.put("setting." + i, "a".repeat(150));
            after.put("setting." + i, "b".repeat(150));
        }

        auditor.changed(before, after);

        var changed = (Map<?, ?>) announced().details().get(SettingsAuditor.CHANGED);
        assertEquals(100, changed.size());
        assertEquals(Map.of("from", "a".repeat(150), "to", "b".repeat(150)), changed.get("setting.100"));
    }

    @Test
    void announcesNothingWhenNoValueChanged() {
        auditor.changed(values("user.mode", "multi", "mail.username", null),
                values("user.mode", "multi", "mail.username", " "));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void neverFailsAChangeForTheAuditLog() {
        doThrow(new IllegalStateException("The database is down")).when(eventPublisher).publishEvent(any());

        assertDoesNotThrow(() -> auditor.changed(values("user.mode", "single"), values("user.mode", "multi")));
    }

    /** The values of the properties, as property and value pairs in their order; a value may be missing. */
    private static Map<String, @Nullable String> values(@Nullable String... pairs) {
        var values = new LinkedHashMap<String, @Nullable String>();
        for (var i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return values;
    }

    private AuditEvent announced() {
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        return event.getValue();
    }
}
