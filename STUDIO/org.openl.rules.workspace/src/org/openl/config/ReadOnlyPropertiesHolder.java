package org.openl.config;

import java.io.File;
import java.io.IOException;

import org.openl.rules.repository.config.PassCoder;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

public class ReadOnlyPropertiesHolder implements PropertiesHolder {
    private static final String REPO_PASS_KEY = "repository.encode.decode.key";
    protected final PropertyResolver propertyResolver;
    private final Logger log = LoggerFactory.getLogger(ReadOnlyPropertiesHolder.class);

    public ReadOnlyPropertiesHolder(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @Override
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }

    @Override
    public String getPassword(String key) {
        try {
            String repoPassKey = getRepoPassKey();
            String pass = getProperty(key);
            return StringUtils.isEmpty(repoPassKey) ? pass : PassCoder.decode(pass, repoPassKey);
        } catch (Exception e) {
            log.error("Error when getting password property: {}", key, e);
            return "";
        }
    }

    @Override
    public void setPassword(String key, String pass) {
        try {
            String repoPassKey = getRepoPassKey();
            setProperty(key, StringUtils.isEmpty(repoPassKey) ? pass : PassCoder.encode(pass, repoPassKey));
        } catch (Exception e) {
            Logger log = LoggerFactory.getLogger(ReadOnlyPropertiesHolder.class);
            log.error("Error when setting password property: {}", key, e);
        }
    }

    private String getRepoPassKey() {
        String passKey = getProperty(REPO_PASS_KEY);
        return passKey != null ? StringUtils.trimToEmpty(passKey) : "";
    }

    @Override
    public void revertProperties(String... keys) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }


    @Override
    public String getProperty(String key) {
        return propertyResolver.getProperty(key);
    }

    @Override
    public void setProperty(String key, Object value) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public void revertProperty(String key) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public void writeTo(File file) throws IOException {
        throw new UnsupportedOperationException("Editing isn't supported");
    }
}
