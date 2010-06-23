package org.openl.rules.project.model;

import java.io.File;
import java.util.List;

public class ProjectDescriptor {

    private String id;
    private String name;
    private String comment;
    private File projectFolder;
    private List<Module> modules;
    private List<PathEntry> classpath;

    public File getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(File projectRoot) {
        this.projectFolder = projectRoot;
    }

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

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }

}
