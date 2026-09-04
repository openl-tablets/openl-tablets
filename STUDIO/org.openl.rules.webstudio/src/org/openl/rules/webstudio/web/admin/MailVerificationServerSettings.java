package org.openl.rules.webstudio.web.admin;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.rest.validation.MailConfigConstraint;
import org.openl.studio.settings.converter.SettingPropertyName;

@MailConfigConstraint
public class MailVerificationServerSettings implements SettingsHolder {

    public static final String MAIL_URL = "mail.url";
    public static final String MAIL_USERNAME = "mail.username";
    public static final String MAIL_PASSWORD = "mail.password";

    @Getter
    @Parameter(description = "Mail server url", example = "smtps://mail.example.com:1587")
    @Setter
    @SettingPropertyName(MAIL_URL)
    private String url;

    @Getter
    @Parameter(description = "Username for authentication on mail server", example = "jhon@mail.example.com")
    @Setter
    @SettingPropertyName(MAIL_USERNAME)
    private String username;

    @Getter
    @Parameter(description = "Password for authentication on mail server", example = "qwerty")
    @Setter
    @SettingPropertyName(value = MAIL_PASSWORD, secret = true)
    private String password;

    @Override
    public void load(PropertiesHolder properties) {
        url = properties.getProperty(MAIL_URL);
        username = properties.getProperty(MAIL_USERNAME);
        password = properties.getProperty(MAIL_PASSWORD);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(MAIL_URL, url);
        properties.setProperty(MAIL_USERNAME, username);
        properties.setProperty(MAIL_PASSWORD, password);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(MAIL_URL, MAIL_USERNAME, MAIL_PASSWORD);
        load(properties);
    }
}
