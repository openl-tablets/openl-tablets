package org.openl.rules.diff.tree;

import org.openl.rules.diff.hierarchy.ProjectionProperty;

public interface DiffProperty {

    DiffStatus getDiffStatus();
    ProjectionProperty getProjectionProperty();
}
