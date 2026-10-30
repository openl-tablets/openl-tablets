package org.openl.rules.webstudio.web.admin;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.validation.annotation.Validated;

import org.openl.config.InMemoryProperties;
import org.openl.rules.webstudio.web.Props;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.studio.audit.service.SettingsAuditor;

/**
 * Loads, stores and applies the system settings.
 *
 * <p>Applying the settings saves them to the settings file of the application and announces to the audit log, in
 * one event, every setting the save gave another value. The saves are made one at a time, so each event names the
 * values its own save changed.
 *
 * <p>The event holds the values as they are written. A value that refers to an environment variable or to another
 * property, as {@code ${MAIL_URL}} does, is kept as the reference, so the value it stands for never reaches the log.
 *
 * <p>A setting an environment variable or a system property sets, directly or through a {@code $ref} key, is
 * read-only, and its value is never written to the settings file: written there, it would take the place of a value
 * reached through a {@code $ref} key. A request that changes such a setting is refused before it is stored (see
 * {@code ReadOnlySettings}), so what is left out here is the value a setting model writes back as it reads it, or
 * reformatted.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Validated
public class SettingsServiceImpl implements SettingsService {

    /** A property every save sets anew; it changes no setting. */
    private static final String LAST_MODIFIED_TIME = "_last.modified.time";
    /** The settings file is one for the application, so its saves are made one at a time. */
    private static final Lock SAVES = new ReentrantLock();

    private final InMemoryProperties inMemoryProperties;
    private final ConfigurableEnvironment environment;
    private final SettingsAuditor auditor;
    private final SettingsStore store;

    /**
     * Saves the settings to the settings file of the application, read through the given environment.
     */
    public SettingsServiceImpl(InMemoryProperties inMemoryProperties,
                               ConfigurableEnvironment environment,
                               SettingsAuditor auditor) {
        this(inMemoryProperties, environment, auditor, settings -> DynamicPropertySource.get().save(settings));
    }

    @Override
    public void load(@NotNull SettingsHolder settings) {
        settings.load(inMemoryProperties);
    }

    @Override
    public void store(@Valid SettingsHolder settings) {
        settings.store(inMemoryProperties);
    }

    @Override
    public void revert(@NotNull SettingsHolder settings) {
        settings.revert(inMemoryProperties);
    }

    @Override
    public void commit() throws IOException {
        // Temporary solution. Made so that when saving settings, if there were no changes, active users are still
        // logged out.
        // This is necessary to reset some parameters such as: information about blocking authentication attempts in
        // git,
        // when the maximum number of attempts is exceeded.
        inMemoryProperties.setProperty(LAST_MODIFIED_TIME, Instant.now().toString());
        var config = new HashMap<>(inMemoryProperties.getConfig());
        config.entrySet()
                .removeIf(setting -> setting.getValue() != null && Props.isDisabled(environment, setting.getKey()));
        var properties = config.keySet().stream().filter(property -> !LAST_MODIFIED_TIME.equals(property)).sorted()
                .toList();
        SAVES.lock();
        try {
            var before = valuesOf(properties);
            store.save(config);
            // A reverted setting has its default value now.
            auditor.changed(before, valuesOf(properties));
        } finally {
            SAVES.unlock();
        }
    }

    /** The values the properties have now, as they are written, by property, a missing one included. */
    private Map<String, @Nullable String> valuesOf(List<String> properties) {
        var values = new LinkedHashMap<String, @Nullable String>();
        properties.forEach(property -> values.put(property, valueOf(property)));
        return values;
    }

    /**
     * The value a property has now, as it is written: the references it holds, as {@code ${MAIL_URL}}, are left as
     * they are. The value the first property source gives wins, as it does for the environment.
     *
     * <p>A default a repository takes by reference ({@code .$ref}) is read resolved. When it names a property no
     * setting has, it cannot be read and counts as no value: reading it never fails a save.
     */
    private @Nullable String valueOf(String property) {
        try {
            return environment.getPropertySources().stream()
                    .map(source -> source.getProperty(property))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            // The message of the exception holds the value, so it is not logged.
            log.warn("Cannot resolve the value of the setting '{}'.", property);
            return null;
        }
    }

    /** Where the settings are saved. */
    @FunctionalInterface
    interface SettingsStore {

        /** Saves the given settings, a {@code null} value removing the property. */
        void save(Map<String, String> settings) throws IOException;
    }
}
