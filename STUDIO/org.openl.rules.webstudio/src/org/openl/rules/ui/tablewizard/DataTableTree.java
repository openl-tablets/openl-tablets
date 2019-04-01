package org.openl.rules.ui.tablewizard;

import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;

/**
 * Tree class containing object's fields for the Data Table Wizard.
 *
 * @author NSamatov
 *
 */
public class DataTableTree {
    private TreeNode rootNode;
    private DataTableTreeNode root;
    private UITree currentTreeNode;

    /**
     * Root node for UI to build a tree. Will be initialized when {@link #setRoot(DataTableTreeNode)} is invoked
     *
     * @return root node
     */
    public TreeNode getRootNode() {
        return rootNode;
    }

    /**
     * Get business root containing a type description for the data table.
     *
     * @return Business root
     */
    public DataTableTreeNode getRoot() {
        return root;
    }

    /**
     * Set business root containing a type description for the data table.
     *
     * @param root business root
     */
    public void setRoot(DataTableTreeNode root) {
        this.root = root;
        if (root != null) {
            root.useAggregatedFields();
        }
        currentTreeNode = null;

        this.rootNode = root;
        // TreeNodeImpl r = new TreeNodeImpl();
        // r.addChild(root.getValue().getName(), root);
        // this.rootNode = r;
    }

    /**
     * Get current selected tree node
     *
     * @return current selected tree node
     */
    public UITree getCurrentTreeNode() {
        return currentTreeNode;
    }

    /**
     * Set current selected tree node
     *
     * @param currentTreeNode current selected tree node
     */
    public void setCurrentTreeNode(UITree currentTreeNode) {
        this.currentTreeNode = currentTreeNode;
    }

    /**
     * Get a data for a current selected tree node
     *
     * @return data for a current selected tree node
     */
    public DataTableTreeNode getCurrentNode() {
        return (DataTableTreeNode) getCurrentTreeNode().getRowData();
    }
}
