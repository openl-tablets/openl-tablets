package org.openl.rules.webstudio.web.admin.security;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.studio.settings.converter.SettingPropertyName;

public class SAMLAttributesSettings implements SettingsHolder {

    private static final String ATTR_USERNAME = "security.saml.attribute.username";
    private static final String ATTR_FIRST_NAME = "security.saml.attribute.first-name";
    private static final String ATTR_LAST_NAME = "security.saml.attribute.last-name";
    private static final String ATTR_DISPLAY_NAME = "security.saml.attribute.display-name";
    private static final String ATTR_EMAIL = "security.saml.attribute.email";
    private static final String ATTR_GROUPS = "security.saml.attribute.groups";

    @Getter
    @Parameter(description = "SAML attribute for username.")
    @Setter
    @SettingPropertyName(ATTR_USERNAME)
    private String username;

    @Getter
    @Parameter(description = "SAML attribute for first name.")
    @Setter
    @SettingPropertyName(ATTR_FIRST_NAME)
    private String firstName;

    @Getter
    @Parameter(description = "SAML attribute for last name.")
    @Setter
    @SettingPropertyName(ATTR_LAST_NAME)
    private String lastName;

    @Getter
    @Parameter(description = "SAML attribute for display name.")
    @Setter
    @SettingPropertyName(ATTR_DISPLAY_NAME)
    private String displayName;

    @Getter
    @Parameter(description = "SAML attribute for email.")
    @Setter
    @SettingPropertyName(ATTR_EMAIL)
    private String email;

    @Getter
    @Parameter(description = "SAML attribute for groups.")
    @Setter
    @SettingPropertyName(ATTR_GROUPS)
    private String groups;

    @Override
    public void load(PropertiesHolder properties) {
        username = properties.getProperty(ATTR_USERNAME);
        firstName = properties.getProperty(ATTR_FIRST_NAME);
        lastName = properties.getProperty(ATTR_LAST_NAME);
        displayName = properties.getProperty(ATTR_DISPLAY_NAME);
        email = properties.getProperty(ATTR_EMAIL);
        groups = properties.getProperty(ATTR_GROUPS);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(ATTR_USERNAME, username);
        properties.setProperty(ATTR_FIRST_NAME, firstName);
        properties.setProperty(ATTR_LAST_NAME, lastName);
        properties.setProperty(ATTR_DISPLAY_NAME, displayName);
        properties.setProperty(ATTR_EMAIL, email);
        properties.setProperty(ATTR_GROUPS, groups);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(
                ATTR_USERNAME,
                ATTR_FIRST_NAME,
                ATTR_LAST_NAME,
                ATTR_DISPLAY_NAME,
                ATTR_EMAIL,
                ATTR_GROUPS
        );
    }
}
