package org.openl.rules.ui.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeFile;
import org.openl.rules.ui.repository.tree.TreeFolder;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.ui.repository.tree.TreeRepository;
import org.openl.rules.webstudio.RulesUserSession;
import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.webstudio.services.upload.UploadService;
import org.openl.rules.webstudio.services.upload.UploadServiceParams;
import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

import org.richfaces.component.UITree;

import org.richfaces.event.NodeSelectedEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;


/**
 * Repository tree controller. Used for retrieving data for repository tree and
 * performing repository actions.
 *
 * @author Aleh Bykhavets
 * @author Andrey Naumenko
 */
public class RepositoryTreeController {
    private final static Log log = LogFactory.getLog(RepositoryTreeController.class);
    private RepositoryTreeState repositoryTreeState;
    private UserWorkspace userWorkspace;
    private UploadService uploadService;
    private String projectName;
    private String folderName;
    private UploadedFile file;
    private String fileName;
    private String uploadFrom;
    private Collection<UserWorkspaceProject> projects;

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
        repositoryTreeState.setRoot(new TreeRepository(generateId(""), ""));
        repositoryTreeState.setRepository(new TreeRepository(generateId(
                    "Rules Repository"), "Rules Repository"));
        repositoryTreeState.getRepository().setDataBean(null);
        repositoryTreeState.getRoot().add(repositoryTreeState.getRepository());

        if (projects == null) {
            projects = userWorkspace.getProjects();
        }

        for (Project project : projects) {
            TreeProject prj = new TreeProject(generateId(project.getName()),
                    project.getName());
            prj.setDataBean(project);
            repositoryTreeState.getRepository().add(prj);
            // redo that
            traverseFolder(prj, project.getArtefacts());
        }
    }

    public synchronized Object getData() {
        if (repositoryTreeState.getRoot() == null) {
            buildTree();
        }

        return repositoryTreeState.getRoot();
    }

    public synchronized TreeRepository getRepositoryNode() {
        if (repositoryTreeState.getRoot() == null) {
            buildTree();
        }
        return repositoryTreeState.getRepository();
    }

    public AbstractTreeNode getSelected() {
        if (repositoryTreeState.getCurrentNode() == null) {
            // lazy loading
            getData();
            //
            repositoryTreeState.setCurrentNode(repositoryTreeState.getRepository());
        }
        return repositoryTreeState.getCurrentNode();
    }

    public void processSelection(NodeSelectedEvent event) {
        UITree tree = (UITree) event.getComponent();
        AbstractTreeNode node = (AbstractTreeNode) tree.getRowData();
        repositoryTreeState.setCurrentNode(node);
    }

    public Boolean adviseNodeSelected(UITree uiTree) {
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getRowData();
        AbstractTreeNode selected = getSelected();
        return (node.getId() == selected.getId());
    }

    public void invalidateTree() {
        repositoryTreeState.setRoot(null);

//        currentNode = null;
    }

    /**
     * Gets all projects from a rule repository.
     *
     * @return list of projects
     */
    public List<AbstractTreeNode> getProjects() {
        return getRepositoryNode().getChildNodes();
    }

    public String addFolder() {
        ProjectArtefact projectArtefact = getSelected().getDataBean();
        boolean result = false;
        if (projectArtefact instanceof UserWorkspaceProjectFolder) {
            UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) projectArtefact;
            try {
                folder.addFolder(folderName);
                invalidateTree();
                result = true;
            } catch (ProjectException e) {
                log.error("Failed to add new folder " + folderName, e);
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
            invalidateTree();
        } catch (ProjectException e) {
            log.error("error deleting", e);
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
            invalidateTree();
        } catch (ProjectException e) {
            log.error("Cannot delete project " + projectName, e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Failed to delete project", e.getMessage()));
        }
        return null;
    }

    public String undeleteProject() {
        ProjectArtefact projectArtefact = getSelected().getDataBean();
        if (projectArtefact instanceof UserWorkspaceProject) {
            UserWorkspaceProject project = (UserWorkspaceProject) projectArtefact;
            if (!project.isDeleted()) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage("Can not undelete project " + project.getName(),
                            "project is not deleted"));
                return null;
            }

            try {
                project.undelete();
                invalidateTree();
            } catch (ProjectException e) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage("Can not undelete project " + project.getName(),
                            e.getMessage()));
            }
        }

        return null;
    }

    public String eraseProject() {
        ProjectArtefact projectArtefact = getSelected().getDataBean();
        if (projectArtefact instanceof UserWorkspaceProject) {
            UserWorkspaceProject project = (UserWorkspaceProject) projectArtefact;
            if (!project.isDeleted()) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage("Can not erase project " + project.getName(),
                            "project is not deleted"));
                return null;
            }

            try {
                project.erase();
                invalidateTree();
                repositoryTreeState.setCurrentNode(null);
            } catch (ProjectException e) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage("Can not erase project " + project.getName(),
                            e.getMessage()));
            }
        }

        return null;
    }

    public String createProject() {
        try {
            userWorkspace.createProject(projectName);
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to create new project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage("Failed to create new project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }
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
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to open project", e);

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
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to close project", e);

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
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to check out project", e);

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
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to check in project", e);

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

    public String upload() {
        if (runUpload()) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("Project was successfully uploaded"));
            invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("Error occured during uploading file"));
        }
        return null;
    }

    private boolean runUpload() {
        UploadServiceParams params = new UploadServiceParams();
        params.setFile(file);
        params.setProjectName(projectName);

        RulesUserSession rulesUserSession = (RulesUserSession) FacesUtils.getSessionMap()
                .get("rulesUserSession");

        try {
            UserWorkspace workspace = rulesUserSession.getUserWorkspace();
            params.setWorkspace(workspace);
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
            return false;
        }

        try {
            //UploadServiceResult result = (UploadServiceResult)
            uploadService.execute(params);

            //importFile = result.getResultFile().getName();
        } catch (ServiceException e) {
            log.error("Error while uploading project", e);
            return false;
        }

        return true;
    }

    /**
     * Adds new file to active node (Project or Folder)
     *
     * @return outcome: "success" or "fail"
     */
    public String addFile() {
        return null;
    }

    /**
     * Updates file (active node)
     *
     * @return
     */
    public String updateFile() {
        return null;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        /*Object dataBean = FacesUtils.getFacesVariable(
           "#{repositoryTreeController.selected.dataBean}");*/
        return properties;
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
        if (projects == null) {
            projects = userWorkspace.getProjects();
        }
        for (Project project : projects) {
            if (c > 0) {
                return project.getName();
            }
            c++;
        }
        return null;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String newProjectName) {
        this.projectName = newProjectName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String newFolderName) {
        this.folderName = newFolderName;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String getUploadFrom() {
        return uploadFrom;
    }

    public void setUploadFrom(String uploadFrom) {
        this.uploadFrom = uploadFrom;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setUploadService(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }
}
