package org.openl.rules.project.model;

import java.util.Map;

public class Module {
    private String name;
    private PathEntry rulesRootPath;
    private ProjectDescriptor project;
    private Map<String, Object> properties;
    private String wildcardName;
    private String wildcardRulesRootPath;
    private MethodFilter methodFilter;
    private Extension extension;

    public MethodFilter getMethodFilter() {
        return methodFilter;
    }

    public void setMethodFilter(MethodFilter methodFilter) {
        this.methodFilter = methodFilter;
    }

    public String getWildcardRulesRootPath() {
        return wildcardRulesRootPath;
    }

    public void setWildcardRulesRootPath(String wildcardRulesRootPath) {
        this.wildcardRulesRootPath = wildcardRulesRootPath;
    }

    public String getWildcardName() {
        return wildcardName;
    }

    public void setWildcardName(String wildcardName) {
        this.wildcardName = wildcardName;
    }

    public ProjectDescriptor getProject() {
        return project;
    }

    public void setProject(ProjectDescriptor project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PathEntry getRulesRootPath() {
        return rulesRootPath;
    }

    public void setRulesRootPath(PathEntry rulesRootPath) {
        this.rulesRootPath = rulesRootPath;
    }

    public Extension getExtension() {
        return extension;
    }

    public void setExtension(Extension extension) {
        this.extension = extension;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return name;
    }

}
