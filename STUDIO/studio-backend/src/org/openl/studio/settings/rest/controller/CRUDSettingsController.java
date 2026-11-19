package org.openl.studio.settings.rest.controller;

import java.io.IOException;
import java.util.function.Supplier;
import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.security.AdminPrivilege;
import org.openl.studio.settings.converter.ReadOnlySettings;

/**
 * Settings of one kind that an administrator also replaces as a whole or resets to their defaults.
 *
 * @param <E> the settings
 */
@AdminPrivilege
public abstract class CRUDSettingsController<E extends SettingsHolder> extends PatchSettingsController<E> {

    protected CRUDSettingsController(ObjectMapper objectMapper, BeanValidationProvider validationProvider,
                                     SettingsService settingsService, Supplier<E> settingsFactory) {
        super(objectMapper, validationProvider, settingsService, settingsFactory);
    }

    @PostMapping
    @Operation(summary = "msg.settings.post-all.summary", description = "msg.settings.post-all.desc")
    public void saveSettings(@Valid @RequestBody E settings) throws IOException {
        ReadOnlySettings.check(ReadOnlySettings.answerOf(objectMapper, loadSettings()),
                ReadOnlySettings.answerOf(objectMapper, settings));
        settingsService.store(settings);
        settingsService.commit();
    }

    @DeleteMapping
    @Operation(summary = "msg.settings.delete-all.summary", description = "msg.settings.delete-all.desc")
    public void revertSettings() throws IOException {
        settingsService.revert(settingsFactory.get());
        settingsService.commit();
    }
}
