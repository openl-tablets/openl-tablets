package org.openl.rules.webstudio.web.repository;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.openl.rules.webstudio.web.repository.tree.TreeNode;

/**
 * Returns Tree Node shown in the current request. If for updateNodeToView() isn't invoked for the request, returns null. It's
 * needed to disable long history rendering when it is not shown to a user.
 */
@ManagedBean
@ViewScoped
public class NodeVersionsBean {
    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @ManagedProperty(value = "#{repositoryTreeController}")
    private RepositoryTreeController repositoryTreeController;

    private TreeNode nodeToView;

    public TreeNode getNodeToView() {
        TreeNode selectedNode = repositoryTreeState.getSelectedNode();
        if (selectedNode != nodeToView) {
            nodeToView = null;
        }
        return nodeToView;
    }

    public void updateNodeToView() {
        this.nodeToView = repositoryTreeState.getSelectedNode();
        repositoryTreeController.setVersion(null);
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setRepositoryTreeController(RepositoryTreeController repositoryTreeController) {
        this.repositoryTreeController = repositoryTreeController;
    }
}
