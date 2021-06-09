package org.openl.rules.webstudio.web.admin;

import java.util.Optional;

import org.openl.config.PropertiesHolder;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;

public class GitRepositorySettings extends RepositorySettings {
    private boolean remoteRepository;

    private String uri;
    private String login;
    private String password;
    private String userDisplayName;
    private String userEmail;
    private String localRepositoryPath;
    private String branch;
    private String newBranchTemplate;
    private String newBranchRegex;
    private String newBranchRegexError;
    private String tagPrefix;
    private int listenerTimerPeriod;
    private int connectionTimeout;
    private int failedAuthenticationSeconds;
    private Integer maxAuthenticationAttempts;

    private final String URI;
    private final String LOGIN;
    private final String PASSWORD;
    private final String USER_DISPLAY_NAME;
    private final String USER_EMAIL;
    private final String LOCAL_REPOSITORY_PATH;
    private final String BRANCH;
    private final String NEW_BRANCH_TEMPLATE;
    private final String NEW_BRANCH_REGEX;
    private final String NEW_BRANCH_REGEX_ERROR;
    private final String TAG_PREFIX;
    private final String LISTENER_TIMER_PERIOD;
    private final String CONNECTION_TIMEOUT;
    private final String CONFIG_PREFIX;
    private final String FAILED_AUTHENTICATION_SECONDS;
    private final String MAX_AUTHENTICATION_ATTEMPTS;


    GitRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        CONFIG_PREFIX = configPrefix;
        URI = configPrefix + ".uri";
        LOGIN = configPrefix + ".login";
        PASSWORD = configPrefix + ".password";
        USER_DISPLAY_NAME = configPrefix + ".user-display-name";
        USER_EMAIL = configPrefix + ".user-email";
        LOCAL_REPOSITORY_PATH = configPrefix + ".local-repository-path";
        BRANCH = configPrefix + ".branch";
        NEW_BRANCH_TEMPLATE = configPrefix + ".new-branch.pattern";
        NEW_BRANCH_REGEX = configPrefix + ".new-branch.regex";
        NEW_BRANCH_REGEX_ERROR = configPrefix + ".new-branch.regex-error";
        TAG_PREFIX = configPrefix + ".tag-prefix";
        LISTENER_TIMER_PERIOD = configPrefix + ".listener-timer-period";
        CONNECTION_TIMEOUT = configPrefix + ".connection-timeout";
        FAILED_AUTHENTICATION_SECONDS = configPrefix + ".failed-authentication-seconds";
        MAX_AUTHENTICATION_ATTEMPTS = configPrefix + ".max-authentication-attempts";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        String localPath = properties.getProperty(LOCAL_REPOSITORY_PATH);
        String[] prefixParts = CONFIG_PREFIX.split("\\.");
        String id = prefixParts.length > 1 ? prefixParts[1] : "repository";
        //prefixParts.length must be always > 1
        String defaultLocalPath = localPath != null ? localPath
                : properties.getProperty(
                DynamicPropertySource.OPENL_HOME) + "/repositories/" + id;

        uri = properties.getProperty(URI);
        login = properties.getProperty(LOGIN);
        password = properties.getProperty(PASSWORD);
        userDisplayName = properties.getProperty(USER_DISPLAY_NAME);
        userEmail = properties.getProperty(USER_EMAIL);
        localRepositoryPath = defaultLocalPath;
        branch = properties.getProperty(BRANCH);
        tagPrefix = properties.getProperty(TAG_PREFIX);
        listenerTimerPeriod = Integer.parseInt(Optional.ofNullable(properties.getProperty(LISTENER_TIMER_PERIOD)).orElse(properties.getProperty("repo-git.listener-timer-period")));
        connectionTimeout = Integer.parseInt(Optional.ofNullable(properties.getProperty(CONNECTION_TIMEOUT)).orElse(properties.getProperty("repo-git.connection-timeout")));
        failedAuthenticationSeconds = Integer.parseInt(Optional.ofNullable(properties.getProperty(FAILED_AUTHENTICATION_SECONDS)).orElse(properties.getProperty("repo-git.failed-authentication-seconds")));
        String authsAttempts = Optional.ofNullable(properties.getProperty(MAX_AUTHENTICATION_ATTEMPTS)).orElse(properties.getProperty("repo-git.max-authentication-attempts"));
        if(StringUtils.isNotBlank(authsAttempts)){
            maxAuthenticationAttempts = Integer.parseInt(authsAttempts);
        }
        newBranchTemplate = properties.getProperty(NEW_BRANCH_TEMPLATE);
        newBranchRegex = properties.getProperty(NEW_BRANCH_REGEX);
        newBranchRegexError = properties.getProperty(NEW_BRANCH_REGEX_ERROR);

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

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    public int getListenerTimerPeriod() {
        return listenerTimerPeriod;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
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

        propertiesHolder.setProperty(USER_DISPLAY_NAME, userDisplayName);
        propertiesHolder.setProperty(USER_EMAIL, userEmail);
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
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(URI,
                LOGIN,
                PASSWORD,
                USER_DISPLAY_NAME,
                USER_EMAIL,
                LOCAL_REPOSITORY_PATH,
                BRANCH,
                NEW_BRANCH_TEMPLATE,
                NEW_BRANCH_REGEX,
                NEW_BRANCH_REGEX_ERROR,
                TAG_PREFIX,
                LISTENER_TIMER_PERIOD);
        load(properties);
    }

    @Override
    public void copyContent(RepositorySettings other) {
        super.copyContent(other);

        if (other instanceof GitRepositorySettings) {
            GitRepositorySettings otherSettings = (GitRepositorySettings) other;
            setUri(otherSettings.getUri());
            setLogin(otherSettings.getLogin());
            setPassword(otherSettings.getPassword());
            setUserDisplayName(otherSettings.getUserDisplayName());
            setUserEmail(otherSettings.getUserEmail());
            setLocalRepositoryPath(otherSettings.getLocalRepositoryPath());
            setBranch(otherSettings.getBranch());
            setNewBranchTemplate(otherSettings.getNewBranchTemplate());
            setNewBranchRegex(otherSettings.getNewBranchRegex());
            setNewBranchRegexError(otherSettings.getNewBranchRegexError());
            setTagPrefix(otherSettings.getTagPrefix());
            setListenerTimerPeriod(otherSettings.getListenerTimerPeriod());
            setConnectionTimeout(otherSettings.getConnectionTimeout());
            setFailedAuthenticationSeconds(otherSettings.getFailedAuthenticationSeconds());
            setMaxAuthenticationAttempts(otherSettings.getMaxAuthenticationAttempts());
            setCommentTemplate(otherSettings.getCommentTemplate());
            setRemoteRepository(otherSettings.isRemoteRepository());
        }
    }

    @Override
    public void applyRepositorySuffix(FreeValueFinder valueFinder) {
        super.applyRepositorySuffix(valueFinder);
        String path = getLocalRepositoryPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        final String paramNameSuffix = LOCAL_REPOSITORY_PATH.substring(CONFIG_PREFIX.length() + 1);
        setLocalRepositoryPath(valueFinder.find(paramNameSuffix, path));
    }

    @Override
    public RepositorySettingsValidators getValidators() {
        return new GitRepositorySettingsValidators();
    }
}
