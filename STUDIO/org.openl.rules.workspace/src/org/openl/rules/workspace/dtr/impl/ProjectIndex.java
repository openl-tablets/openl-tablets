package org.openl.rules.workspace.dtr.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectIndex {
    private List<ProjectInfo> projects = new ArrayList<>();

    public List<ProjectInfo> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectInfo> projects) {
        this.projects = projects == null ? new ArrayList<>() : projects;
    }

    public ProjectIndex copy() {
        var index = new ProjectIndex();
        var projectsCopy = projects.stream().map(ProjectInfo::copy).collect(Collectors.toCollection(ArrayList::new));
        index.setProjects(projectsCopy);
        return index;
    }
}
