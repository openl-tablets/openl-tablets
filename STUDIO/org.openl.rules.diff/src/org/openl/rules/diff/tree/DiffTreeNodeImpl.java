package org.openl.rules.diff.tree;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class DiffTreeNodeImpl implements DiffTreeNode {
    @Getter
    @Setter
    private List<DiffTreeNode> children;
    @Getter
    @Setter
    private DiffElement[] elements;

    @Override
    public DiffElement getElement(int idx) {
        return elements[idx];
    }
}
