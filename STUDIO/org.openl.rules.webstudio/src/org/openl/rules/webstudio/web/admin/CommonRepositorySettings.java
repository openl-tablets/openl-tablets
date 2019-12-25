package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;
import org.openl.util.StringUtils;

public class CommonRepositorySettings extends RepositorySettings {
    private String login;
    private String password;
    private String uri;
    private boolean secure = false;
    private final RepositoryType repositoryType;
    private final String REPOSITORY_URI;
    private final String REPOSITORY_LOGIN;
    private final String REPOSITORY_PASS;
    private String CONFIG_PREFIX;

    public CommonRepositorySettings(PropertiesHolder properties, String configPrefix, RepositoryType repositoryType) {
        super(properties, configPrefix);
        CONFIG_PREFIX = configPrefix;
        this.repositoryType = repositoryType;
        REPOSITORY_URI = configPrefix + ".uri";
        REPOSITORY_LOGIN = configPrefix + ".login";
        REPOSITORY_PASS = configPrefix + ".password";
        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(REPOSITORY_URI);
        login = properties.getProperty(REPOSITORY_LOGIN);
        password = properties.getPassword(REPOSITORY_PASS);

        secure = StringUtils.isNotEmpty(getLogin());
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

    private String getDefaultPath(RepositoryType repositoryType) {
        String type = RepositorySettings.getTypePrefix(CONFIG_PREFIX);
        switch (repositoryType) {
            case DB:
                return "jdbc:mysql://localhost:3306/" + type + "-repository";
            case JNDI:
                return "java:comp/env/jdbc/" + type + "DB";
        }
        return null;
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);
        propertiesHolder.setProperty(REPOSITORY_URI, uri);

        if (!isSecure()) {
            propertiesHolder.setProperty(REPOSITORY_LOGIN, "");
            propertiesHolder.setPassword(REPOSITORY_PASS, "");
        } else {
            if (StringUtils.isNotEmpty(password)) {
                propertiesHolder.setProperty(REPOSITORY_LOGIN, getLogin());
                propertiesHolder.setPassword(REPOSITORY_PASS, getPassword());
            }
        }
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(REPOSITORY_URI, REPOSITORY_LOGIN, REPOSITORY_PASS);
        load(properties);
    }

    @Override
    protected void onTypeChanged(RepositoryType newRepositoryType) {
        super.onTypeChanged(newRepositoryType);
        uri = getDefaultPath(newRepositoryType);
    }

    @Override
    public void copyContent(RepositorySettings other) {
        super.copyContent(other);
        if (other instanceof CommonRepositorySettings) {
            CommonRepositorySettings otherSettings = (CommonRepositorySettings) other;
            setPath(otherSettings.getPath());
            setLogin(otherSettings.getLogin());
            setPassword(otherSettings.getPassword());
            setSecure(otherSettings.isSecure());
        }
    }

}
