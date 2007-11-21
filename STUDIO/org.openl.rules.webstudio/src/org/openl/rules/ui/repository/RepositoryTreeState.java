package org.openl.rules.ui.repository;

import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeRepository;


/**
 * Used for holding information about repository tree.
 *
 * @author Andrey Naumenko
 */
public class RepositoryTreeState {
    /** Root node for RichFaces's tree.  It is not displayed. */
    private TreeRepository root;
    private AbstractTreeNode currentNode;
    private TreeRepository repository;

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

    public TreeRepository getRepository() {
        return repository;
    }

    public void setRepository(TreeRepository repository) {
        this.repository = repository;
    }
}
