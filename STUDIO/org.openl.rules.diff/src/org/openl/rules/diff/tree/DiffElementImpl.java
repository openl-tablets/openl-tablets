package org.openl.rules.diff.tree;

import static org.openl.rules.diff.tree.DiffStatus.*;

import org.openl.rules.diff.hierarchy.Projection;

public class DiffElementImpl implements DiffElement {
    private Projection projection;
    private DiffStatus diffStatus;
    private boolean hierarhyEqual;
    private boolean childrenEqual;
    private boolean selfEqual;

    public DiffElementImpl(Projection projection) {
        this.projection = projection;
    }

    @Override
    public DiffStatus getDiffStatus() {
        return diffStatus;
    }

    @Override
    public boolean isHierarhyEqual() {
        return hierarhyEqual;
    }

    @Override
    public boolean isChildrenEqual() {
        return childrenEqual;
    }

    @Override
    public boolean isSelfEqual() {
        return selfEqual;
    }

    @Override
    public Projection getProjection() {
        return projection;
    }

    public void asAdded() {
        hierarhyEqual = false;
        childrenEqual = false;
        selfEqual = false;

        diffStatus = ADDED;
    }

    public void asRemoved() {
        hierarhyEqual = false;
        childrenEqual = false;
        selfEqual = false;

        diffStatus = REMOVED;
    }

    public void asExists(boolean hierarhyEqual, boolean childrenEqual, boolean selfEqual) {
        this.hierarhyEqual = hierarhyEqual;
        this.childrenEqual = childrenEqual;
        this.selfEqual = selfEqual;

        if (childrenEqual && !hierarhyEqual) {
            throw new IllegalArgumentException("childrenEqual is 'true' while less strict hierarhyEqual is 'false'.");
        }

        diffStatus = (childrenEqual && selfEqual) ? EQUALS : DIFFERS;
    }

    public void asOriginal(boolean exists) {
        // self equety
        hierarhyEqual = exists;
        childrenEqual = exists;
        selfEqual = exists;

        diffStatus = (exists) ? ORIGINAL : ORIGINAL_ABSENT;
    }
}
