package org.openl.rules.webstudio.web.admin;

import java.io.File;

import org.openl.config.PropertiesHolder;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;

public class LocalRepositorySettings extends RepositorySettings {

    private String uri;
    private String homeDirectory;

    private final String uriProperty;
    private final String baseDeployPathProperty;
    private final String supportDeploymentsProperty;

    public LocalRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        this.uriProperty = configPrefix + ".uri";
        this.baseDeployPathProperty = configPrefix + ".base.path";
        this.supportDeploymentsProperty = configPrefix + ".support-deployments";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        String localPath = properties.getProperty(uriProperty);
        uri = localPath != null ? localPath : getDefaultPath();
        homeDirectory = properties.getProperty(DynamicPropertySource.OPENL_HOME);
    }

    private String getDefaultPath() {
        return homeDirectory + File.separator + "local-repository";
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);
        propertiesHolder.setProperty(uriProperty, uri);
        propertiesHolder.setProperty(supportDeploymentsProperty, true);
        propertiesHolder.setProperty(baseDeployPathProperty, "");
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(uriProperty, supportDeploymentsProperty, baseDeployPathProperty);
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

    @Override
    protected void onTypeChanged(RepositoryType newRepositoryType) {
        super.onTypeChanged(newRepositoryType);
        uri = getDefaultPath();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = StringUtils.trimToEmpty(uri);
    }
}
