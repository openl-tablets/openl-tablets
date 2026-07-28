package org.openl.rules.webstudio.web.admin.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.studio.settings.converter.SettingPropertyName;

public class NOPUserSettings implements SettingsHolder {

    public static final String SINGLE_USERNAME = "security.single.username";
    public static final String SINGLE_EMAIL = "security.single.email";
    public static final String SINGLE_FIRST_NAME = "security.single.first-name";
    public static final String SINGLE_LAST_NAME = "security.single.last-name";
    public static final String SINGLE_DISPLAY_NAME = "security.single.display-name";

    @Getter
    @Parameter(description = "Username", example = "DEFAULT")
    @Setter
    @SettingPropertyName(SINGLE_USERNAME)
    @NotBlank
    @Size(min = 1, max = 125)
    private String username;

    @Parameter(description = "Email", example = "default@example.com")
    @Setter
    @SettingPropertyName(SINGLE_EMAIL)
    @Email
    @Getter
    private String email;

    @Getter
    @Parameter(description = "First name", example = "DE")
    @Setter
    @SettingPropertyName(SINGLE_FIRST_NAME)
    @NotBlank
    @Size(min = 1, max = 125)
    private String firstName;

    @Getter
    @Parameter(description = "Last name", example = "FAULT")
    @Setter
    @SettingPropertyName(SINGLE_LAST_NAME)
    @NotBlank
    @Size(min = 1, max = 125)
    private String lastName;

    @Getter
    @Parameter(description = "Display name", example = "DEFAULT")
    @Setter
    @SettingPropertyName(SINGLE_DISPLAY_NAME)
    @NotBlank
    @Size(min = 1, max = 255)
    private String displayName;

    @Override
    public void load(PropertiesHolder properties) {
        username = properties.getProperty(SINGLE_USERNAME);
        email = properties.getProperty(SINGLE_EMAIL);
        firstName = properties.getProperty(SINGLE_FIRST_NAME);
        lastName = properties.getProperty(SINGLE_LAST_NAME);
        displayName = properties.getProperty(SINGLE_DISPLAY_NAME);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(SINGLE_USERNAME, username);
        properties.setProperty(SINGLE_EMAIL, email);
        properties.setProperty(SINGLE_FIRST_NAME, firstName);
        properties.setProperty(SINGLE_LAST_NAME, lastName);
        properties.setProperty(SINGLE_DISPLAY_NAME, displayName);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(SINGLE_USERNAME,
                SINGLE_EMAIL,
                SINGLE_FIRST_NAME,
                SINGLE_LAST_NAME,
                SINGLE_DISPLAY_NAME);
        load(properties);
    }
}
