package org.openl.rules.workspace.abstracts;

public interface ProjectDependency {
    String getProjectName();

    boolean hasUpperLimit();

    ProjectVersion getLowerLimit();
    ProjectVersion getUpperLimit();
}
