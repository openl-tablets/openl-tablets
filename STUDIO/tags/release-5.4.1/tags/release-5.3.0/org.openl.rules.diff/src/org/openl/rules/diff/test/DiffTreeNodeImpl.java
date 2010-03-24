package org.openl.rules.diff.test;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.tree.DiffElement;

public class DiffTreeNodeImpl implements DiffTreeNode {
    private String id;
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

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
