package org.openl.rules.ruleservice.core;

import java.util.List;


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

}
