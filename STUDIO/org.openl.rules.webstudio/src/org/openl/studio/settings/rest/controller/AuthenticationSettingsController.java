package org.openl.studio.settings.rest.controller;

import java.io.IOException;
import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.rules.webstudio.web.admin.security.AuthenticationSettings;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.security.AdminPrivilege;
import org.openl.studio.settings.model.auth.CreateAuthenticationTemplateModel;
import org.openl.studio.settings.service.auth.AuthenticationSettingsFactory;

@RestController
@RequestMapping("/admin/settings/authentication")
@Tag(name = "Settings: Authentication")
@Validated
public class AuthenticationSettingsController extends CRUDSettingsController<AuthenticationSettings> {

    private final AuthenticationSettingsFactory authenticationSettingsFactory;

    public AuthenticationSettingsController(ObjectMapper objectMapper,
                                            BeanValidationProvider validationProvider,
                                            SettingsService settingsService,
                                            AuthenticationSettingsFactory authenticationSettingsFactory) {
        super(objectMapper, validationProvider, settingsService, authenticationSettingsFactory);
        this.authenticationSettingsFactory = authenticationSettingsFactory;
    }

    @Operation(description = "msg.settings.template.desc", summary = "msg.settings.template.summary")
    @PostMapping(value = "/template", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @AdminPrivilege
    public AuthenticationSettings getConfigurationTemplate(@RequestBody @Valid CreateAuthenticationTemplateModel request) {
        var settings = authenticationSettingsFactory.create(request.getUserMode());
        settingsService.load(settings);
        settings.setUserMode(request.getUserMode());
        return settings;
    }

    @Override
    protected AuthenticationSettings mergeSettings(AuthenticationSettings originalSettings, JsonNode patch) throws IOException {
        var userMode = patch.get("userMode");
        if (userMode != null && !originalSettings.getUserMode().getValue().equals(userMode.asText())) {
            throw new BadRequestException("invalid.settings.authentication.user-mode.not-match.message",
                    new Object[]{userMode.asText(), originalSettings.getUserMode().getValue()});
        }
        return super.mergeSettings(originalSettings, patch);
    }
}
