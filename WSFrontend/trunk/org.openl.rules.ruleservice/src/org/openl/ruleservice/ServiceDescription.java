package org.openl.ruleservice;

import java.util.List;

import org.openl.rules.common.CommonVersion;


public class ServiceDescription {
    private String name;
    private String url;
    private String serviceClassName;
    private List<ModuleConfiguration> modulesToLoad;
    
    public static class ModuleConfiguration{
        private String deploymentName;
        private CommonVersion deploymentVersion;
        private String projectName;
        private String moduleName;
    }
    
}
