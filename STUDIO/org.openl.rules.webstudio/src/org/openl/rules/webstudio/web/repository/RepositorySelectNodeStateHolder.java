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
    private final SelectionHolder selectionHolder = new SelectionHolder();

    public TreeNode getSelectedNode() {
        return selectionHolder.getSelectedNode();
    }

    public void setSelectedNode(TreeNode selectedNode) {
        selectionHolder.setSelectedNode(selectedNode);
    }

    public boolean isProductionRepository() {
        TreeNode selectedNode = selectionHolder.getSelectedNode();
        return selectedNode != null && selectedNode.getType().startsWith("prod");
    }

    SelectionHolder getSelectionHolder() {
        return selectionHolder;
    }

    static class SelectionHolder {
        private TreeNode selectedNode;

        public TreeNode getSelectedNode() {
            return selectedNode;
        }

        public void setSelectedNode(TreeNode selectedNode) {
            this.selectedNode = selectedNode;
        }
    }
}
