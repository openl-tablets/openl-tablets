package org.openl.rules.workspace.dtr.impl;

import java.util.ArrayList;
import java.util.List;

public class ProjectIndex {
    private List<ProjectInfo> projects = new ArrayList<>();

    public List<ProjectInfo> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectInfo> projects) {
        this.projects = projects == null ? new ArrayList<>() : projects;
    }
}
