package org.openl.rules.webstudio.web.admin;

import java.io.IOException;
import java.time.Instant;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import org.openl.config.InMemoryProperties;
import org.openl.spring.env.DynamicPropertySource;

@Validated
public class SettingsServiceImpl implements SettingsService {

    private final InMemoryProperties inMemoryProperties;

    public SettingsServiceImpl(InMemoryProperties inMemoryProperties) {
        this.inMemoryProperties = inMemoryProperties;
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
        inMemoryProperties.setProperty("_last.modified.time", Instant.now().toString());
        DynamicPropertySource.get().save(inMemoryProperties.getConfig());
    }
}
