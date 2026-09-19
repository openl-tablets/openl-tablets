package org.openl.rules.webstudio.web.admin;

import java.util.Optional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.rest.validation.PathConstraint;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.studio.settings.model.constraint.NewBranchNamePatternConstraint;
import org.openl.studio.settings.model.constraint.RegexpConstraint;
import org.openl.util.StringUtils;

@Schema(allOf = RepositorySettings.class)
public class GitRepositorySettings extends RepositorySettings {

    public static final String URI_SUFFIX = ".uri";
    private static final String LOGIN_SUFFIX = ".login";
    private static final String PASSWORD_SUFFIX = ".password";
    private static final String BRANCH_SUFFIX = ".branch";
    private static final String NEW_BRANCH_TEMPLATE_SUFFIX = ".new-branch.pattern";
    private static final String NEW_BRANCH_REGEX_SUFFIX = ".new-branch.regex";
    private static final String NEW_BRANCH_REGEX_ERROR_SUFFIX = ".new-branch.regex-error";
    private static final String TAG_PREFIX_SUFFIX = ".tag-prefix";
    private static final String LISTENER_TIMER_PERIOD_SUFFIX = ".listener-timer-period";
    private static final String CONNECTION_TIMEOUT_SUFFIX = ".connection-timeout";
    private static final String FAILED_AUTHENTICATION_SECONDS_SUFFIX = ".failed-authentication-seconds";
    private static final String MAX_AUTHENTICATION_ATTEMPTS_SUFFIX = ".max-authentication-attempts";
    private static final String PROTECTED_BRANCHES_SUFFIX = ".protected-branches";

    @Getter
    @Parameter(description = "URL")
    @SettingPropertyName(suffix = URI_SUFFIX)
    @JsonView(Views.Base.class)
    @NotBlank
    @PathConstraint(allowLeadingSlash = true, allowedSchemes = {"http", "https"})
    @Setter
    private String uri;

    @Getter
    @Parameter(description = "Login")
    @Setter
    @SettingPropertyName(suffix = LOGIN_SUFFIX)
    @JsonView(Views.Base.class)
    private String login;

    @Getter
    @Parameter(description = "Password")
    @Setter
    @SettingPropertyName(suffix = PASSWORD_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String password;

    @Getter
    @Parameter(description = "The main branch to commit changes")
    @Setter
    @SettingPropertyName(suffix = BRANCH_SUFFIX)
    @JsonView(Views.Base.class)
    private String branch;

    @Getter
    @Parameter(description = "This pattern is used for new branches")
    @Setter
    @SettingPropertyName(suffix = NEW_BRANCH_TEMPLATE_SUFFIX)
    @JsonView(Views.Design.class)
    @NewBranchNamePatternConstraint
    private String newBranchTemplate;

    @Getter
    @Parameter(description = "Additional regex for new branches")
    @SettingPropertyName(suffix = NEW_BRANCH_REGEX_SUFFIX)
    @JsonView(Views.Design.class)
    @RegexpConstraint
    @Setter
    private String newBranchRegex;

    @Getter
    @Parameter(description = "Error message for regex validation")
    @Setter
    @SettingPropertyName(suffix = NEW_BRANCH_REGEX_ERROR_SUFFIX)
    @JsonView(Views.Design.class)
    private String newBranchRegexError;

    @Getter
    @Parameter(description = "Prefix for the automatically generated tag added to every commit")
    @Setter
    @SettingPropertyName(suffix = TAG_PREFIX_SUFFIX)
    @JsonView(Views.Base.class)
    private String tagPrefix;

    @Getter
    @Parameter(description = "Changes check interval (sec)")
    @Setter
    @SettingPropertyName(suffix = LISTENER_TIMER_PERIOD_SUFFIX)
    @JsonView(Views.Base.class)
    @Min(1)
    @NotNull
    private Integer listenerTimerPeriod;

    @Parameter(description = "Connection timeout (sec)")
    @SettingPropertyName(suffix = CONNECTION_TIMEOUT_SUFFIX)
    @JsonView(Views.Base.class)
    @Min(1)
    @NotNull
    private Integer connectionTimeout;

    @Parameter(description = "Time to wait between the failed authentication attempt and the next attempt (sec)")
    @SettingPropertyName(suffix = FAILED_AUTHENTICATION_SECONDS_SUFFIX)
    @JsonView(Views.Base.class)
    @Min(1)
    @NotNull
    private Integer failedAuthenticationSeconds;

    @Getter
    @Parameter(description = "Maximum number of authentication attempts")
    @Setter
    @SettingPropertyName(suffix = MAX_AUTHENTICATION_ATTEMPTS_SUFFIX)
    @JsonView(Views.Base.class)
    private Integer maxAuthenticationAttempts;

    @Getter
    @Parameter(description = "Comma separated list of protected branches.")
    @Setter
    @SettingPropertyName(suffix = PROTECTED_BRANCHES_SUFFIX)
    @JsonView(Views.Base.class)
    private String protectedBranches;

    private final String uriProperty;
    private final String loginProperty;
    private final String passwordProperty;
    private final String branchProperty;
    private final String newBranchTemplateProperty;
    private final String newBranchRegexProperty;
    private final String newBranchRegexErrorProperty;
    private final String tagPrefixProperty;
    private final String listenerTimerPeriodProperty;
    private final String connectionTimeoutProperty;
    private final String failedAuthenticationSecondsProperty;
    private final String maxAuthenticationAttemptsProperty;
    private final String protectedBranchesProperty;

    GitRepositorySettings(PropertiesHolder properties, String configPrefix, RepositoryMode repositoryMode) {
        super(properties, configPrefix, repositoryMode);
        uriProperty = configPrefix + URI_SUFFIX;
        loginProperty = configPrefix + LOGIN_SUFFIX;
        passwordProperty = configPrefix + PASSWORD_SUFFIX;
        branchProperty = configPrefix + BRANCH_SUFFIX;
        newBranchTemplateProperty = configPrefix + NEW_BRANCH_TEMPLATE_SUFFIX;
        newBranchRegexProperty = configPrefix + NEW_BRANCH_REGEX_SUFFIX;
        newBranchRegexErrorProperty = configPrefix + NEW_BRANCH_REGEX_ERROR_SUFFIX;
        tagPrefixProperty = configPrefix + TAG_PREFIX_SUFFIX;
        listenerTimerPeriodProperty = configPrefix + LISTENER_TIMER_PERIOD_SUFFIX;
        connectionTimeoutProperty = configPrefix + CONNECTION_TIMEOUT_SUFFIX;
        failedAuthenticationSecondsProperty = configPrefix + FAILED_AUTHENTICATION_SECONDS_SUFFIX;
        maxAuthenticationAttemptsProperty = configPrefix + MAX_AUTHENTICATION_ATTEMPTS_SUFFIX;
        protectedBranchesProperty = configPrefix + PROTECTED_BRANCHES_SUFFIX;

        loadProperties(properties);
    }

    private void loadProperties(PropertiesHolder properties) {
        uri = properties.getProperty(uriProperty);
        login = properties.getProperty(loginProperty);
        password = properties.getProperty(passwordProperty);
        branch = properties.getProperty(branchProperty);
        tagPrefix = properties.getProperty(tagPrefixProperty);
        listenerTimerPeriod = Optional.ofNullable(properties.getProperty(listenerTimerPeriodProperty))
                .map(Integer::parseInt)
                .orElse(null);
        connectionTimeout = Optional.ofNullable(properties.getProperty(connectionTimeoutProperty))
                .map(Integer::parseInt)
                .orElse(null);
        failedAuthenticationSeconds = Optional.ofNullable(properties.getProperty(failedAuthenticationSecondsProperty))
                .map(Integer::parseInt)
                .orElse(null);
        maxAuthenticationAttempts = Optional.ofNullable(properties.getProperty(maxAuthenticationAttemptsProperty))
                .filter(StringUtils::isNotBlank)
                .map(Integer::parseInt)
                .orElse(null);
        newBranchTemplate = properties.getProperty(newBranchTemplateProperty);
        newBranchRegex = properties.getProperty(newBranchRegexProperty);
        newBranchRegexError = properties.getProperty(newBranchRegexErrorProperty);
        protectedBranches = properties.getProperty(protectedBranchesProperty);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getFailedAuthenticationSeconds() {
        return failedAuthenticationSeconds;
    }

    public void setFailedAuthenticationSeconds(int failedAuthenticationSeconds) {
        this.failedAuthenticationSeconds = failedAuthenticationSeconds;
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);

        var clearLogin = StringUtils.isEmpty(login);

        propertiesHolder.setProperty(uriProperty, uri);

        if (clearLogin) {
            propertiesHolder.setProperty(loginProperty, "");
            propertiesHolder.setProperty(passwordProperty, "");
        } else {
            propertiesHolder.setProperty(loginProperty, getLogin());
            propertiesHolder.setProperty(passwordProperty, getPassword());
        }

        propertiesHolder.setProperty(branchProperty, branch);
        propertiesHolder.setProperty(newBranchTemplateProperty, newBranchTemplate);
        propertiesHolder.setProperty(newBranchRegexProperty, newBranchRegex);
        propertiesHolder.setProperty(newBranchRegexErrorProperty, newBranchRegexError);
        propertiesHolder.setProperty(tagPrefixProperty, tagPrefix);
        propertiesHolder.setProperty(listenerTimerPeriodProperty, listenerTimerPeriod);
        propertiesHolder.setProperty(connectionTimeoutProperty, connectionTimeout);
        propertiesHolder.setProperty(failedAuthenticationSecondsProperty, failedAuthenticationSeconds);
        propertiesHolder.setProperty(maxAuthenticationAttemptsProperty, maxAuthenticationAttempts);
        propertiesHolder.setProperty(protectedBranchesProperty, protectedBranches);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(uriProperty,
                loginProperty,
                passwordProperty,
                branchProperty,
                newBranchTemplateProperty,
                newBranchRegexProperty,
                newBranchRegexErrorProperty,
                tagPrefixProperty,
                listenerTimerPeriodProperty,
                protectedBranchesProperty);
        loadProperties(properties);
    }
}
