package org.openl.rules.workspace.dtr.impl;

import java.util.Date;

public class ProjectInfo {
    private String name;
    private String path;
    private Date modifiedAt;
    private boolean archived;
    private boolean duplicated = false;

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

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isDuplicated() {
        return duplicated;
    }

    public void setDuplicated(boolean duplicated) {
        this.duplicated = duplicated;
    }

    public ProjectInfo copy() {
        ProjectInfo info = new ProjectInfo(getName(), getPath());
        info.setModifiedAt(getModifiedAt());
        info.setArchived(isArchived());
        info.setDuplicated(isDuplicated());
        return info;
    }
}
