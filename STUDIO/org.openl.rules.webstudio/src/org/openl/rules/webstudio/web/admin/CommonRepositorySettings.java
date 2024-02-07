package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;
import org.openl.util.StringUtils;

public class CommonRepositorySettings extends RepositorySettings {
    private String login;
    private String password;
    private String uri;
    private boolean secure;
    private final String uriPath;
    private final String loginPath;
    private final String passwordPath;

    public CommonRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
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

}
