package org.openl.rules.project.model.v5_13;

import java.util.List;

import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;

public class ProjectDescriptor_v5_13 {
    private String name;
    private String comment;
    private List<Module_v5_13> modules;
    private List<PathEntry> classpath;

    private List<ProjectDependencyDescriptor_v5_12> dependencies;
    private String propertiesFileNamePattern;
    private String propertiesFileNameProcessor;

    public String getPropertiesFileNamePattern() {
        return propertiesFileNamePattern;
    }

    public void setPropertiesFileNamePattern(String propertiesFileNamePattern) {
        this.propertiesFileNamePattern = propertiesFileNamePattern;
    }

    public String getPropertiesFileNameProcessor() {
        return propertiesFileNameProcessor;
    }

    public void setPropertiesFileNameProcessor(String propertiesFileNameProcessor) {
        this.propertiesFileNameProcessor = propertiesFileNameProcessor;
    }

    public List<ProjectDependencyDescriptor_v5_12> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ProjectDependencyDescriptor_v5_12> dependencies) {
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Module_v5_13> getModules() {
        return modules;
    }

    public void setModules(List<Module_v5_13> modules) {
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }
}
