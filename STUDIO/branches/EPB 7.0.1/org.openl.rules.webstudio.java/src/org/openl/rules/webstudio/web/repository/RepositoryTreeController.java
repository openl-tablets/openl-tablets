package org.openl.rules.webstudio.web.repository;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.webstudio.services.ServiceException;
import org.openl.rules.webstudio.services.upload.FileProjectResource;
import org.openl.rules.webstudio.services.upload.UploadService;
import org.openl.rules.webstudio.services.upload.UploadServiceParams;
import org.openl.rules.webstudio.services.upload.UploadServiceResult;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspaceProjectResource;
import org.openl.rules.workspace.uw.impl.UserWorkspaceProjectImpl;

import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private RepositoryTreeState repositoryTreeState;
    private UserWorkspace userWorkspace;
    private UploadService uploadService;
    private String projectName;
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

    private ProjectVersion getProjectVersion() {
        UserWorkspaceProject project = getSelectedProject();
        if (project != null) {
            return project.getVersion();
        }
        return null;
    }

    private UserWorkspaceProject getSelectedProject() {
        ProjectArtefact artefact = repositoryTreeState.getSelectedNode().getDataBean();
        if (artefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact;
        }
        return null;
    }

    public SelectItem[] getSelectedProjectVersions() {
        Collection<ProjectVersion> versions = repositoryTreeState.getSelectedNode()
                .getVersions();

        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ProjectVersion version : versions) {
            selectItems.add(new SelectItem(version.getVersionName()));
        }
        return selectItems.toArray(new SelectItem[selectItems.size()]);
    }

    /**
     * Gets all rules projects from a rule repository.
     *
     * @return list of rules projects
     */
    public List<AbstractTreeNode> getRulesProjects() {
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
        ProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode()
                .getDataBean();
        String errorMessage = null;
        if (projectArtefact instanceof UserWorkspaceProjectFolder) {
            if (NameChecker.checkName(folderName)) {
                UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) projectArtefact;
                try {
                    folder.addFolder(folderName);
                    repositoryTreeState.refreshSelectedNode();
                    repositoryTreeState.invalidateTree();
                } catch (ProjectException e) {
                    log.error("Failed to create folder '" + folderName + "'.", e);
                    errorMessage = e.getMessage();
                }
            } else {
                errorMessage = "Folder name '" + folderName + "' is invalid. "
                    + NameChecker.BAD_NAME_MSG;
            }
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to create folder.", errorMessage));
        }
        return null;
    }

    public String deleteNode() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean();
        try {
            projectArtefact.delete();
            repositoryTreeState.invalidateTree();
            if (projectArtefact instanceof UserWorkspaceProjectImpl) {
                UserWorkspaceProjectImpl project = (UserWorkspaceProjectImpl) projectArtefact;
                if (project.isLocalOnly()) {
                    repositoryTreeState.invalidateSelection();
                    return null;
                } else {
                    repositoryTreeState.refreshSelectedNode();
                    return null;
                }
            }
            repositoryTreeState.moveSelectionToParentNode();
        } catch (ProjectException e) {
            log.error("Failed to delete node.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to delete node.", e.getMessage()));
        }
        return null;
    }

    public String deleteElement() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean();
        String childName = FacesUtils.getRequestParameter("element");

        try {
            projectArtefact.getArtefact(childName).delete();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();
        } catch (ProjectException e) {
            log.error("Error deleting element.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting.",
                        e.getMessage()));
        }
        return null;
    }

    public String deleteRulesProject() {
        String projectName = FacesUtils.getRequestParameter("projectName");

        try {
            UserWorkspaceProject project = userWorkspace.getProject(projectName);
            project.delete();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Cannot delete rules project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to delete rules project.", e.getMessage()));
        }
        return null;
    }

    public String deleteDeploymentProject() {
        String projectName = FacesUtils.getRequestParameter("deploymentProjectName");

        try {
            UserWorkspaceDeploymentProject project = userWorkspace.getDDProject(projectName);
            project.delete();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Cannot delete deployment project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to delete deployment project.", e.getMessage()));
        }
        return null;
    }

    public String undeleteProject() {
        UserWorkspaceProject project = getSelectedProject();
        if (!project.isDeleted()) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Cannot undelete project '" + project.getName() + "'.",
                        "Project is not marked for deletion."));
            return null;
        }

        try {
            project.undelete();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Cannot undelete project '" + project.getName() + "'.",
                        e.getMessage()));
        }
        return null;
    }

    public String eraseProject() {
        UserWorkspaceProject project = getSelectedProject();
        // EPBDS-225
        if (project == null) return null;
        
        if (!project.isDeleted()) {
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
                        "Cannot erase project '" + project.getName() + "'."));
            return null;
        }

        try {
            project.erase();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();
        } catch (ProjectException e) {
            log.error("Failed to erase project.", e);
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
                        "Cannot erase project '" + project.getName() + "'."));
        }
        return null;
    }

    public String createRulesProject() {
        String errorMessage = null;
        try {
            if (NameChecker.checkName(projectName)) {
                if (userWorkspace.hasProject(projectName)) {
                    errorMessage = "Cannot create project because project with such name already exists.";
                } else {
                    userWorkspace.createProject(projectName);
                    repositoryTreeState.invalidateTree();
                }
            } else {
                errorMessage = "Specified name is not a valid project name.";
            }
        } catch (ProjectException e) {
            log.error("Error creating project.", e);
            errorMessage = e.getMessage();
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
        }
        return null;
    }

    public String createDeploymentProject() {
        try {
            userWorkspace.createDDProject(projectName);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Failed to create deployment project.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to create deployment project.", e.getMessage()));
        }
        return null;
    }

    public String openProject() {
        try {
            getSelectedProject().open();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Failed to open project.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to open project.", e.getMessage()));
        }
        return null;
    }

    public String openProjectVersion() {
        try {
            getSelectedProject().openVersion(new CommonVersionImpl(version));
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Failed to open project version.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to open project version.", e.getMessage()));
        }
        return null;
    }

    public String closeProject() {
        try {
            getSelectedProject().close();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.refreshSelectedNode();
        } catch (ProjectException e) {
            log.error("Failed to close project.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to close project.", e.getMessage()));
        }
        return null;
    }

    public String checkOutProject() {
        try {
            getSelectedProject().checkOut();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Failed to check out project.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to check out project.", e.getMessage()));
        }
        return null;
    }

    public String checkInProject() {
        try {
            getSelectedProject().checkIn(major, minor);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Failed to check in project.", e);

            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to check in project.", null));
        }
        return null;
    }

    public String copyProject() {
        String errorMessage = null;
        UserWorkspaceProject project;

        try {
            project = userWorkspace.getProject(projectName);
        } catch (ProjectException e) {
            log.error("Cannot obtain rules project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, e.getMessage()));
            return null;
        }

        if (project == null) {
            errorMessage = "No project is selected.";
        } else if (StringUtils.isBlank(newProjectName)) {
            errorMessage = "Project name is empty.";
        } else if (!NameChecker.checkName(newProjectName)) {
            errorMessage = "Project name '" + newProjectName + "' is invalid. "
                + NameChecker.BAD_NAME_MSG;
        } else if (userWorkspace.hasProject(newProjectName)) {
            boolean isLocalOnly;

            try {
                isLocalOnly = userWorkspace.getProject(newProjectName).isLocalOnly();
            } catch (ProjectException e) {
                isLocalOnly = false;
            }

            if (!isLocalOnly) {
                errorMessage = "Project '" + newProjectName + "' already exists.";
            }

            // it is possible to copy into the repository local only project (publish it)
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot copy project.",
                        errorMessage));
            return null;
        }

        try {
            userWorkspace.copyProject(project, newProjectName);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Failed to copy project.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to copy project.", e.getMessage()));
        }

        return null;
    }

    public String copyDeploymentProject() {
        String errorMessage = null;
        UserWorkspaceDeploymentProject project;

        try {
            project = userWorkspace.getDDProject(projectName);
        } catch (ProjectException e) {
            log.error("Cannot obtain deployment project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, e.getMessage()));
            return null;
        }

        if (project == null) {
            errorMessage = "No project is selected.";
        } else if (StringUtils.isBlank(newProjectName)) {
            errorMessage = "Project name is empty.";
        } else if (!NameChecker.checkName(newProjectName)) {
            errorMessage = "Project name '" + newProjectName + "' is invalid. "
                + NameChecker.BAD_NAME_MSG;
        } else if (userWorkspace.hasDDProject(newProjectName)) {
            errorMessage = "Deployment project '" + newProjectName + "' already exists.";
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Cannot copy deployment project.", errorMessage));
            return null;
        }

        try {
            userWorkspace.copyDDProject(project, newProjectName);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            log.error("Failed to copy deployment project.", e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to copy deployment project.", e.getMessage()));
        }

        return null;
    }

    public String refreshTree() {
        repositoryTreeState.invalidateTree();
        repositoryTreeState.invalidateSelection();
        return null;
    }

    public String upload() {
        String errorMessage = uploadProject();
        if (errorMessage == null) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("Project was uploaded successfully."));
            repositoryTreeState.invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
        }
        return null;
    }

    private String uploadProject() {
        UploadServiceParams params = new UploadServiceParams();
        params.setFile(file);
        params.setProjectName(projectName);
        params.setWorkspace(userWorkspace);

        if (userWorkspace.hasProject(projectName)) {
            return "Cannot create project because project with such name already exists.";
        }

        try {
            //UploadServiceResult result = (UploadServiceResult)
            uploadService.execute(params);

            //importFile = result.getResultFile().getName();
        } catch (ServiceException e) {
            log.error("Error while uploading project.", e);
            return "" + e.getMessage();
        }

        return null;
    }

    /**
     * Adds new file to active node (project or folder).
     *
     * @return
     */
    public String addFile() {
        if (StringUtils.isEmpty(fileName)) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
                        "File name must not be empty."));
            return null;
        }
        String errorMessage = uploadAndAddFile();
        if (errorMessage == null) {
            FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage("File was uploaded successfully."));
            repositoryTreeState.refreshSelectedNode();
            repositoryTreeState.invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
        }
        return null;
    }

    private String uploadAndAddFile() {
        if (!NameChecker.checkName(fileName)) {
            return "File name '" + fileName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
        }

        UploadServiceParams params = new UploadServiceParams();
        params.setFile(file);
        params.setUnpackZipFile(false);

        try {
            params.setWorkspace(userWorkspace);
        } catch (Exception e) {
            log.error("Error obtaining user workspace.", e);
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
            log.error("Error adding file to user workspace.", e);
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
                .addMessage(null, new FacesMessage("File was successfully uploaded."));
            repositoryTreeState.invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMessage,
                        "Error occured during uploading file."));
        }
        return null;
    }

    private String uploadAndUpdateFile() {
        UploadServiceParams params = new UploadServiceParams();
        params.setFile(file);
        params.setUnpackZipFile(false);

        try {
            params.setWorkspace(userWorkspace);
        } catch (Exception e) {
            log.error("Error obtaining user workspace.", e);
            return e.getMessage();
        }

        try {
            UploadServiceResult result = (UploadServiceResult) uploadService.execute(params);

            UserWorkspaceProjectResource node = (UserWorkspaceProjectResource) repositoryTreeState.getSelectedNode()
                    .getDataBean();
            node.setContent(new FileInputStream(result.getResultFile()));
            result.getResultFile().delete();
        } catch (Exception e) {
            log.error("Error updating file in user workspace.", e);
            return e.getMessage();
        }

        return null;
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

    public void setProjectName(String newProjectName) {
        this.projectName = newProjectName;
    }

    public String getDeploymentProjectName() {
        //EPBDS-92 - clear newDProject dialog every time
        return null;
    }

    public String getFolderName() {
        return null;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
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
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getEffectiveDate() {
        RulesRepositoryArtefact dataBean = ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean());
        if (dataBean != null) {
            return dataBean.getEffectiveDate();
        }
        return null;
    }

    public void setEffectiveDate(Date date) {
        if (!SPECIAL_DATE.equals(date)) {
            try {
                ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                    .getDataBean()).setEffectiveDate(date);
            } catch (ProjectException e) {
                log.error(e);
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Can not set effective date.", e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
                        "Specified effective date value is not a valid date."));
        }
    }

    public Date getExpirationDate() {
        RulesRepositoryArtefact dataBean = ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean());
        if (dataBean != null) {
            return dataBean.getExpirationDate();
        }
        return null;
    }

    public void setExpirationDate(Date date) {
        if (!SPECIAL_DATE.equals(date)) {
            try {
                ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                    .getDataBean()).setExpirationDate(date);
            } catch (ProjectException e) {
                log.error(e);
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Can not set expiration date.", e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
                        "Specified expiration date value is not a valid date."));
        }
    }

    public String getLineOfBusiness() {
        RulesRepositoryArtefact dataBean = ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean());
        if (dataBean != null) {
            return dataBean.getLineOfBusiness();
        }
        return null;
    }

    public void setLineOfBusiness(String lineOfBusiness) {
        try {
            ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean())
                .setLineOfBusiness(lineOfBusiness);
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Can not set line of business.", e.getMessage()));
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
