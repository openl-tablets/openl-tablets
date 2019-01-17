package org.openl.rules.webstudio.web.admin;

import java.io.File;

import org.openl.config.ConfigurationManager;
import org.openl.util.StringUtils;

public class CommonRepositorySettings extends RepositorySettings {
    private String login;
    private String password;
    private String uri;
    private boolean secure = false;
    private String defaultLocalUri = null;
    private final RepositoryMode repositoryMode;
    private final RepositoryType repositoryType;
    private final ConfigurationManager configManager;
    final String REPOSITORY_URI;
    final String REPOSITORY_LOGIN;
    final String REPOSITORY_PASS;

    public CommonRepositorySettings(ConfigurationManager configManager,
            String configPrefix,
            RepositoryMode repositoryMode,
            RepositoryType repositoryType) {
        super(configManager, configPrefix);
        this.configManager = configManager;
        this.repositoryMode = repositoryMode;
        this.repositoryType = repositoryType;
        REPOSITORY_URI = configPrefix + "uri";
        REPOSITORY_LOGIN = configPrefix + "login";
        REPOSITORY_PASS = configPrefix + "password";


        if (repositoryType == RepositoryType.LOCAL) {
            defaultLocalUri = configManager.getStringProperty(REPOSITORY_URI);
        }

        uri = configManager.getStringProperty(REPOSITORY_URI);
        login = configManager.getStringProperty(REPOSITORY_LOGIN);
        password = configManager.getPassword(REPOSITORY_PASS);
    }

    public String getPath() {
        // Default values
        if (StringUtils.isEmpty(uri)) {
            String defaultPath = getDefaultPath(repositoryType);
            if (defaultPath != null) {
                return defaultPath;
            }
        }
        return uri;
    }

    public void setPath(String path) {
        this.uri = StringUtils.trimToEmpty(path);
    }

    public boolean isRepositoryPathSystem() {
        return configManager.isSystemProperty(REPOSITORY_URI);
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

    public void setPassword(String pass) {
        this.password = pass;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    String getDefaultPath(RepositoryType repositoryType) {
        String type = repositoryMode == RepositoryMode.DESIGN ? "design" : "deployment";
        switch (repositoryType) {
            case LOCAL:
                return defaultLocalUri != null ?
                       defaultLocalUri :
                       System.getProperty("webstudio.home") + File.separator + type + "-repository";
            case RMI:
                return "//localhost:1099/" + type + "-repository";
            case WEBDAV:
                return "http://localhost:8080/" + type + "-repository";
            case DB:
                return "jdbc:mysql://localhost:3306/" + type + "-repository";
            case JNDI:
                return "java:comp/env/jdbc/" + type + "DB";
        }
        return null;
    }

    @Override
    protected void fixState() {
        super.fixState();
        secure = StringUtils.isNotEmpty(getLogin());
    }

    @Override
    protected void store(ConfigurationManager configurationManager) {
        super.store(configurationManager);
        configurationManager.setProperty(REPOSITORY_URI, uri);

        if (!isSecure()) {
            configurationManager.removeProperty(REPOSITORY_LOGIN);
            configurationManager.removeProperty(REPOSITORY_PASS);
        } else {
            if (StringUtils.isNotEmpty(password)) {
                configurationManager.setProperty(REPOSITORY_LOGIN, getLogin());
                configurationManager.setPassword(REPOSITORY_PASS, getPassword());
            }
        }
    }

    @Override
    protected void onTypeChanged(RepositoryType newRepositoryType) {
        super.onTypeChanged(newRepositoryType);
        uri = getDefaultPath(newRepositoryType);
        if (RepositoryType.LOCAL == newRepositoryType) {
            setSecure(false);
        }
    }

    @Override
    public void copyContent(RepositorySettings other) {
        super.copyContent(other);
        if (other instanceof CommonRepositorySettings) {
            CommonRepositorySettings otherSettings = (CommonRepositorySettings) other;
            setPath(otherSettings.getPath());
            setLogin(otherSettings.getLogin());
            setPassword(otherSettings.getPassword());
            // Needed for default local repository when creating or connecting to new repositories.
            defaultLocalUri = otherSettings.defaultLocalUri;
        } else {
            // Needed for default local repository when creating or connecting to new repositories.
            defaultLocalUri = null;
        }
    }

}
