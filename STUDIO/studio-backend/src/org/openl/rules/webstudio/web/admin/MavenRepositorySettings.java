package org.openl.rules.webstudio.web.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import org.openl.config.PropertiesHolder;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.util.StringUtils;

/**
 * The Maven repository OpenL Studio downloads the dependencies a {@code rules.xml} names by Maven coordinates from.
 *
 * <p>An empty URL downloads nothing. The cleanup removes what the projects have not used for the period, and what
 * they used longest ago while the downloads exceed the threshold.
 *
 * @author Yury Molchan
 */
@Getter
@Setter
public class MavenRepositorySettings implements SettingsHolder {

    public static final String MAVEN_REPOSITORY_URL = "maven.repository.url";
    public static final String MAVEN_REPOSITORY_USERNAME = "maven.repository.username";
    public static final String MAVEN_REPOSITORY_PASSWORD = "maven.repository.password";
    public static final String MAVEN_CLEANUP_PERIOD = "maven.repository.cleanup.period";
    public static final String MAVEN_CLEANUP_THRESHOLD = "maven.repository.cleanup.threshold";

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

    @Parameter(description = "Days a downloaded dependency stays unused before it is removed. 0 keeps it.",
            example = "30")
    @SettingPropertyName(MAVEN_CLEANUP_PERIOD)
    @Min(value = 0, message = "The cleanup period cannot be negative.")
    private Integer cleanupPeriod;

    @Parameter(description = "Size in MB above which the dependencies used longest ago are removed. 0: no limit.",
            example = "1024")
    @SettingPropertyName(MAVEN_CLEANUP_THRESHOLD)
    @Min(value = 0, message = "The cleanup threshold cannot be negative.")
    private Integer cleanupThreshold;

    @Override
    public void load(PropertiesHolder properties) {
        url = properties.getProperty(MAVEN_REPOSITORY_URL);
        username = properties.getProperty(MAVEN_REPOSITORY_USERNAME);
        password = properties.getProperty(MAVEN_REPOSITORY_PASSWORD);
        cleanupPeriod = number(properties, MAVEN_CLEANUP_PERIOD);
        cleanupThreshold = number(properties, MAVEN_CLEANUP_THRESHOLD);
    }

    /** The number the property holds, or nothing when it holds none, as a value set by hand may. */
    private static @Nullable Integer number(PropertiesHolder properties, String name) {
        var value = properties.getProperty(name);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(MAVEN_REPOSITORY_URL, url);
        properties.setProperty(MAVEN_REPOSITORY_USERNAME, username);
        properties.setProperty(MAVEN_REPOSITORY_PASSWORD, password);
        properties.setProperty(MAVEN_CLEANUP_PERIOD, cleanupPeriod);
        properties.setProperty(MAVEN_CLEANUP_THRESHOLD, cleanupThreshold);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(MAVEN_REPOSITORY_URL,
                MAVEN_REPOSITORY_USERNAME,
                MAVEN_REPOSITORY_PASSWORD,
                MAVEN_CLEANUP_PERIOD,
                MAVEN_CLEANUP_THRESHOLD);
        load(properties);
    }
}
