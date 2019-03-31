package org.openl.rules.webstudio.web.diff;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.tree.DiffStatus;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.webstudio.web.repository.UiConst;
import org.richfaces.model.TreeNodeImpl;

public class TreeNode extends TreeNodeImpl {

    private DiffTreeNode diffTreeNode;

    public TreeNode(DiffTreeNode node) {
        this(node, false);
    }

    public TreeNode(DiffTreeNode node, boolean leaf) {
        super(leaf);
        this.diffTreeNode = node;
    }

    public DiffTreeNode getDiffTreeNode() {
        return diffTreeNode;
    }

    public DiffStatus getStatus() {
        // [1] status is what matters
        // [0] is either here or not
        return diffTreeNode.getElement(1).getDiffStatus();
    }

    public String getIcon() {
        String icon = null;

        switch (diffTreeNode.getElement(1).getDiffStatus()) {
            case ADDED:
                icon = UiConst.ICON_DIFF_ADDED;
                break;
            case REMOVED:
                icon = UiConst.ICON_DIFF_REMOVED;
                break;
            case DIFFERS:
                icon = UiConst.ICON_DIFF_EQUALS;
                break;
            default:
                icon = UiConst.ICON_DIFF_DIFFERS;
                break;
        }

        return icon;
    }

    /**
     * Get name old name preferably or new if there is no old one.
     * 
     * @return old or new name
     */
    public String getName() {
        return getFirstAvailable().getName();
    }

    /**
     * Get type of projection. It must be the same for all projections within a DiffTreeNode.
     * 
     * @return type of projection
     */
    public String getType() {
        return getFirstAvailable().getType();
    }

    /**
     * Get old projection preferably or new if there is no old one.
     * 
     * @return old or new projection
     */
    private Projection getFirstAvailable() {
        DiffElement[] elements = diffTreeNode.getElements();
        if (elements[1].getDiffStatus() == DiffStatus.ADDED) {
            // if [1] was added then [0] is absent
            return elements[1].getProjection();
        } else {
            // [0] should be there
            return elements[0].getProjection();
        }
    }
}
