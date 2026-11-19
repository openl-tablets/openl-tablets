package org.openl.studio.settings.rest.controller;

import java.io.IOException;
import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.security.AdminPrivilege;
import org.openl.studio.settings.converter.ReadOnlySettings;

/**
 * Settings of one kind that an administrator reads and changes.
 *
 * <p>A change is a JSON merge patch: the settings it leaves out keep their values. Settings that need a replacement
 * as a whole or a reset to their defaults extend {@link CRUDSettingsController}.
 *
 * @param <E> the settings
 */
@AdminPrivilege
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PatchSettingsController<E extends SettingsHolder> {

    protected final ObjectMapper objectMapper;
    private final BeanValidationProvider validationProvider;
    protected final SettingsService settingsService;
    protected final Supplier<E> settingsFactory;

    @GetMapping
    @Operation(summary = "msg.settings.get-all.summary", description = "msg.settings.get-all.desc")
    public E getSettings() {
        return loadSettings();
    }

    protected E loadSettings() {
        var settings = settingsFactory.get();
        settingsService.load(settings);
        return settings;
    }

    /**
     * Applies a JSON merge patch to the stored settings, validates and stores them.
     *
     * <p>A patch value the settings cannot hold, such as text or a fraction for a whole number, is refused with
     * {@code 400} as an unreadable request body is, naming the field. A patch that changes a read-only setting is
     * refused with {@code 400} too, naming the setting.
     */
    @PatchMapping(consumes = "application/merge-patch+json")
    @Operation(summary = "msg.settings.patch-merge.summary", description = "msg.settings.patch-merge.desc")
    public void mergePatchSettings(@RequestBody JsonNode patch, HttpServletRequest request) throws IOException {
        var originalSettings = loadSettings();
        ReadOnlySettings.check(ReadOnlySettings.answerOf(objectMapper, originalSettings), patch);
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
        return objectMapper.readerForUpdating(originalSettings).readValue(patch);
    }
}
