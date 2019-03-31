package org.openl.rules.diff.tree;

import org.openl.rules.diff.hierarchy.Projection;

public interface DiffTreeBuilder {
    DiffTreeNode compare(Projection p1, Projection p2);

    DiffTreeNode compare(Projection[] projections);
}
