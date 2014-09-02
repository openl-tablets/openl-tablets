package org.openl.rules.webstudio.web.repository;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.rules.webstudio.web.repository.tree.TreeNode;

/**
 * Used for holding information about repository selected node.
 *
 * @author Pavel Tarasevich
 */
@ManagedBean
@SessionScoped
public class RepositorySelectNodeStateHolder {
    private int tab = 0;
    private TreeNode selectedNode;

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public void setTab(int tab) {
        this.tab = tab;
    }

    public int getTab() {
        return tab;
    }

    public boolean isProductionNode() {
        return selectedNode.getType().contains("prod");
    }
}
