package org.openl.rules.workspace.dtr.impl;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectInfo {
    private String name;
    private String path;
    private Date modifiedAt;

    public ProjectInfo() {
    }

    public ProjectInfo(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (path != null && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        this.path = path;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public ProjectInfo copy() {
        ProjectInfo info = new ProjectInfo(getName(), getPath());
        info.setModifiedAt(getModifiedAt());
        return info;
    }
}
