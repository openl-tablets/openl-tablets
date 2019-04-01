package org.openl.rules.diff.tree;

import java.util.List;

/**
 * Node of Diff Tree.
 * <p>
 * Diff Tree can be described as union of comparing data. The tree has node if corresponding element exists at least in
 * one data.
 * <p>
 * Each node of Diff Tree has exactly the same number of elements. The number of elements equals number of comparing
 * data. It can be 2..N.
 * <p>
 * Each element represents an artifact in comparing data under corresponding index.
 *
 * @author Aleh Bykhavets
 *
 */
public interface DiffTreeNode {
    /**
     * Direct children or direct sub-nodes of this DiffTreeNode.
     * <p>
     * If there is no direct children empty (zero length) array is returned.
     *
     * @return direct children of the node or empty array
     */
    List<DiffTreeNode> getChildren();

    /**
     * Comparing elements at some location.
     * <p>
     * All nodes in Diff Tree have exactly the same number of elements. It is guaranteed that return will have at least
     * 2 elements.
     *
     * @return comparing elements
     */
    DiffElement[] getElements();

    DiffElement getElement(int index);
}
