package org.openl.rules.webstudio.web.admin.security;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;
import org.openl.rules.webstudio.web.admin.SettingsHolder;

public class CASAttributesSettings implements SettingsHolder {

    private static final String FIRST_NAME = "security.cas.attribute.first-name";
    private static final String DISPLAY_NAME = "security.cas.attribute.display-name";
    private static final String EMAIL = "security.cas.attribute.email";
    private static final String LAST_NAME = "security.cas.attribute.last-name";
    private static final String GROUPS = "security.cas.attribute.groups";

    @Parameter(description = "CAS attribute for the first name.", example = "firstName")
    @SettingPropertyName(FIRST_NAME)
    private String firstName;

    @Parameter(description = "CAS attribute for the display name.", example = "displayName")
    @SettingPropertyName(DISPLAY_NAME)
    private String displayName;

    @Parameter(description = "CAS attribute for the email.", example = "email")
    @SettingPropertyName(EMAIL)
    private String email;

    @Parameter(description = "CAS attribute for the last name.", example = "lastName")
    @SettingPropertyName(LAST_NAME)
    private String lastName;

    @Parameter(description = "CAS attribute for the groups.", example = "groups")
    @SettingPropertyName(GROUPS)
    private String groups;

    @Override
    public void load(PropertiesHolder properties) {
        firstName = properties.getProperty(FIRST_NAME);
        displayName = properties.getProperty(DISPLAY_NAME);
        email = properties.getProperty(EMAIL);
        lastName = properties.getProperty(LAST_NAME);
        groups = properties.getProperty(GROUPS);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(FIRST_NAME, firstName);
        properties.setProperty(DISPLAY_NAME, displayName);
        properties.setProperty(EMAIL, email);
        properties.setProperty(LAST_NAME, lastName);
        properties.setProperty(GROUPS, groups);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(FIRST_NAME,
                DISPLAY_NAME,
                EMAIL,
                LAST_NAME,
                GROUPS);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }
}
