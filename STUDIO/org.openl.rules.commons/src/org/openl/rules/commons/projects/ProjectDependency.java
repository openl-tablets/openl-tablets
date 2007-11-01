package org.openl.rules.commons.projects;

public interface ProjectDependency {
    String getProjectName();

    boolean hasUpperLimit();

    ProjectVersion getLowerLimit();
    ProjectVersion getUpperLimit();
}
