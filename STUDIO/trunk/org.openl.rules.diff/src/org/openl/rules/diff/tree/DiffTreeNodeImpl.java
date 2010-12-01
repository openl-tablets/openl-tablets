package org.openl.rules.diff.tree;


public class DiffTreeNodeImpl implements DiffTreeNode {
    private DiffTreeNode[] children;
    private DiffElement[] elements;

    public DiffTreeNode[] getChildren() {
        return children;
    }

    public DiffElement[] getElements() {
        return elements;
    }

    public void setChildren(DiffTreeNode[] children) {
        this.children = children;
    }

    public void setElements(DiffElement[] elements) {
        this.elements = elements;
    }

    public DiffElementImpl getElement(int idx) {
        return (DiffElementImpl) elements[idx];
    }
}
