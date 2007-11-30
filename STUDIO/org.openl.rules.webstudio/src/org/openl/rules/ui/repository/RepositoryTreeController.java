package org.openl.rules.ui.repository;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
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
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspaceProjectResource;

import org.richfaces.component.UITree;

import org.richfaces.event.NodeSelectedEvent;

import org.richfaces.model.TreeNode;

import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;


/**
 * Repository tree controller. Used for retrieving data for repository tree and
 * performing repository actions.
 *
 * @author Aleh Bykhavets
 * @author Andrey Naumenko
 */
public class RepositoryTreeController {
    private static final Date SPECIAL_DATE = new Date(0);
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
    private String version;
    private int major;
    private int minor;

    public int getMajor() {
        ProjectVersion v = getProjectVersion();
        if (v != null) {
            return v.getMajor();
        }
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        ProjectVersion v = getProjectVersion();
        if (v != null) {
            return v.getMinor();
        }
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public SelectItem[] getProjectVersions() {
        UserWorkspaceProject project = getActiveProject();
        if (project == null) {
            return new SelectItem[0];
        }

        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ProjectVersion version : project.getVersions()) {
            selectItems.add(new SelectItem(version.getVersionName()));
        }
        return selectItems.toArray(new SelectItem[selectItems.size()]);
    }

    public final void invalidateTree() {
        repositoryTreeState.setRoot(null);
    }

    public void invalidateTreeUpdateSelection() {
        invalidateTree();

        AbstractTreeNode selected = repositoryTreeState.getSelectedNode();

        Iterator<String> iterator = selected.getDataBean().getArtefactPath().getSegments()
                .iterator();
        TreeNode currentNode = repositoryTreeState.getRulesRepository();
        while ((currentNode != null) && iterator.hasNext()) {
            currentNode = currentNode.getChild(iterator.next());
        }

        if (currentNode != null) {
            repositoryTreeState.setSelectedNode((AbstractTreeNode) currentNode);
        }
    }

    /**
     * Gets all rules projects from a rule repository.
     *
     * @return list of rules projects
     */
    public List<AbstractTreeNode> getProjects() {
        return repositoryTreeState.getRulesRepository().getChildNodes();
    }

    /**
     * Gets all deployments projects from a repository.
     *
     * @return list of deployments projects
     */
    public List<AbstractTreeNode> getDeploymentProjects() {
        return repositoryTreeState.getDeploymentRepository().getChildNodes();
    }

    public String addFolder() {
        ProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getDataBean();
        String errorMessage = null;
        if (projectArtefact instanceof UserWorkspaceProjectFolder) {
            if (checkName(folderName)) {
                UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) projectArtefact;
                try {
                    folder.addFolder(folderName);
                    invalidateTreeUpdateSelection();
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
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean();
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
        AbstractTreeNode selected = repositoryTreeState.getSelectedNode();
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) selected
                .getDataBean();
        String childName = FacesUtils.getRequestParameter("element");

        try {
            projectArtefact.getArtefact(childName).delete();
            invalidateTreeUpdateSelection();
        } catch (ProjectException e) {
            log.error("error deleting", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting",
                        e.getMessage()));
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
        ProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getDataBean();
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
        ProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getDataBean();
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
                repositoryTreeState.setSelectedNode(null);
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

    public String openVersion() {
        UserWorkspaceProject project = getActiveProject();

        try {
            project.openVersion(new CommonVersionImpl(version));
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to open project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to open project", e.getMessage()));
            return null;
        }
    }

    public String openProject() {
        UserWorkspaceProject project = getActiveProject();

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
            return null;
        }
    }

    public String closeProject() {
        UserWorkspaceProject project = getActiveProject();

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
            return null;
        }
    }

    public String refresh() {
        invalidateTree();
        repositoryTreeState.setSelectedNode(null);
        return null;
    }

    public String copyProject() {
        String errorMessage = null;
        ProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getDataBean();
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

        try {
            project.checkIn(major, minor);
            invalidateTree();
            return null;
        } catch (ProjectException e) {
            log.error("Failed to check in project", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to check in project", null));
            return null;
        }
    }

    private UserWorkspaceProject getActiveProject() {
        ProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getDataBean();
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
            invalidateTreeUpdateSelection();
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

            UserWorkspaceProjectFolder node = (UserWorkspaceProjectFolder) repositoryTreeState.getSelectedNode()
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

            UserWorkspaceProjectResource node = (UserWorkspaceProjectResource) repositoryTreeState.getSelectedNode()
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

        return rulesUserSession.getUserWorkspace();
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
        return null;
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
        return null;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getVersion() {
        UserWorkspaceProject project = getActiveProject();
        if (project != null) {
            ProjectVersion version = project.getVersion();
            if (version != null) {
                version.getRevision();
                ProjectVersion newVersion = new RepositoryProjectVersionImpl(version
                            .getMajor(), version.getMinor(), version.getRevision() + 1,
                        null);

                return newVersion.getVersionName();
            }
        }
        return version;
    }

    public ProjectVersion getProjectVersion() {
        UserWorkspaceProject project = getActiveProject();
        if (project != null) {
            ProjectVersion version = project.getVersion();
            if (version != null) {
                version.getRevision();
                ProjectVersion newVersion = new RepositoryProjectVersionImpl(version
                            .getMajor(), version.getMinor(), version.getRevision() + 1,
                        null);

                return newVersion;
            }
        }
        return null;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getEffectiveDate() {
        return ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).getEffectiveDate();
    }

    public void setEffectiveDate(Date date) {
        if (!SPECIAL_DATE.equals(date)) {
            try {
                ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).setEffectiveDate(date);
            } catch (ProjectException e) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Failed to set effective date.", e.getMessage()));
                log.error(e);
            }
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Specified effective date value is not a valid date/time.", null));
        }
    }

    public Date getExpirationDate() {
        return ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).getExpirationDate();
    }

    public void setExpirationDate(Date date) {
        if (!SPECIAL_DATE.equals(date)) {
            try {
                ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).setExpirationDate(date);
            } catch (ProjectException e) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Failed to set expiration date.", e.getMessage()));
                log.error(e);
            }
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Specified expiration date value is not a valid date/time.", null));
        }
    }

    public String getLineOfBusiness() {
        return ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).getLineOfBusiness();
    }

    public void setLineOfBusiness(String lineOfBusiness) {
        try {
            ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).setLineOfBusiness(lineOfBusiness);
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to set line of business.", e.getMessage()));
            log.error(e);
        }
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
