package org.openl.rules.webstudio.web.admin;

import java.util.Optional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.rest.settings.model.validation.NewBranchNamePatternConstraint;
import org.openl.rules.rest.settings.model.validation.RegexpConstraint;
import org.openl.rules.rest.validation.PathConstraint;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;

public class GitRepositorySettings extends RepositorySettings {

    private final static String URI_SUFFIX = ".uri";
    private final static String LOGIN_SUFFIX = ".login";
    private final static String PASSWORD_SUFFIX = ".password";
    private final static String LOCAL_REPOSITORY_PATH_SUFFIX = ".local-repository-path";
    private final static String BRANCH_SUFFIX = ".branch";
    private final static String NEW_BRANCH_TEMPLATE_SUFFIX = ".new-branch.pattern";
    private final static String NEW_BRANCH_REGEX_SUFFIX = ".new-branch.regex";
    private final static String NEW_BRANCH_REGEX_ERROR_SUFFIX = ".new-branch.regex-error";
    private final static String TAG_PREFIX_SUFFIX = ".tag-prefix";
    private final static String LISTENER_TIMER_PERIOD_SUFFIX = ".listener-timer-period";
    private final static String CONNECTION_TIMEOUT_SUFFIX = ".connection-timeout";
    private final static String FAILED_AUTHENTICATION_SECONDS_SUFFIX = ".failed-authentication-seconds";
    private final static String MAX_AUTHENTICATION_ATTEMPTS_SUFFIX = ".max-authentication-attempts";
    private final static String PROTECTED_BRANCHES_SUFFIX = ".protected-branches";

    @Parameter(description = "Remote repository")
    @JsonView(Views.Base.class)
    private boolean remoteRepository;

    @Parameter(description = "URL")
    @SettingPropertyName(suffix = URI_SUFFIX)
    @JsonView(Views.Base.class)
    private String uri;

    @Parameter(description = "Login")
    @SettingPropertyName(suffix = LOGIN_SUFFIX)
    @JsonView(Views.Base.class)
    private String login;

    @Parameter(description = "Password")
    @SettingPropertyName(suffix = PASSWORD_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String password;

    @Parameter(description = "Local path to directory for Git repository")
    @SettingPropertyName(suffix = LOCAL_REPOSITORY_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    @NotBlank
    @PathConstraint(allowLeadingSlash = true)
    private String localRepositoryPath;

    @Parameter(description = "The main branch to commit changes")
    @SettingPropertyName(suffix = BRANCH_SUFFIX)
    @JsonView(Views.Base.class)
    private String branch;

    @Parameter(description = "This pattern is used for new branches")
    @SettingPropertyName(suffix = NEW_BRANCH_TEMPLATE_SUFFIX)
    @JsonView(Views.Design.class)
    @NewBranchNamePatternConstraint
    private String newBranchTemplate;

    @Parameter(description = "Additional regex for new branches")
    @SettingPropertyName(suffix = NEW_BRANCH_REGEX_SUFFIX)
    @JsonView(Views.Design.class)
    @RegexpConstraint
    private String newBranchRegex;

    @Parameter(description = "Error message for regex validation")
    @SettingPropertyName(suffix = NEW_BRANCH_REGEX_ERROR_SUFFIX)
    @JsonView(Views.Design.class)
    private String newBranchRegexError;

    @Parameter(description = "Prefix for the automatically generated tag added to every commit")
    @SettingPropertyName(suffix = TAG_PREFIX_SUFFIX)
    @JsonView(Views.Base.class)
    private String tagPrefix;

    @Parameter(description = "Changes check interval (sec)")
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

    @Parameter(description = "Maximum number of authentication attempts")
    @SettingPropertyName(suffix = MAX_AUTHENTICATION_ATTEMPTS_SUFFIX)
    @JsonView(Views.Base.class)
    private Integer maxAuthenticationAttempts;

    @Parameter(description = "Comma separated list of protected branches.")
    @SettingPropertyName(suffix = PROTECTED_BRANCHES_SUFFIX)
    @JsonView(Views.Base.class)
    private String protectedBranches;

    private final String URI;
    private final String LOGIN;
    private final String PASSWORD;
    private final String LOCAL_REPOSITORY_PATH;
    private final String BRANCH;
    private final String NEW_BRANCH_TEMPLATE;
    private final String NEW_BRANCH_REGEX;
    private final String NEW_BRANCH_REGEX_ERROR;
    private final String TAG_PREFIX;
    private final String LISTENER_TIMER_PERIOD;
    private final String CONNECTION_TIMEOUT;
    private final String FAILED_AUTHENTICATION_SECONDS;
    private final String MAX_AUTHENTICATION_ATTEMPTS;
    private final String PROTECTED_BRANCHES;

    GitRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        URI = configPrefix + URI_SUFFIX;
        LOGIN = configPrefix + LOGIN_SUFFIX;
        PASSWORD = configPrefix + PASSWORD_SUFFIX;
        LOCAL_REPOSITORY_PATH = configPrefix + LOCAL_REPOSITORY_PATH_SUFFIX;
        BRANCH = configPrefix + BRANCH_SUFFIX;
        NEW_BRANCH_TEMPLATE = configPrefix + NEW_BRANCH_TEMPLATE_SUFFIX;
        NEW_BRANCH_REGEX = configPrefix + NEW_BRANCH_REGEX_SUFFIX;
        NEW_BRANCH_REGEX_ERROR = configPrefix + NEW_BRANCH_REGEX_ERROR_SUFFIX;
        TAG_PREFIX = configPrefix + TAG_PREFIX_SUFFIX;
        LISTENER_TIMER_PERIOD = configPrefix + LISTENER_TIMER_PERIOD_SUFFIX;
        CONNECTION_TIMEOUT = configPrefix + CONNECTION_TIMEOUT_SUFFIX;
        FAILED_AUTHENTICATION_SECONDS = configPrefix + FAILED_AUTHENTICATION_SECONDS_SUFFIX;
        MAX_AUTHENTICATION_ATTEMPTS = configPrefix + MAX_AUTHENTICATION_ATTEMPTS_SUFFIX;
        PROTECTED_BRANCHES = configPrefix + PROTECTED_BRANCHES_SUFFIX;

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        String localPath = properties.getProperty(LOCAL_REPOSITORY_PATH);
        String[] prefixParts = getConfigPrefix().split("\\.");
        String id = prefixParts.length > 1 ? prefixParts[1] : "repository";
        // prefixParts.length must be always > 1
        String defaultLocalPath = localPath != null ? localPath
                : properties.getProperty(
                DynamicPropertySource.OPENL_HOME) + "/repositories/" + id;

        uri = properties.getProperty(URI);
        login = properties.getProperty(LOGIN);
        password = properties.getProperty(PASSWORD);
        localRepositoryPath = defaultLocalPath;
        branch = properties.getProperty(BRANCH);
        tagPrefix = properties.getProperty(TAG_PREFIX);
        listenerTimerPeriod = Optional.ofNullable(properties.getProperty(LISTENER_TIMER_PERIOD)).map(Integer::parseInt)
                .orElse(null);
        connectionTimeout = Optional.ofNullable(properties.getProperty(CONNECTION_TIMEOUT)).map(Integer::parseInt)
                .orElse(null);
        failedAuthenticationSeconds = Optional.ofNullable(properties.getProperty(FAILED_AUTHENTICATION_SECONDS))
                .map(Integer::parseInt)
                .orElse(null);
        maxAuthenticationAttempts = Optional.ofNullable(properties.getProperty(MAX_AUTHENTICATION_ATTEMPTS))
                .filter(StringUtils::isNotBlank)
                .map(Integer::parseInt)
                .orElse(null);
        newBranchTemplate = properties.getProperty(NEW_BRANCH_TEMPLATE);
        newBranchRegex = properties.getProperty(NEW_BRANCH_REGEX);
        newBranchRegexError = properties.getProperty(NEW_BRANCH_REGEX_ERROR);
        protectedBranches = properties.getProperty(PROTECTED_BRANCHES);

        remoteRepository = StringUtils.isNotBlank(uri);
    }

    public boolean isRemoteRepository() {
        return remoteRepository;
    }

    public void setRemoteRepository(boolean remoteRepository) {
        this.remoteRepository = remoteRepository;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocalRepositoryPath() {
        return localRepositoryPath;
    }

    public void setLocalRepositoryPath(String localRepositoryPath) {
        this.localRepositoryPath = localRepositoryPath;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getTagPrefix() {
        return tagPrefix;
    }

    public void setTagPrefix(String tagPrefix) {
        this.tagPrefix = tagPrefix;
    }

    public Integer getListenerTimerPeriod() {
        return listenerTimerPeriod;
    }

    public void setListenerTimerPeriod(Integer listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
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

    public Integer getMaxAuthenticationAttempts() {
        return maxAuthenticationAttempts;
    }

    public void setMaxAuthenticationAttempts(Integer maxAuthenticationAttempts) {
        this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    public String getNewBranchTemplate() {
        return newBranchTemplate;
    }

    public void setNewBranchTemplate(String newBranchTemplate) {
        this.newBranchTemplate = newBranchTemplate;
    }

    public String getNewBranchRegex() {
        return newBranchRegex;
    }

    public void setNewBranchRegex(String newBranchRegex) {
        this.newBranchRegex = newBranchRegex;
    }

    public String getNewBranchRegexError() {
        return newBranchRegexError;
    }

    public void setNewBranchRegexError(String newBranchRegexError) {
        this.newBranchRegexError = newBranchRegexError;
    }

    public String getProtectedBranches() {
        return protectedBranches;
    }

    public void setProtectedBranches(String protectedBranches) {
        this.protectedBranches = protectedBranches;
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);

        boolean clearLogin = StringUtils.isEmpty(login);

        if (isRemoteRepository()) {
            propertiesHolder.setProperty(URI, uri);
        } else {
            propertiesHolder.setProperty(URI, "");
            clearLogin = true;
        }

        if (clearLogin) {
            propertiesHolder.setProperty(LOGIN, "");
            propertiesHolder.setProperty(PASSWORD, "");
        } else {
            propertiesHolder.setProperty(LOGIN, getLogin());
            propertiesHolder.setProperty(PASSWORD, getPassword());
        }

        propertiesHolder.setProperty(LOCAL_REPOSITORY_PATH, localRepositoryPath);
        propertiesHolder.setProperty(BRANCH, branch);
        propertiesHolder.setProperty(NEW_BRANCH_TEMPLATE, newBranchTemplate);
        propertiesHolder.setProperty(NEW_BRANCH_REGEX, newBranchRegex);
        propertiesHolder.setProperty(NEW_BRANCH_REGEX_ERROR, newBranchRegexError);
        propertiesHolder.setProperty(TAG_PREFIX, tagPrefix);
        propertiesHolder.setProperty(LISTENER_TIMER_PERIOD, listenerTimerPeriod);
        propertiesHolder.setProperty(CONNECTION_TIMEOUT, connectionTimeout);
        propertiesHolder.setProperty(FAILED_AUTHENTICATION_SECONDS, failedAuthenticationSeconds);
        propertiesHolder.setProperty(MAX_AUTHENTICATION_ATTEMPTS, maxAuthenticationAttempts);
        propertiesHolder.setProperty(PROTECTED_BRANCHES, protectedBranches);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(URI,
                LOGIN,
                PASSWORD,
                LOCAL_REPOSITORY_PATH,
                BRANCH,
                NEW_BRANCH_TEMPLATE,
                NEW_BRANCH_REGEX,
                NEW_BRANCH_REGEX_ERROR,
                TAG_PREFIX,
                LISTENER_TIMER_PERIOD,
                PROTECTED_BRANCHES);
        load(properties);
    }

    @Override
    public RepositorySettingsValidators getValidators() {
        return new GitRepositorySettingsValidators();
    }
}
