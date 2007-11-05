package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.RepositoryTreeController;
import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.util.Log;

import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

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

    // Add new Project
    private String newProjectName;

    /**
     * Gets all projects from a rule repository.
     * 
     * @return list of projects
     */
    public List<AbstractTreeNode> getProjects() {
        return repositoryTree.getRepositoryNode().getChildNodes();
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = newProjectName;
    }
    
    public String getNewProjectName() {
        // expect null here
        return newProjectName;
    }

    public String addProject() {
        boolean result = false;
        
        try {
            repositoryTree.getUserWorkspace().createProject(newProjectName);
            repositoryTree.reInit();
            result = true;
        } catch (ProjectException e) {
            Log.error("Failed to create new project", e);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage("Failed to create new project", e.getMessage()));
        }        
        return (result) ? UiConst.OUTCOME_SUCCESS : UiConst.OUTCOME_FAILED;
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
