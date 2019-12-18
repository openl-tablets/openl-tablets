package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;
import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.config.PassCoder;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

public class CommonRepositorySettings extends RepositorySettings {
    private final Logger log = LoggerFactory.getLogger(CommonRepositorySettings.class);

    private String login;
    private String password;
    private String uri;
    private boolean secure = false;
    private final RepositoryType repositoryType;
    final String REPOSITORY_URI;
    final String REPOSITORY_LOGIN;
    final String REPOSITORY_PASS;
    private PropertyResolver propertyResolver;
    private String CONFIG_PREFIX;

    public CommonRepositorySettings(PropertyResolver propertyResolver,
            String configPrefix,
            RepositoryType repositoryType) {
        super(propertyResolver, configPrefix);
        CONFIG_PREFIX = configPrefix;
        this.repositoryType = repositoryType;
        REPOSITORY_URI = configPrefix + ".uri";
        REPOSITORY_LOGIN = configPrefix + ".login";
        REPOSITORY_PASS = configPrefix + ".password";
        this.propertyResolver = propertyResolver;
        load(propertyResolver);
    }

    private void load(PropertyResolver propertyResolver) {
        uri = propertyResolver.getProperty(REPOSITORY_URI);
        login = propertyResolver.getProperty(REPOSITORY_LOGIN);
        String propertyKeyValue = propertyResolver.getProperty("repository.encode.decode.key");
        String key = propertyKeyValue != null ? StringUtils.trimToEmpty(propertyKeyValue) : "";
        String pass = propertyResolver.getProperty(REPOSITORY_PASS);
        String encodedPassword = null;
        if (StringUtils.isEmpty(key)) {
            encodedPassword = pass;
        } else {
            try {
                encodedPassword = PassCoder.decode(pass, key);
            } catch (Exception e) {
                log.error("Error when getting password property: {}", key, e);
            }
        }
        password = encodedPassword;
        fixState();
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
        return propertyResolver.getProperty(REPOSITORY_URI) != null;
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
        String type = CONFIG_PREFIX;

        switch (repositoryType) {
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
    protected void revert(ConfigurationManager configurationManager) {
        super.revert(configurationManager);

        configurationManager.revertProperties(REPOSITORY_URI, REPOSITORY_LOGIN, REPOSITORY_PASS);
        load(null);
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
        }
    }

}
