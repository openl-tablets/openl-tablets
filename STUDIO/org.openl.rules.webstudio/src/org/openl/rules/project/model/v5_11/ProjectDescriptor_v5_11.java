package org.openl.rules.project.model.v5_11;

import java.util.List;

import org.openl.rules.project.model.PathEntry;

public class ProjectDescriptor_v5_11 {
    private String id;
    private String name;
    private String comment;
    private List<Module_v5_11> modules;
    private List<PathEntry> classpath;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<Module_v5_11> getModules() {
        return modules;
    }

    public void setModules(List<Module_v5_11> modules) {
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }
}
