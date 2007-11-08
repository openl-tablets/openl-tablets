package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.RepositoryTreeController;
import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;
import org.openl.util.Log;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
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

    // Add new Project
    private String newProjectName;

    private String newFolderName;

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

    public String getNewFolderName() {
        return newFolderName;
    }

    public void setNewFolderName(String newFolderName) {
        this.newFolderName = newFolderName;
    }

    public String addFolder() {
        ProjectArtefact projectArtefact = repositoryTree.getSelected().getDataBean();
        boolean result = false;
        if (projectArtefact instanceof UserWorkspaceProjectFolder) {
            UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) projectArtefact;
            try {
                folder.addFolder(newFolderName);
                repositoryTree.reInit();
                result = true;
            } catch (ProjectException e) {
                Log.error("Failed to add new folder {0}", e, newFolderName);
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("error adding folder", e.getMessage()));
            }
        }
        return result ? UiConst.OUTCOME_SUCCESS : UiConst.OUTCOME_FAILED;
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
