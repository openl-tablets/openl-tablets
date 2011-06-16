package org.openl.rules.ruleservice;

import java.util.List;

import org.openl.rules.common.CommonVersion;

public class ServiceDescription {
    private String name;
    private String url;
    private String serviceClassName;
    private boolean provideRuntimeContext;
    private List<ModuleConfiguration> modulesToLoad;

    public ServiceDescription() {
    }

    public ServiceDescription(String name, String url, String serviceClassName, boolean provideRuntimeContext,
            List<ModuleConfiguration> modulesToLoad) {
        this.name = name;
        this.url = url;
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
        this.modulesToLoad = modulesToLoad;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public void setServiceClassName(String serviceClassName) {
        this.serviceClassName = serviceClassName;
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public List<ModuleConfiguration> getModulesToLoad() {
        return modulesToLoad;
    }

    public void setModulesToLoad(List<ModuleConfiguration> modulesToLoad) {
        this.modulesToLoad = modulesToLoad;
    }

    public static class ModuleConfiguration {
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

}
