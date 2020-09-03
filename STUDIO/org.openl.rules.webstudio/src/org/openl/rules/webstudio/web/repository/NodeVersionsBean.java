package org.openl.rules.webstudio.web.repository;

import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.springframework.stereotype.Service;

/**
 * Returns Tree Node shown in the current request. If for updateNodeToView() isn't invoked for the request, returns null. It's
 * needed to disable long history rendering when it is not shown to a user.
 */
@Service
@ViewScope
public class NodeVersionsBean {
    private final RepositoryTreeState repositoryTreeState;

    private final RepositoryTreeController repositoryTreeController;

    private TreeNode nodeToView;

    public NodeVersionsBean(RepositoryTreeState repositoryTreeState,
        RepositoryTreeController repositoryTreeController) {
        this.repositoryTreeState = repositoryTreeState;
        this.repositoryTreeController = repositoryTreeController;
    }

    public TreeNode getNodeToView() {
        TreeNode selectedNode = repositoryTreeState.getSelectedNode();
        if (nodeToView == null || selectedNode == null || selectedNode != nodeToView && !selectedNode.getId()
            .equals(nodeToView.getId())) {
            nodeToView = null;
        }
        return nodeToView;
    }

    public void updateNodeToView() {
        this.nodeToView = repositoryTreeState.getSelectedNode();
        repositoryTreeController.setVersion(null);
    }
}
