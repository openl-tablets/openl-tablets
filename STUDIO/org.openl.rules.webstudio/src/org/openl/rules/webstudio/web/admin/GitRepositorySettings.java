package org.openl.rules.webstudio.web.admin;

import java.io.File;

import org.eclipse.jgit.lib.Constants;
import org.openl.config.ConfigurationManager;
import org.openl.rules.repository.RepositoryMode;
import org.openl.util.StringUtils;

public class GitRepositorySettings extends RepositorySettings {
    private String uri;
    private String login;
    private String password;
    private String localRepositoryPath;
    private String branch;
    private String tagPrefix;
    private int listenerTimerPeriod;
    private String commentPattern;
    private String settingsPath;

    private final String URI;
    private final String LOGIN;
    private final String PASSWORD;
    private final String LOCAL_REPOSITORY_PATH;
    private final String BRANCH;
    private final String TAG_PREFIX;
    private final String LISTENER_TIMER_PERIOD;
    private final String COMMENT_PATTERN;
    private final String SETTINGS_PATH;

    public GitRepositorySettings(ConfigurationManager configManager,
            String configPrefix,
            RepositoryMode repositoryMode) {
        super(configManager, configPrefix);

        URI = configPrefix + "uri";
        LOGIN = configPrefix + "login";
        PASSWORD = configPrefix + "password";
        LOCAL_REPOSITORY_PATH = configPrefix + "local-repository-path";
        BRANCH = configPrefix + "branch";
        TAG_PREFIX = configPrefix + "tag-prefix";
        LISTENER_TIMER_PERIOD = configPrefix + "listener-timer-period";
        COMMENT_PATTERN = "comment-pattern";
        SETTINGS_PATH = "git-settings-path";

        String type;
        switch (repositoryMode) {
            case DESIGN:
                type = "design";
                break;
            case DEPLOY_CONFIG:
                type = "deploy-config";
                break;
            case PRODUCTION:
                type = "deployment";
                break;
            default:
                throw new UnsupportedOperationException();
        }

        String localPath = configManager.getStringProperty(LOCAL_REPOSITORY_PATH);
        String defaultLocalPath = localPath != null ?
                                  localPath :
                                  System.getProperty("webstudio.home") + File.separator + type + "-repository";

        uri = configManager.getStringProperty(URI);
        login = configManager.getStringProperty(LOGIN);
        password = configManager.getPassword(PASSWORD);
        localRepositoryPath = defaultLocalPath;
        branch = configManager.getStringProperty(BRANCH, Constants.MASTER);
        tagPrefix = configManager.getStringProperty(TAG_PREFIX);
        listenerTimerPeriod = configManager.getLongProperty(LISTENER_TIMER_PERIOD, 10L).intValue();
        commentPattern = configManager.getStringProperty(COMMENT_PATTERN);
        settingsPath = configManager.getStringProperty(SETTINGS_PATH);
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

    public int getListenerTimerPeriod() {
        // Convert to seconds
        return listenerTimerPeriod;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        // Convert to milliseconds
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    public String getCommentPattern() {
        return commentPattern;
    }

    public void setCommentPattern(String commentPattern) {
        this.commentPattern = commentPattern;
    }

    public String getSettingsPath() {
        return settingsPath;
    }

    public void setSettingsPath(String settingsPath) {
        this.settingsPath = settingsPath;
    }

    @Override
    protected void store(ConfigurationManager configurationManager) {
        super.store(configurationManager);

        configurationManager.setProperty(URI, uri);

        if (StringUtils.isEmpty(login)) {
            configurationManager.removeProperty(LOGIN);
            configurationManager.removeProperty(PASSWORD);
        } else {
            configurationManager.setProperty(LOGIN, getLogin());
            configurationManager.setPassword(PASSWORD, getPassword());
        }

        configurationManager.setProperty(LOCAL_REPOSITORY_PATH, localRepositoryPath);
        configurationManager.setProperty(BRANCH, branch);
        configurationManager.setProperty(TAG_PREFIX, tagPrefix);
        configurationManager.setProperty(LISTENER_TIMER_PERIOD, listenerTimerPeriod);
        configurationManager.setProperty(COMMENT_PATTERN, commentPattern);
        configurationManager.setProperty(SETTINGS_PATH, settingsPath);
    }

    @Override
    public void copyContent(RepositorySettings other) {
        super.copyContent(other);

        if (other instanceof GitRepositorySettings) {
            GitRepositorySettings otherSettings = (GitRepositorySettings) other;
            setUri(otherSettings.getUri());
            setLogin(otherSettings.getLogin());
            setPassword(otherSettings.getPassword());
            setLocalRepositoryPath(otherSettings.getLocalRepositoryPath());
            setBranch(otherSettings.getBranch());
            setTagPrefix(otherSettings.getTagPrefix());
            setListenerTimerPeriod(otherSettings.getListenerTimerPeriod());
            setCommentPattern(otherSettings.getCommentPattern());
            setSettingsPath(otherSettings.getSettingsPath());
        }
    }
}
