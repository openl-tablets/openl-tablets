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
    private static final Logger LOG = LoggerFactory.getLogger(ReadOnlyPropertiesHolder.class);

    public ReadOnlyPropertiesHolder(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @Override
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }

    private String getRepoPassKey() {
        String passKey = getProperty(REPO_PASS_KEY);
        return passKey != null ? StringUtils.trimToEmpty(passKey) : "";
    }

    @Override
    public final String getProperty(String key) {
        String value = doGetProperty(key);
        if (key.endsWith("password")) {
            try {
                return PassCoder.decode(value, getRepoPassKey());
            } catch (Exception e) {
                LOG.error("Error when getting password property: {}", key, e);
                return "";
            }
        }
        return value;
    }

    @Override
    public final void setProperty(String key, Object value) {
        String propValue = value != null ? value.toString() : null;
        if (key.endsWith("password")) {
            try {
                propValue = PassCoder.encode(propValue, getRepoPassKey());
            } catch (Exception e) {
                LOG.error("Error when setting password property: {}", key, e);
                return;
            }
        }
        doSetProperty(key, propValue);
    }

    protected String doGetProperty(String key) {
        return propertyResolver.getProperty(key);
    }

    protected void doSetProperty(String key, String value) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public void revertProperties(String... keys) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public void writeTo(File file) throws IOException {
        throw new UnsupportedOperationException("Editing isn't supported");
    }
}
