package org.openl.rules.diff.tree;

import java.util.List;


public class DiffTreeNodeImpl implements DiffTreeNode {
    private List<DiffTreeNode> children;
    private DiffElement[] elements;

    @Override
    public List<DiffTreeNode> getChildren() {
        return children;
    }

    @Override
    public DiffElement[] getElements() {
        return elements;
    }

    public void setChildren(List<DiffTreeNode> children) {
        this.children = children;
    }

    public void setElements(DiffElement[] elements) {
        this.elements = elements;
    }

    @Override
    public DiffElement getElement(int idx) {
        return elements[idx];
    }
}
