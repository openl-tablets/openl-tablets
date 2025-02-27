package org.openl.rules.rest.settings;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.settings.model.EntrypointSettingsModel;
import org.openl.rules.rest.settings.model.SettingsModel;
import org.openl.rules.rest.settings.model.SupportedFeaturesModel;
import org.openl.rules.rest.settings.model.UserManagementMode;

@RestController
@Validated
@RequestMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Settings")
public class SettingsController {

    private final Environment environment;
    private final Optional<Supplier<String>> logoutUrlProvider;
    private final BooleanSupplier mailSenderFeature;

    public SettingsController(Environment environment,
                              @Qualifier("logoutUrlProvider") Optional<Supplier<String>> logoutUrlProvider,
                              @Qualifier("mailSenderFeature") BooleanSupplier mailSenderFeature) {
        this.environment = environment;
        this.logoutUrlProvider = logoutUrlProvider;
        this.mailSenderFeature = mailSenderFeature;
    }

    @Operation(summary = "msg.get-app-settings.summary", description = "msg.get-app-settings.desc")
    @GetMapping
    public SettingsModel getSettings() {
        var userManagementMode = getUserManagementMode();
        return SettingsModel.builder()
                .entrypoint(EntrypointSettingsModel.builder()
                        .logoutUrl(logoutUrlProvider.map(Supplier::get).orElse(null))
                        .build())
                .userMode(userManagementMode)
                .supportedFeatures(SupportedFeaturesModel.builder()
                        .groupsManagement(userManagementMode == UserManagementMode.EXTERNAL)
                        .userManagement(userManagementMode != null)
                        .emailVerification(mailSenderFeature.getAsBoolean())
                        .build())
                .build();
    }

    private UserManagementMode getUserManagementMode() {
        return switch (environment.getProperty("user.mode")) {
            case null -> null;
            case "single" -> null;
            case "demo", "multi" -> UserManagementMode.INTERNAL;
            default -> UserManagementMode.EXTERNAL;
        };
    }

}
