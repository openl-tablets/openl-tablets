package org.openl.rules.webstudio.web.admin;

import java.io.File;

import org.openl.config.PropertiesHolder;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;

public class LocalRepositorySettings extends RepositorySettings {

    private String uri;

    private final String uriProperty;
    private final String configPathPrefix;

    public LocalRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        this.configPathPrefix = configPrefix;
        this.uriProperty = configPrefix + ".uri";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        String type = RepositorySettings.getTypePrefix(configPathPrefix);
        String localPath = properties.getProperty(uriProperty);
        uri = localPath != null ? localPath
                                : properties.getProperty(
                                    DynamicPropertySource.OPENL_HOME) + File.separator + type + "-repository";
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);
        propertiesHolder.setProperty(uriProperty, uri);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(uriProperty);
        load(properties);
    }

    @Override
    public void copyContent(RepositorySettings other) {
        super.copyContent(other);
        if (other instanceof LocalRepositorySettings) {
            LocalRepositorySettings otherSettings = (LocalRepositorySettings) other;
            setUri(otherSettings.getUri());
        }
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = StringUtils.trimToEmpty(uri);
    }
}
