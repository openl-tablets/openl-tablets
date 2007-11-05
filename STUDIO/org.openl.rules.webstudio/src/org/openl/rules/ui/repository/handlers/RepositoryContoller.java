package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.RepositoryTreeController;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;

import java.util.List;

/**
 * Repository Handler.
 * It works with repository projects.
 * 
 * @author Aleh Bykhavets
 *
 */
public class RepositoryContoller {
    /**
     * A controller which contains pre-built UI object tree.
     */
    private RepositoryTreeController repositoryTree;

    /**
     * Gets all projects from a rule repository.
     * 
     * @return list of projects
     */
    public List<AbstractTreeNode> getProjects() {
        return repositoryTree.getRepositoryNode().getChildNodes();
    }

    // todo: implement
    public boolean addProject(String newProjectName) {
        boolean result = true;
        

        return result;
    }

    // todo: implement
    public boolean copyProject(String existingProject, String newProject) {
        boolean result = true;
        

        return result;
    }

    /**
     * Sets <code>RepositoryTreeController</code> to be used by this controller.
     *
     * @param treeController <code>RepositoryTreeController</code> instance
     */
    public void setRepositoryTree(RepositoryTreeController treeController) {
        repositoryTree = treeController;
    }
}
