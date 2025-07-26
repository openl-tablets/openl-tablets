package org.openl.rules.webstudio.web.admin;

import org.openl.config.PropertiesHolder;
import org.openl.util.StringUtils;

public class LocalRepositorySettings extends RepositorySettings {

    private String uri;

    private final String uriProperty;
    private final String baseDeployPathProperty;

    public LocalRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        this.uriProperty = configPrefix + ".uri";
        this.baseDeployPathProperty = configPrefix + ".base.path";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(uriProperty);
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);
        propertiesHolder.setProperty(uriProperty, uri);
        propertiesHolder.setProperty(baseDeployPathProperty, "");
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(uriProperty, baseDeployPathProperty);
        load(properties);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = StringUtils.trimToEmpty(uri);
    }
}
