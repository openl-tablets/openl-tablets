package org.openl.studio.audit.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.settings.converter.SecretSettings;

/**
 * Announces the system settings an administrator changes to the audit log: one event for each save, naming every
 * setting the save gave another value.
 *
 * <p>The target lists the properties that changed, so a search by the target finds the event by any of them. The
 * details hold each of them with its values before and after the save. A blank value counts as no value.
 *
 * <p>No secret reaches the log. The value of a secret setting, such as a password, is never recorded: the details
 * tell only that it changed. The credentials an ordinary value holds, such as a token in the address of a
 * repository, are hidden ({@link SecretSettings#masked(String)}). A long value is cut short.
 *
 * <p>The settings live in a file, which no transaction covers, so a change is announced once it is saved. A failure
 * to record it is logged and never fails the change: the change is saved already.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsAuditor {

    /** The detail that holds the settings that changed, by their properties. */
    static final String CHANGED = "changed";
    /** The mark of a secret setting, whose values the event leaves out. */
    static final String SECRET = "secret";
    /** The longest value of a setting the details keep; a longer one is cut short. */
    static final int VALUE_LENGTH = 200;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Announces the settings one save gave other values: the properties whose values before and after the save
     * differ. Nothing is announced when no value changed.
     *
     * @param before the values of the properties the save wrote, before it, by property, as they are written: a
     *               reference to an environment variable or to another property is never resolved
     * @param after  the values of the same properties after the save, as they are written
     */
    public void changed(Map<String, @Nullable String> before, Map<String, @Nullable String> after) {
        var changes = new LinkedHashMap<String, Change>();
        after.forEach((property, value) -> {
            var change = new Change(valueOf(before.get(property)), valueOf(value));
            if (!Objects.equals(change.from(), change.to())) {
                changes.put(property, change);
            }
        });
        if (changes.isEmpty()) {
            return;
        }
        try {
            eventPublisher.publishEvent(eventOf(changes.keySet(), detailsOf(changes)));
        } catch (RuntimeException e) {
            log.error("Failed to record the change of the settings {} in the audit log.", changes.keySet(), e);
        }
    }

    /**
     * The event announcing a change of the settings: the properties it changed as the target, so a search by the
     * target finds it by any of them, and the details given.
     */
    private static AuditEvent eventOf(Collection<String> properties, Map<String, ?> details) {
        return AuditEvent.builder()
                .action(AuditAction.SETTINGS_CHANGED)
                .targetType(AuditTargetType.SETTINGS)
                .target(String.join(", ", properties))
                .details(details)
                .build();
    }

    /** The details of the changes: each setting with its values. */
    private static Map<String, ?> detailsOf(Map<String, Change> changes) {
        var values = new LinkedHashMap<String, Object>();
        changes.forEach((property, change) -> values.put(property, change.recorded(property)));
        return Map.of(CHANGED, values);
    }

    private static @Nullable String valueOf(@Nullable String value) {
        return Optional.ofNullable(value).filter(Predicate.not(String::isBlank)).orElse(null);
    }

    /** The values of a setting before a save and after it, either one missing. */
    private record Change(@Nullable String from, @Nullable String to) {

        /**
         * The change as the details record it: the values, with their credentials hidden and cut short, or only
         * the mark of a secret setting.
         */
        Map<String, Object> recorded(String property) {
            var change = new LinkedHashMap<String, Object>();
            if (SecretSettings.isSecret(property)) {
                change.put(SECRET, true);
                return change;
            }
            Optional.ofNullable(from).map(Change::shown).ifPresent(value -> change.put("from", value));
            Optional.ofNullable(to).map(Change::shown).ifPresent(value -> change.put("to", value));
            return change;
        }

        private static @Nullable String shown(String value) {
            return AuditService.cut(SecretSettings.masked(value), VALUE_LENGTH);
        }
    }
}
