package org.openl.rules.rest.settings;

import java.io.IOException;
import java.util.function.Supplier;
import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.rules.webstudio.web.admin.SettingsService;

public abstract class CRUDSettingsController<E extends SettingsHolder> {

    private final ObjectMapper objectMapper;
    private final BeanValidationProvider validationProvider;
    protected final SettingsService settingsService;
    private final Supplier<E> settingsFactory;

    protected CRUDSettingsController(ObjectMapper objectMapper, BeanValidationProvider validationProvider,
                                     SettingsService settingsService, Supplier<E> settingsFactory) {
        this.validationProvider = validationProvider;
        this.settingsService = settingsService;
        this.settingsFactory = settingsFactory;
        this.objectMapper = objectMapper;
    }

    @AdminPrivilege
    @GetMapping
    @Operation(summary = "msg.settings.get-all.summary", description = "msg.settings.get-all.desc")
    public E getSettings() {
        return loadSettings();
    }

    private E loadSettings() {
        var settings = settingsFactory.get();
        settingsService.load(settings);
        return settings;
    }

    @AdminPrivilege
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "msg.settings.post-all.summary", description = "msg.settings.post-all.desc")
    public void saveSettings(@Valid @RequestBody E settings) throws IOException {
        settingsService.store(settings);
        settingsService.commit();
    }

    @AdminPrivilege
    @PatchMapping(consumes = "application/merge-patch+json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "msg.settings.patch-merge.summary", description = "msg.settings.patch-merge.desc")
    public void mergePatchSettings(@RequestBody JsonNode patch) throws IOException {
        var originalSettings = loadSettings();
        var updatedSettings = mergeSettings(originalSettings, patch);
        validationProvider.validate(updatedSettings);
        settingsService.store(updatedSettings);
        settingsService.commit();
    }

    protected E mergeSettings(E originalSettings, JsonNode patch) throws IOException {
        return objectMapper.readerForUpdating(originalSettings).<E>readValue(patch);
    }

    @AdminPrivilege
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "msg.settings.delete-all.summary", description = "msg.settings.delete-all.desc")
    public void revertSettings() throws IOException {
        settingsService.revert(settingsFactory.get());
        settingsService.commit();
    }
}
