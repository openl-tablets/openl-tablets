package org.openl.rules.webstudio.web.admin;

import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.util.StringUtils;

public class LocalRepositorySettings extends RepositorySettings {

    private final static String URI_SUFFIX = ".uri";
    private final static String BASE_DEPLOY_PATH_SUFFIX = ".base.path";
    private final static String SUPPORT_DEPLOYMENTS_SUFFIX = ".support-deployments";

    @Parameter(description = "Local path")
    @SettingPropertyName(URI_SUFFIX)
    @NotBlank
    private String uri;

    private final String uriProperty;
    private final String baseDeployPathProperty;
    private final String supportDeploymentsProperty;

    public LocalRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        this.uriProperty = configPrefix + URI_SUFFIX;
        this.baseDeployPathProperty = configPrefix + BASE_DEPLOY_PATH_SUFFIX;
        this.supportDeploymentsProperty = configPrefix + SUPPORT_DEPLOYMENTS_SUFFIX;

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(uriProperty);
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = StringUtils.trimToEmpty(uri);
    }
}
