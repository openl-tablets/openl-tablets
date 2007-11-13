package org.openl.rules.workspace.abstracts;

import java.io.Serializable;

public interface ProjectDependency extends Serializable {
    String getProjectName();

    boolean hasUpperLimit();

    ProjectVersion getLowerLimit();
    ProjectVersion getUpperLimit();
}
