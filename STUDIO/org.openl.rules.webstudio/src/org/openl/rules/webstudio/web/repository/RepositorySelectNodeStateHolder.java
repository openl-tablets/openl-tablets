package org.openl.rules.webstudio.web.repository;

import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Used for holding information about repository selected node.
 *
 * @author Pavel Tarasevich
 */
@Controller
@SessionScope
public class RepositorySelectNodeStateHolder {
    private TreeNode selectedNode;

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public boolean isProductionRepository() {
        return selectedNode != null && selectedNode.getType().startsWith("prod");
    }
}
