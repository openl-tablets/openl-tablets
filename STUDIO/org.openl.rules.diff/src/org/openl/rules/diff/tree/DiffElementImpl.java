package org.openl.rules.diff.tree;

import static org.openl.rules.diff.tree.DiffStatus.ADDED;
import static org.openl.rules.diff.tree.DiffStatus.DIFFERS;
import static org.openl.rules.diff.tree.DiffStatus.EQUALS;
import static org.openl.rules.diff.tree.DiffStatus.ORIGINAL;
import static org.openl.rules.diff.tree.DiffStatus.ORIGINAL_ABSENT;
import static org.openl.rules.diff.tree.DiffStatus.REMOVED;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.diff.hierarchy.Projection;

@RequiredArgsConstructor
public class DiffElementImpl implements DiffElement {
    @Getter
    private final Projection projection;
    @Getter
    private DiffStatus diffStatus;
    @Getter
    private boolean hierarhyEqual;
    @Getter
    private boolean childrenEqual;
    @Getter
    private boolean selfEqual;

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

        diffStatus = childrenEqual && selfEqual ? EQUALS : DIFFERS;
    }

    public void asOriginal(boolean exists) {
        // self equety
        hierarhyEqual = exists;
        childrenEqual = exists;
        selfEqual = exists;

        diffStatus = exists ? ORIGINAL : ORIGINAL_ABSENT;
    }
}
