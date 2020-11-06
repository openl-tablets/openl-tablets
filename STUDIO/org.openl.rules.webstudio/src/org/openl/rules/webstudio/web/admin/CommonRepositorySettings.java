package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.util.StringUtils;

public class CommonRepositorySettings extends RepositorySettings {
    private String login;
    private String password;
    private String uri;
    private boolean secure;
    private final RepositoryType repositoryType;
    private final String uriPath;
    private final String loginPath;
    private final String passwordPath;
    private final String configPathPrefix;

    public CommonRepositorySettings(PropertiesHolder properties, String configPrefix, RepositoryType repositoryType) {
        super(properties, configPrefix);
        configPathPrefix = configPrefix;
        this.repositoryType = repositoryType;
        uriPath = configPrefix + ".uri";
        loginPath = configPrefix + ".login";
        passwordPath = configPrefix + ".password";
        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(uriPath);
        login = properties.getProperty(loginPath);
        password = properties.getProperty(passwordPath);

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
        String type = RepositoryMode.getTypePrefix(configPathPrefix).toString();
        switch (repositoryType) {
            case DB:
                return "jdbc:mysql://localhost:3306/" + type + "-repository";
            case JNDI:
                return "java:comp/env/jdbc/" + type + "DB";
            default:
                return null;
        }
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);
        propertiesHolder.setProperty(uriPath, uri);

        if (!isSecure()) {
            propertiesHolder.setProperty(loginPath, "");
            propertiesHolder.setProperty(passwordPath, "");
        } else {
            if (StringUtils.isNotEmpty(password)) {
                propertiesHolder.setProperty(loginPath, getLogin());
                propertiesHolder.setProperty(passwordPath, getPassword());
            }
        }
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(uriPath, loginPath, passwordPath);
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
