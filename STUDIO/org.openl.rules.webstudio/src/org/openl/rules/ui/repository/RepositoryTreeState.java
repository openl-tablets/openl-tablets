package org.openl.rules.ui.repository;

import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeRepository;


/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
public class RepositoryTreeState {
    /** Root node for RichFaces's tree.  It is not displayed. */
    private TreeRepository root;
    private AbstractTreeNode currentNode;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;

    public TreeRepository getRoot() {
        return root;
    }

    public void setRoot(TreeRepository root) {
        this.root = root;
    }

    public AbstractTreeNode getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(AbstractTreeNode currentNode) {
        this.currentNode = currentNode;
    }

    public TreeRepository getRulesRepository() {
        return rulesRepository;
    }

    public void setRulesRepository(TreeRepository repository) {
        this.rulesRepository = repository;
    }

    public TreeRepository getDeploymentRepository() {
        return deploymentRepository;
    }

    public void setDeploymentRepository(TreeRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }
}
