package org.openl.rules.webstudio.web.admin;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.util.StringUtils;

public class CommonRepositorySettings extends RepositorySettings {

    private static final String URI_PATH_SUFFIX = ".uri";
    private static final String LOGIN_PATH_SUFFIX = ".login";
    private static final String PASSWORD_PATH_SUFFIX = ".password";

    @Parameter(description = "Login")
    @SettingPropertyName(suffix = LOGIN_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    private String login;

    @Parameter(description = "Password")
    @SettingPropertyName(suffix = PASSWORD_PATH_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String password;

    @Parameter(description = "URL")
    @SettingPropertyName(suffix = URI_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    @NotBlank(message = "URL is required")
    private String uri;

    @Parameter(description = "Secure connection")
    @JsonView(Views.Base.class)
    private boolean secure;

    private final String uriPath;
    private final String loginPath;
    private final String passwordPath;

    public CommonRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        uriPath = configPrefix + URI_PATH_SUFFIX;
        loginPath = configPrefix + LOGIN_PATH_SUFFIX;
        passwordPath = configPrefix + PASSWORD_PATH_SUFFIX;
        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(uriPath);
        login = properties.getProperty(loginPath);
        password = properties.getProperty(passwordPath);

        secure = StringUtils.isNotEmpty(getLogin());
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = StringUtils.trimToEmpty(uri);
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
