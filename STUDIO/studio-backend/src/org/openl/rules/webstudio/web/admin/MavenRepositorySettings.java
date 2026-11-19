package org.openl.rules.webstudio.web.admin;

import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.studio.settings.converter.SettingPropertyName;

/**
 * The Maven repository OpenL Studio downloads the dependencies a {@code rules.xml} names by Maven coordinates from.
 *
 * <p>An empty URL downloads nothing.
 *
 * @author Yury Molchan
 */
@Getter
@Setter
public class MavenRepositorySettings implements SettingsHolder {

    public static final String MAVEN_REPOSITORY_URL = "maven.repository.url";
    public static final String MAVEN_REPOSITORY_USERNAME = "maven.repository.username";
    public static final String MAVEN_REPOSITORY_PASSWORD = "maven.repository.password";

    @Parameter(description = "URL of the Maven repository. Empty to download nothing.",
            example = "https://repo.maven.apache.org/maven2")
    @SettingPropertyName(MAVEN_REPOSITORY_URL)
    @Pattern(regexp = "\\s*|\\s*(?i:https?|file):\\S+\\s*", message = "The URL must use http, https or file.")
    private String url;

    @Parameter(description = "Username for authentication on the Maven repository", example = "openl")
    @SettingPropertyName(MAVEN_REPOSITORY_USERNAME)
    private String username;

    @Parameter(description = "Password for authentication on the Maven repository", example = "qwerty")
    @SettingPropertyName(value = MAVEN_REPOSITORY_PASSWORD, secret = true)
    private String password;

    @Override
    public void load(PropertiesHolder properties) {
        url = properties.getProperty(MAVEN_REPOSITORY_URL);
        username = properties.getProperty(MAVEN_REPOSITORY_USERNAME);
        password = properties.getProperty(MAVEN_REPOSITORY_PASSWORD);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(MAVEN_REPOSITORY_URL, url);
        properties.setProperty(MAVEN_REPOSITORY_USERNAME, username);
        properties.setProperty(MAVEN_REPOSITORY_PASSWORD, password);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(MAVEN_REPOSITORY_URL, MAVEN_REPOSITORY_USERNAME, MAVEN_REPOSITORY_PASSWORD);
        load(properties);
    }
}
