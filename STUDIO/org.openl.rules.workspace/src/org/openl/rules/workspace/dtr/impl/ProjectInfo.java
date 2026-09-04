package org.openl.rules.workspace.dtr.impl;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectInfo {
    @Getter
    @Setter
    private String name;
    @Getter
    private String path;
    @Getter
    @Setter
    private Date modifiedAt;

    public ProjectInfo() {
    }

    public ProjectInfo(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public void setPath(String path) {
        if (path != null && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        this.path = path;
    }

    public ProjectInfo copy() {
        var copy = new ProjectInfo(getName(), getPath());
        copy.setModifiedAt(getModifiedAt());
        return copy;
    }
}
