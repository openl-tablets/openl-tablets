package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;

public abstract class RepositorySettings {
    public static final String VERSION_IN_DEPLOYMENT_NAME = "version-in-deployment-name";

    private boolean includeVersionInDeploymentName;

    public boolean isIncludeVersionInDeploymentName() {
        return includeVersionInDeploymentName;
    }

    public void setIncludeVersionInDeploymentName(boolean includeVersionInDeploymentName) {
        this.includeVersionInDeploymentName = includeVersionInDeploymentName;
    }

    public RepositorySettings(ConfigurationManager configManager, String configPrefix) {
        includeVersionInDeploymentName = Boolean.valueOf(configManager.getStringProperty(VERSION_IN_DEPLOYMENT_NAME));
    }

    protected void fixState() {
    }

    protected void store(ConfigurationManager configurationManager) {
        configurationManager.setProperty(VERSION_IN_DEPLOYMENT_NAME, includeVersionInDeploymentName);
    }

    protected void onTypeChanged(RepositoryType newRepositoryType) {
    }

    public void copyContent(RepositorySettings other) {
        setIncludeVersionInDeploymentName(other.isIncludeVersionInDeploymentName());
    }

    public RepositorySettingsValidators getValidators() {
        return new RepositorySettingsValidators();
    }
}
