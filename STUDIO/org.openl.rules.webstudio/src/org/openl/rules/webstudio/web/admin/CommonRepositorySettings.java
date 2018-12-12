package org.openl.rules.webstudio.web.admin;

import java.io.File;

import org.openl.config.ConfigurationManager;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.util.StringUtils;

public class CommonRepositorySettings extends RepositorySettings {
    private String login;
    private String password;
    private String uri;
    private boolean secure = false;
    private String defaultLocalUri = null;
    private final RepositoryType repositoryType;
    private final JcrType jcrType;
    private final ConfigurationManager configManager;
    final String REPOSITORY_URI;
    final String REPOSITORY_LOGIN;
    final String REPOSITORY_PASS;

    public CommonRepositorySettings(ConfigurationManager configManager,
            String configPrefix,
            RepositoryType repositoryType,
            JcrType jcrType) {
        super(configManager, configPrefix);
        this.configManager = configManager;
        this.repositoryType = repositoryType;
        this.jcrType = jcrType;
        REPOSITORY_URI = configPrefix + "uri";
        REPOSITORY_LOGIN = configPrefix + "login";
        REPOSITORY_PASS = configPrefix + "password";


        if (jcrType == JcrType.LOCAL) {
            defaultLocalUri = configManager.getStringProperty(REPOSITORY_URI);
        }

        uri = configManager.getStringProperty(REPOSITORY_URI);
        login = configManager.getStringProperty(REPOSITORY_LOGIN);
        password = configManager.getPassword(REPOSITORY_PASS);
    }

    public String getPath() {
        // Default values
        if (StringUtils.isEmpty(uri)) {
            String defaultPath = getDefaultPath(jcrType);
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

    String getDefaultPath(JcrType jcrType) {
        String type = repositoryType == RepositoryType.DESIGN ? "design" : "deployment";
        switch (jcrType) {
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
    protected void onTypeChanged(JcrType newJcrType) {
        super.onTypeChanged(newJcrType);
        uri = getDefaultPath(newJcrType);
        if (JcrType.LOCAL == newJcrType) {
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
