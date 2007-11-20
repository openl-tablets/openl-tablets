package org.openl.rules.ui.repository;

import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeFile;
import org.openl.rules.ui.repository.tree.TreeFolder;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.ui.repository.tree.TreeRepository;
import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

import org.openl.util.Log;

import org.richfaces.component.UITree;

import org.richfaces.event.NodeSelectedEvent;

import java.util.Collection;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;


/**
 * Repository tree controller. Used for retrieving data for repository tree and
 * perform repository actions.
 *
 * @author Aleh Bykhavets
 * @author Andrey Naumenko
 */
public class RepositoryTreeController {
    /** Root node for RichFaces's tree.  It is not displayed. */
    private TreeRepository root;
    private AbstractTreeNode currentNode;
    private TreeRepository repository;
    private UserWorkspace userWorkspace;

    // Add new Project
    private String newProjectName;
    private String newFolderName;

    /**
     * TODO: re-implement properly when AbstractTreeNode.id becomes Object.
     *
     * @param nodeName
     *
     * @return
     */
    private static long generateId(String nodeName) {
        return nodeName.hashCode();
    }

    private void traverseFolder(TreeFolder folder,
        Collection<?extends ProjectArtefact> artefacts) {
        for (ProjectArtefact artefact : artefacts) {
            String path = artefact.getArtefactPath().getStringValue();
            if (artefact instanceof ProjectFolder) {
                TreeFolder tf = new TreeFolder(generateId(path), artefact.getName());
                tf.setDataBean(artefact);
                folder.add(tf);
                traverseFolder(tf, ((ProjectFolder) artefact).getArtefacts());
            } else {
                TreeFile tf = new TreeFile(generateId(path), artefact.getName());
                tf.setDataBean(artefact);
                folder.add(tf);
            }
        }
    }

    private void buildTree() {
        root = new TreeRepository(generateId(""), "");
        repository = new TreeRepository(generateId("Rules Repository"), "Rules Repository");
        repository.setDataBean(null);
        root.add(repository);

        for (Project project : userWorkspace.getProjects()) {
            TreeProject prj = new TreeProject(generateId(project.getName()),
                    project.getName());
            prj.setDataBean(project);
            repository.add(prj);
            // redo that
            traverseFolder(prj, project.getArtefacts());
        }
    }

    public synchronized Object getData() {
        if (root == null) {
            buildTree();
        }

        return root;
    }

    public synchronized TreeRepository getRepositoryNode() {
        if (root == null) {
            buildTree();
        }
        return repository;
    }

    public AbstractTreeNode getSelected() {
        if (currentNode == null) {
            // lazy loading
            getData();
            //
            currentNode = repository;
        }
        return currentNode;
    }

    public void processSelection(NodeSelectedEvent event) throws AbortProcessingException {
        UITree tree = (UITree) event.getComponent();
        AbstractTreeNode node = (AbstractTreeNode) tree.getRowData();
        currentNode = node;
    }

    public Boolean adviseNodeSelected(UITree uiTree) {
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getRowData();
        AbstractTreeNode selected = getSelected();
        return (node.getId() == selected.getId());
    }

    public void reInit() {
        root = null;

//        currentNode = null;
    }

    public static void refreshSessionTree() {
        RepositoryTreeController self = (RepositoryTreeController) FacesUtils
                .getFacesVariable("#{repositoryTree}");
        if (self != null) {
            self.reInit();
        }
    }

    /**
     * Gets all projects from a rule repository.
     *
     * @return list of projects
     */
    public List<AbstractTreeNode> getProjects() {
        return getRepositoryNode().getChildNodes();
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
        ProjectArtefact projectArtefact = getSelected().getDataBean();
        boolean result = false;
        if (projectArtefact instanceof UserWorkspaceProjectFolder) {
            UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) projectArtefact;
            try {
                folder.addFolder(newFolderName);
                reInit();
                result = true;
            } catch (ProjectException e) {
                Log.error("Failed to add new folder {0}", e, newFolderName);
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage("error adding folder", e.getMessage()));
            }
        }
        return result ? null : UiConst.OUTCOME_FAILURE;
    }

    public String delete() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) getSelected()
                .getDataBean();
        try {
            projectArtefact.delete();
            reInit();
        } catch (ProjectException e) {
            Log.error("error deleting", e);
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("error deleting", e.getMessage()));
        }
        return null;
    }

    public String deleteProject() {
        String projectName = FacesUtils.getRequestParameter("projectName");
        
        try {
            UserWorkspaceProject project = userWorkspace.getProject(projectName);
            project.delete();
            reInit();
        } catch (ProjectException e) {
            Log.error("Cannot delete project {0}", e, projectName);
            FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage("Failed to delete project", e.getMessage()));
        }
        return null;
    }
    
    public String addProject() {
        boolean result = false;

        try {
            userWorkspace.createProject(newProjectName);
            reInit();
            result = true;
        } catch (ProjectException e) {
            Log.error("Failed to create new project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Failed to create new project", e.getMessage()));
        }
        return (result) ? null : UiConst.OUTCOME_FAILURE;
    }

    // TODO implement
    public boolean copyProject(String existingProject, String newProject) {
        boolean result = true;

        return result;
    }

    public String openProject() {
        UserWorkspaceProject project = getActiveProject();
        if (project == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        try {
            project.open();
            reInit();
            return null;
        } catch (ProjectException e) {
            Log.error("Failed to open project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Failed to open project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }
    }

    public String closeProject() {
        UserWorkspaceProject project = getActiveProject();
        if (project == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        try {
            project.close();
            reInit();
            return null;
        } catch (ProjectException e) {
            Log.error("Failed to close project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Failed to close project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }
    }

    public String checkOutProject() {
        UserWorkspaceProject project = getActiveProject();
        if (project == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        try {
            project.checkOut();
            reInit();
            return null;
        } catch (ProjectException e) {
            Log.error("Failed to check out project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Failed to check out project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }
    }

    public String checkInProject() {
        UserWorkspaceProject project = getActiveProject();
        if (project == null) {
            return UiConst.OUTCOME_FAILURE;
        }

        try {
            project.checkIn();
            reInit();
            return null;
        } catch (ProjectException e) {
            Log.error("Failed to check in project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Failed to check in project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }
    }

    private UserWorkspaceProject getActiveProject() {
        ProjectArtefact projectArtefact = getSelected().getDataBean();
        if (projectArtefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) projectArtefact;
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Active tree element is not a project!", null));

            return null;
        }
    }

    /**
     * Used for testing DDP. Must be removed in the future.
     *
     * @return
     *
     * @deprecated
     */
    public String getSecondProjectName() {
        int c = 0;
        for (Project project : userWorkspace.getProjects()) {
            if (c > 0) {
                return project.getName();
            }
            c++;
        }
        return null;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }
}
