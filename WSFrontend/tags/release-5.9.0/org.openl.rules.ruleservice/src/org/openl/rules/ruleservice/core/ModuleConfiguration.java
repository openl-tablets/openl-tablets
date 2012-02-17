package org.openl.rules.ruleservice.core;

import org.openl.rules.common.CommonVersion;

public class ModuleConfiguration {
    private String deploymentName;
    private CommonVersion deploymentVersion;
    private String projectName;
    private String moduleName;

    public ModuleConfiguration(String deploymentName, CommonVersion deploymentVersion, String projectName,
            String moduleName) {
        this.deploymentName = deploymentName;
        this.deploymentVersion = deploymentVersion;
        this.projectName = projectName;
        this.moduleName = moduleName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public CommonVersion getDeploymentVersion() {
        return deploymentVersion;
    }

    public void setDeploymentVersion(CommonVersion deploymentVersion) {
        this.deploymentVersion = deploymentVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}