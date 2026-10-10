package org.openl.studio.settings.rest.controller;

import java.io.IOException;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.security.AdminPrivilege;

@AdminPrivilege
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

    @PostMapping
    @Operation(summary = "msg.settings.post-all.summary", description = "msg.settings.post-all.desc")
    public void saveSettings(@Valid @RequestBody E settings) throws IOException {
        settingsService.store(settings);
        settingsService.commit();
    }

    /**
     * Applies a JSON merge patch to the stored settings, validates and stores them.
     *
     * <p>A patch value the settings cannot hold, such as text or a fraction for a whole number, is refused with
     * {@code 400} as an unreadable request body is, naming the field.
     */
    @PatchMapping(consumes = "application/merge-patch+json")
    @Operation(summary = "msg.settings.patch-merge.summary", description = "msg.settings.patch-merge.desc")
    public void mergePatchSettings(@RequestBody JsonNode patch, HttpServletRequest request) throws IOException {
        var originalSettings = loadSettings();
        E updatedSettings;
        try {
            updatedSettings = mergeSettings(originalSettings, patch);
        } catch (JsonMappingException e) {
            throw new HttpMessageNotReadableException(e.getOriginalMessage(), e, new ServletServerHttpRequest(request));
        }
        validationProvider.validate(updatedSettings);
        settingsService.store(updatedSettings);
        settingsService.commit();
    }

    protected E mergeSettings(E originalSettings, JsonNode patch) throws IOException {
        return objectMapper.readerForUpdating(originalSettings).<E>readValue(patch);
    }

    @DeleteMapping
    @Operation(summary = "msg.settings.delete-all.summary", description = "msg.settings.delete-all.desc")
    public void revertSettings() throws IOException {
        settingsService.revert(settingsFactory.get());
        settingsService.commit();
    }
}
