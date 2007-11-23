package org.openl.rules.ui.repository;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeDProject;
import org.openl.rules.ui.repository.tree.TreeFile;
import org.openl.rules.ui.repository.tree.TreeFolder;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.ui.repository.tree.TreeRepository;
import org.openl.rules.webstudio.RulesUserSession;
import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.webstudio.services.upload.FileProjectResource;
import org.openl.rules.webstudio.services.upload.UploadService;
import org.openl.rules.webstudio.services.upload.UploadServiceParams;
import org.openl.rules.webstudio.services.upload.UploadServiceResult;
import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspaceProjectResource;

import org.richfaces.component.UITree;

import org.richfaces.event.NodeSelectedEvent;

import java.io.FileInputStream;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    private static final String PROJECTNAME_FORBIDDEN_REGEXP = "[\\\\/:;<>\\?\\*\t\n$%]";
    private static final Pattern PROJECTNAME_FORBIDDEN_PATTERN = Pattern.compile(PROJECTNAME_FORBIDDEN_REGEXP);
    private RepositoryTreeState repositoryTreeState;
    private UserWorkspace userWorkspace;
    private UploadService uploadService;
    private String projectName;
    private String deploymentProjectName;
    private String folderName;
    private UploadedFile file;
    private String fileName;
    private String uploadFrom;
    private String newProjectName;
    private Collection<UserWorkspaceProject> rulesProjects;
    private Collection<UserWorkspaceDeploymentProject> deploymentsProjects;

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
        repositoryTreeState.setRoot(new TreeRepository(generateId(""), "", "root"));

        TreeRepository rulesRep = new TreeRepository(generateId("Rules Projects"),
                "Rules Projects", UiConst.TYPE_REPOSITORY);
        rulesRep.setDataBean(null);
        TreeRepository deploymentRep = new TreeRepository(generateId(
                    "Deployment Projects"), "Deployment Projects",
                UiConst.TYPE_DEPLOYMENT_REPOSITORY);
        deploymentRep.setDataBean(null);

        repositoryTreeState.setRulesRepository(rulesRep);
        repositoryTreeState.setDeploymentRepository(deploymentRep);
        repositoryTreeState.getRoot().add(rulesRep);
        repositoryTreeState.getRoot().add(deploymentRep);

        if (rulesProjects == null) {
            rulesProjects = userWorkspace.getProjects();
        }

        for (Project project : rulesProjects) {
            TreeProject prj = new TreeProject(generateId(project.getName()),
                    project.getName());
            prj.setDataBean(project);
            rulesRep.add(prj);
            // redo that
            traverseFolder(prj, project.getArtefacts());
        }

        if (deploymentsProjects == null) {
            try {
                deploymentsProjects = userWorkspace.getDDProjects();
            } catch (RepositoryException e) {
                log.error("Cannot list deployments projects", e);
                deploymentsProjects = new LinkedList<UserWorkspaceDeploymentProject>();
            }
        }

        for (UserWorkspaceDeploymentProject deplProject : deploymentsProjects) {
            String name = deplProject.getName();
            TreeDProject prj = new TreeDProject(generateId(name), name);
            prj.setDataBean(deplProject);
            deploymentRep.add(prj);

            // deployments projects haven't child nodes
        }
    }

    public synchronized Object getData() {
        if (repositoryTreeState.getRoot() == null) {
            buildTree();
        }

        return repositoryTreeState.getRoot();
    }

    public synchronized TreeRepository getRulesNode() {
        if (repositoryTreeState.getRoot() == null) {
            buildTree();
        }
        return repositoryTreeState.getRulesRepository();
    }

    public synchronized TreeRepository getDeploymentNode() {
        if (repositoryTreeState.getRoot() == null) {
            buildTree();
        }
        return repositoryTreeState.getDeploymentRepository();
    }

    public AbstractTreeNode getSelected() {
        if (repositoryTreeState.getCurrentNode() == null) {
            // lazy loading
            getData();
            //
            repositoryTreeState.setCurrentNode(repositoryTreeState.getRulesRepository());
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
     * Gets all rules projects from a rule repository.
     *
     * @return list of rules projects
     */
    public List<AbstractTreeNode> getProjects() {
        return getRulesNode().getChildNodes();
    }

    /**
     * Gets all deployments projects from a repository.
     *
     * @return list of deployments projects
     */
    public List<AbstractTreeNode> getDeploymentProjects() {
        return getDeploymentNode().getChildNodes();
    }

    public String addFolder() {
        ProjectArtefact projectArtefact = getSelected().getDataBean();
        String errorMessage = null;
        if (projectArtefact instanceof UserWorkspaceProjectFolder) {
            if (checkName(folderName)) {
                UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) projectArtefact;
                try {
                    folder.addFolder(folderName);
                    invalidateTree();
                } catch (ProjectException e) {
                    log.error("Failed to add new folder " + folderName, e);
                    errorMessage = e.getMessage();
                }
            } else {
                errorMessage = "Folder name is invalid";
            }
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error adding folder",
                        errorMessage));
            return UiConst.OUTCOME_FAILURE;
        }
        return null;
    }

    public String delete() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) getSelected().getDataBean();
        try {
            projectArtefact.delete();
            invalidateTree();
        } catch (ProjectException e) {
            log.error("error deleting", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting",
                        e.getMessage()));
        }
        return null;
    }

    public String deleteElement() {
        AbstractTreeNode selected = getSelected();
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) selected.getDataBean();
        String childName = FacesUtils.getRequestParameter("element");

        try {
            projectArtefact.getArtefact(childName).delete();
            selected.deleteChildByName(childName);
            invalidateTree();
        } catch (ProjectException e) {
            log.error("error deleting", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting", e.getMessage()));
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
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to delete project", e.getMessage()));
        }
        return null;
    }

    public String deleteDeploymentProject() {
        String projectName = FacesUtils.getRequestParameter("deploymentProjectName");

        try {
            UserWorkspaceDeploymentProject project = userWorkspace.getDDProject(projectName);
            project.delete();
            invalidateTree();
        } catch (ProjectException e) {
            log.error("Cannot delete deployment project " + projectName, e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to delete deployment project", e.getMessage()));
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
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Can not undelete project " + project.getName(),
                            "project is not deleted"));
                return null;
            }

            try {
                project.undelete();
                invalidateTree();
            } catch (ProjectException e) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Can not undelete project " + project.getName(),
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
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Can not erase project " + project.getName(),
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
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Can not erase project " + project.getName(), e.getMessage()));
            }
        }

        return null;
    }

    public String createProject() {
        String errorMessage = null;
        try {
            if (checkName(projectName)) {
                userWorkspace.createProject(projectName);
                invalidateTree();
            } else {
                errorMessage = "project name is invalid";
            }
        } catch (ProjectException e) {
            log.error("Failed to create new project", e);
            errorMessage = e.getMessage();
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to create new project", errorMessage));
            return UiConst.OUTCOME_FAILURE;
        }

        return null;
    }

    public String createDeploymentProject() {
        try {
            userWorkspace.createDDProject(deploymentProjectName);
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to create new deployment project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to create new deployment project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }
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
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to open project", e.getMessage()));
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
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to close project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }
    }

    public String copyProject() {
        String errorMessage = null;
        ProjectArtefact projectArtefact = getSelected().getDataBean();
        UserWorkspaceProject project = null;

        if (projectArtefact instanceof UserWorkspaceProject) {
            project = (UserWorkspaceProject) projectArtefact;
        } else if (projectName != null) {
            try {
                project = userWorkspace.getProject(projectName);
            } catch (ProjectException e) {}
        }

        if (project == null) {
            errorMessage = "No project is selected";
        } else if (StringUtils.isBlank(newProjectName)) {
            errorMessage = "Project name is empty";
        } else if (!checkName(newProjectName)) {
            errorMessage = "Project contains forbidden symbols";
        } else if (userWorkspace.hasProject(newProjectName)) {
            errorMessage = "Project " + newProjectName + " already exists";
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Can not copy project",
                        errorMessage));
            return UiConst.OUTCOME_FAILURE;
        }

        try {
            userWorkspace.copyProject(project, newProjectName);
            invalidateTree();
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to copy project", e.getMessage()));
            return UiConst.OUTCOME_FAILURE;
        }

        return null;
    }

    protected boolean checkName(String projectName) {
        return !PROJECTNAME_FORBIDDEN_PATTERN.matcher(projectName).find();
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
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to check out project", e.getMessage()));
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
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to check in project", e.getMessage()));
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
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Active tree element is not a project!", null));

            return null;
        }
    }

    public String upload() {
        String errorMessage = uploadProject();
        if (errorMessage == null) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("Project was successfully uploaded"));
            invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error occured during uploading file", errorMessage));
        }
        return null;
    }

    private String uploadProject() {
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
            return e.getMessage();
        }

        try {
            //UploadServiceResult result = (UploadServiceResult)
            uploadService.execute(params);

            //importFile = result.getResultFile().getName();
        } catch (ServiceException e) {
            log.error("Error while uploading project", e);
            return e.getMessage();
        }

        return null;
    }

    /**
     * Adds new file to active node (project or folder).
     *
     * @return
     */
    public String addFile() {
        String errorMessage = uploadAndAddFile();
        if (errorMessage == null) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("File was successfully uploaded"));
            invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error occured during uploading file", errorMessage));
        }
        return null;
    }

    private String uploadAndAddFile() {
        if (!checkName(fileName)) {
            return "incorrect file name";
        }

        UploadServiceParams params = new UploadServiceParams();
        params.setFile(file);
        params.setUnpackZipFile(false);

        try {
            params.setWorkspace(getUserWorkSpace());
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
            return e.getMessage();
        }

        try {
            UploadServiceResult result = (UploadServiceResult) uploadService.execute(params);

            UserWorkspaceProjectFolder node = (UserWorkspaceProjectFolder) getSelected()
                    .getDataBean();

            ProjectResource projectResource = new FileProjectResource(new FileInputStream(
                        result.getResultFile()));
            node.addResource(fileName, projectResource);

            result.getResultFile().delete();
        } catch (Exception e) {
            log.error("Error adding file to user workspace", e);
            return e.getMessage();
        }

        return null;
    }

    /**
     * Updates file (active node)
     *
     * @return
     */
    public String updateFile() {
        String errorMessage = uploadAndUpdateFile();
        if (errorMessage == null) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("File was successfully uploaded"));
            invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error occured during uploading file", errorMessage));
        }
        return null;
    }

    private String uploadAndUpdateFile() {
        UploadServiceParams params = new UploadServiceParams();
        params.setFile(file);
        params.setUnpackZipFile(false);

        try {
            params.setWorkspace(getUserWorkSpace());
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
            return e.getMessage();
        }

        try {
            UploadServiceResult result = (UploadServiceResult) uploadService.execute(params);

            UserWorkspaceProjectResource node = (UserWorkspaceProjectResource) getSelected()
                    .getDataBean();
            node.setContent(new FileInputStream(result.getResultFile()));
            result.getResultFile().delete();
        } catch (Exception e) {
            log.error("Error updating file in user workspace", e);
            return e.getMessage();
        }

        return null;
    }

    private UserWorkspace getUserWorkSpace() throws WorkspaceException, ProjectException {
        RulesUserSession rulesUserSession = (RulesUserSession) FacesUtils.getSessionMap()
                .get("rulesUserSession");

        UserWorkspace workspace = rulesUserSession.getUserWorkspace();
        return workspace;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        /*Object dataBean = FacesUtils.getFacesVariable(
           "#{repositoryTreeController.selected.dataBean}");*/
        return properties;
    }

    public String getProjectName() {
        //EPBDS-92 - clear newProject dialog every time
        //return projectName;
        return null;
    }

    public String getDeploymentProjectName() {
        //EPBDS-92 - clear newDProject dialog every time
        return null;
    }

    public void setInit(boolean init) {
//        invalidateTree();
    }

    public void setProjectName(String newProjectName) {
        this.projectName = newProjectName;
    }

    public void setDeploymentProjectName(String newDeploymentProjectName) {
        this.deploymentProjectName = newDeploymentProjectName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String newFolderName) {
        this.folderName = newFolderName;
    }

    public String getNewProjectName() {
        //EPBDS-92 - clear newDProject dialog every time
        return null;
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = newProjectName;
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
