package org.openl.rules.diff.test;

import org.openl.rules.diff.tree.DiffProperty;
import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.hierarchy.ProjectionProperty;

import static org.openl.rules.diff.tree.DiffStatus.*;

/**@deprecated DELETE */
public class DiffPropertyImpl implements DiffProperty {
    private ProjectionProperty projectionProperty;
    private DiffStatus diffStatus;

    public DiffPropertyImpl(ProjectionProperty projectionProperty) {
        this.projectionProperty = projectionProperty;
    }

    public DiffPropertyImpl(ProjectionProperty projectionProperty, DiffStatus status) {
        this.projectionProperty = projectionProperty;
        this.diffStatus = status;
    }

    public ProjectionProperty getProjectionProperty() {
        return projectionProperty;
    }

    public DiffStatus getDiffStatus() {
        return diffStatus;
    }

    public void asAdded() {
        diffStatus = ADDED;
    }

    public void asRemoved() {
        diffStatus = REMOVED;
    }

    public void asEquals() {
        diffStatus = EQUALS;
    }

    public void asDiffers() {
        diffStatus = DIFFERS;
    }
}
