package org.openl.rules.project.model;

public class ProjectDependencyDescriptor {
    private String name;
    private boolean autoIncluded;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoIncluded() {
        return autoIncluded;
    }

    public void setAutoIncluded(boolean autoIncluded) {
        this.autoIncluded = autoIncluded;
    }
}
