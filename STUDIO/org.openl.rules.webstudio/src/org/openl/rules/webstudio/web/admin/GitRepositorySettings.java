package org.openl.rules.webstudio.web.admin;

import java.io.File;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.util.PreferencesManager;
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
    private String tagPrefix;
    private int listenerTimerPeriod;
    private int connectionTimeout;
    private String settingsPath;

    private final String URI;
    private final String LOGIN;
    private final String PASSWORD;
    private final String USER_DISPLAY_NAME;
    private final String USER_EMAIL;
    private final String LOCAL_REPOSITORY_PATH;
    private final String BRANCH;
    private final String NEW_BRANCH_TEMPLATE;
    private final String TAG_PREFIX;
    private final String LISTENER_TIMER_PERIOD;
    private final String CONNECTION_TIMEOUT;
    private final String SETTINGS_PATH;
    private String CONFIG_PREFIX;

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
        NEW_BRANCH_TEMPLATE = configPrefix + ".new-branch-pattern";
        TAG_PREFIX = configPrefix + ".tag-prefix";
        LISTENER_TIMER_PERIOD = configPrefix + ".listener-timer-period";
        CONNECTION_TIMEOUT = configPrefix + ".connection-timeout";
        SETTINGS_PATH = ".git-settings-path";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        String type = CONFIG_PREFIX;

        String localPath = properties.getProperty(LOCAL_REPOSITORY_PATH);
        String defaultLocalPath = localPath != null ? localPath
                                                    : properties.getProperty(
                                                        PreferencesManager.WEBSTUDIO_WORKING_DIR_KEY) + File.separator + type + "-repository";

        uri = properties.getProperty(URI);
        login = properties.getProperty(LOGIN);
        password = properties.getPassword(PASSWORD);
        userDisplayName = properties.getProperty(USER_DISPLAY_NAME);
        userEmail = properties.getProperty(USER_EMAIL);
        localRepositoryPath = defaultLocalPath;
        branch = properties.getProperty(BRANCH);
        tagPrefix = properties.getProperty(TAG_PREFIX);
        listenerTimerPeriod = Integer.parseInt(properties.getProperty(LISTENER_TIMER_PERIOD));
        connectionTimeout = Integer.parseInt(properties.getProperty(CONNECTION_TIMEOUT));
        settingsPath = properties.getProperty(SETTINGS_PATH);
        newBranchTemplate = properties.getProperty(NEW_BRANCH_TEMPLATE);

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

    public String getSettingsPath() {
        return settingsPath;
    }

    public void setSettingsPath(String settingsPath) {
        this.settingsPath = settingsPath;
    }

    public String getNewBranchTemplate() {
        return newBranchTemplate;
    }

    public void setNewBranchTemplate(String newBranchTemplate) {
        this.newBranchTemplate = newBranchTemplate;
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
            propertiesHolder.setPassword(PASSWORD, "");
        } else {
            propertiesHolder.setProperty(LOGIN, getLogin());
            propertiesHolder.setPassword(PASSWORD, getPassword());
        }

        propertiesHolder.setProperty(USER_DISPLAY_NAME, userDisplayName);
        propertiesHolder.setProperty(USER_EMAIL, userEmail);
        propertiesHolder.setProperty(LOCAL_REPOSITORY_PATH, localRepositoryPath);
        propertiesHolder.setProperty(BRANCH, branch);
        propertiesHolder.setProperty(NEW_BRANCH_TEMPLATE, newBranchTemplate);
        propertiesHolder.setProperty(TAG_PREFIX, tagPrefix);
        propertiesHolder.setProperty(LISTENER_TIMER_PERIOD, listenerTimerPeriod);
        propertiesHolder.setProperty(CONNECTION_TIMEOUT, connectionTimeout);
        propertiesHolder.setProperty(SETTINGS_PATH, settingsPath);
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
            TAG_PREFIX,
            LISTENER_TIMER_PERIOD,
            SETTINGS_PATH);
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
            setTagPrefix(otherSettings.getTagPrefix());
            setListenerTimerPeriod(otherSettings.getListenerTimerPeriod());
            setConnectionTimeout(otherSettings.getConnectionTimeout());
            setCommentTemplate(otherSettings.getCommentTemplate());
            setSettingsPath(otherSettings.getSettingsPath());
            setRemoteRepository(otherSettings.isRemoteRepository());
        }
    }

    @Override
    public RepositorySettingsValidators getValidators() {
        return new GitRepositorySettingsValidators();
    }
}
