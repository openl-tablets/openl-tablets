package org.openl.rules.webstudio.web.repository;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.tree.AbstractTreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.filter.RepositoryFileExtensionFilter;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspaceProjectResource;
import org.openl.rules.workspace.uw.impl.UserWorkspaceProjectImpl;
import org.openl.util.FileTypeHelper;
import org.openl.util.filter.OpenLFilter;
import org.richfaces.model.UploadItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

/**
 * Repository tree controller. Used for retrieving data for repository tree and
 * performing repository actions.
 *
 * @author Aleh Bykhavets
 * @author Andrey Naumenko
 */
public class RepositoryTreeController {
    private static final Date SPECIAL_DATE = new Date(0);
    private static final Log LOG = LogFactory.getLog(RepositoryTreeController.class);
    private RepositoryTreeState repositoryTreeState;
    private UserWorkspace userWorkspace;
    private RepositoryArtefactPropsHolder repositoryArtefactPropsHolder;
    private String projectName;
    private String newProjectTemplate;
    private String[] projectTemplates = { "SampleTemplate.xls" };
    private String folderName;
    private List<UploadItem> uploadedFiles = new ArrayList<UploadItem>();
    private String fileName;
    private String uploadFrom;
    private String newProjectName;
    private String version;
    private int major;
    private int minor;

    private String filterString;

    private PathFilter zipFilter;

    public PathFilter getZipFilter() {
        return zipFilter;
    }

    public void setZipFilter(PathFilter zipFilter) {
        this.zipFilter = zipFilter;
    }

    /**
     * Adds new file to active node (project or folder).
     *
     * @return
     */
    public String addFile() {
        if (getLastUploadedFile() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Please select file to be uploaded."));
            return null;
        }
        if (StringUtils.isEmpty(fileName)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "File name must not be empty."));
            return null;
        }
        String errorMessage = uploadAndAddFile();
        if (errorMessage == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("File was uploaded successfully."));
            repositoryTreeState.invalidateTree();
            repositoryTreeState.refreshSelectedNode();
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
        }
        return null;
    }

    public String addFolder() {
        ProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getDataBean();
        String errorMessage = null;
        if (projectArtefact instanceof UserWorkspaceProjectFolder) {
            if (NameChecker.checkName(folderName)) {
                UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) projectArtefact;
                try {
                    folder.addFolder(folderName);
                    repositoryTreeState.invalidateTree();
                    repositoryTreeState.refreshSelectedNode();
                } catch (ProjectException e) {
                    LOG.error("Failed to create folder '" + folderName + "'.", e);
                    errorMessage = e.getMessage();
                }
            } else {
                errorMessage = "Folder name '" + folderName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
            }
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to create folder.", errorMessage));
        }
        return null;
    }

    public String checkInProject() {
        try {
            repositoryTreeState.getSelectedProject().checkIn(major, minor);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Failed to check in project.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance()
                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
        }
        return null;
    }

    public String checkOutProject() {
        try {
            repositoryTreeState.getSelectedProject().checkOut();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Failed to check out project.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }
        return null;
    }

    public String closeProject() {
        try {
            repositoryTreeState.getSelectedProject().close();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.refreshSelectedNode();
        } catch (ProjectException e) {
            String msg = "Failed to close project.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }
        return null;
    }

    public String copyDeploymentProject() {
        String errorMessage = null;
        UserWorkspaceDeploymentProject project;

        try {
            project = userWorkspace.getDDProject(projectName);
        } catch (ProjectException e) {
            LOG.error("Cannot obtain deployment project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, e.getMessage()));
            return null;
        }

        if (project == null) {
            errorMessage = "No project is selected.";
        } else if (StringUtils.isBlank(newProjectName)) {
            errorMessage = "Project name is empty.";
        } else if (!NameChecker.checkName(newProjectName)) {
            errorMessage = "Project name '" + newProjectName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
        } else if (userWorkspace.hasDDProject(newProjectName)) {
            errorMessage = "Deployment project '" + newProjectName + "' already exists.";
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot copy deployment project.", errorMessage));
            return null;
        }

        try {
            userWorkspace.copyDDProject(project, newProjectName);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Failed to copy deployment project.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }

        return null;
    }

    public String copyProject() {
        String errorMessage = null;
        UserWorkspaceProject project;

        try {
            project = userWorkspace.getProject(projectName);
        } catch (ProjectException e) {
            LOG.error("Cannot obtain rules project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, e.getMessage()));
            return null;
        }

        if (project == null) {
            errorMessage = "No project is selected.";
        } else if (StringUtils.isBlank(newProjectName)) {
            errorMessage = "Project name is empty.";
        } else if (!NameChecker.checkName(newProjectName)) {
            errorMessage = "Project name '" + newProjectName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
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

            // it is possible to copy into the repository local only project
            // (publish it)
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot copy project.", errorMessage));
            return null;
        }

        try {
            userWorkspace.copyProject(project, newProjectName);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Failed to copy project.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }

        return null;
    }

    public String createDeploymentProject() {
        try {
            userWorkspace.createDDProject(projectName);
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Failed to create deployment project '" + projectName + "'.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }
        return null;
    }

    public String createNewRulesProject() {
        InputStream sampleRulesSource = this.getClass().getClassLoader().getResourceAsStream(newProjectTemplate);        
        String errorMessage = String.format("Can`t load template file: %s", newProjectTemplate);
        if (sampleRulesSource == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
            return null;
        }
        String rulesSourceName = "rules." + FilenameUtils.getExtension(newProjectTemplate);
        return createRulesProject(projectName, userWorkspace, sampleRulesSource, rulesSourceName);
    }

    // TODO Extract method to separate controller
    public String createRulesProject(String projectName, UserWorkspace userWorkspace,
            InputStream rulesSource, String rulesSourceName) {
        String errorMessage = null;
        RulesProjectBuilder projectBuilder = null;
        try {
            if (NameChecker.checkName(projectName)) {
                if (userWorkspace.hasProject(projectName)) {
                    errorMessage = "Cannot create project because project with such name already exists.";
                } else {
                    projectBuilder = new RulesProjectBuilder(userWorkspace, projectName, null);

                    projectBuilder.addFile(rulesSourceName, rulesSource);

                    projectBuilder.checkIn();

                    repositoryTreeState.invalidateTree();
                }
            } else {
                errorMessage = "Specified name is not a valid project name.";
            }
        } catch (Exception e) {
            if (projectBuilder != null) {
                projectBuilder.cancel();
            }
            LOG.error("Error creating project.", e);
            errorMessage = e.getMessage();
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, null, "New project created succesfully!"));
        }
        return null;
    }

    private String createRulesProjectFromZip(String projectName, UserWorkspace userWorkspace,
            ZipFile zipFile, PathFilter zipFilter) {
        String errorMessage = null;
        RulesProjectBuilder projectBuilder = null;
        try {
            if (NameChecker.checkName(projectName)) {
                if (userWorkspace.hasProject(projectName)) {
                    errorMessage = "Cannot create project because project with such name already exists.";
                } else {
                    projectBuilder = new RulesProjectBuilder(userWorkspace, projectName, zipFilter);

                    // Sort zip entries names alphabetically
                    Set<String> sortedNames = new TreeSet<String>();
                    for (Enumeration<? extends ZipEntry> items = zipFile.entries(); items.hasMoreElements();) {
                        ZipEntry item = items.nextElement();
                        sortedNames.add(item.getName());
                    }

                    for (String name : sortedNames) {
                        ZipEntry item = zipFile.getEntry(name);

                        if (item.isDirectory()) {
                            projectBuilder.addFolder(item.getName());
                        } else {
                            InputStream zipInputStream = zipFile.getInputStream(item);
                            projectBuilder.addFile(item.getName(), zipInputStream);
                        }
                    }
                    projectBuilder.checkIn();
                    repositoryTreeState.invalidateTree();
                }
            } else {
                errorMessage = "Specified name is not a valid project name.";
            }
        } catch (Exception e) {
            if (projectBuilder != null) {
                projectBuilder.cancel();
            }
            LOG.error("Error creating project.", e);
            errorMessage = e.getMessage();
        }

        if (errorMessage != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
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
            LOG.error("Cannot delete deployment project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete deployment project.", e
                            .getMessage()));
        }
        return null;
    }

    public String deleteElement() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) repositoryTreeState
                .getSelectedNode().getDataBean();
        String childName = FacesUtils.getRequestParameter("element");

        try {
            projectArtefact.getArtefact(childName).delete();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.refreshSelectedNode();
        } catch (ProjectException e) {
            LOG.error("Error deleting element.", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting.", e.getMessage()));
        }
        return null;
    }

    public String deleteNode() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) repositoryTreeState
                .getSelectedNode().getDataBean();
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
            LOG.error("Failed to delete node.", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete node.", e.getMessage()));
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
            LOG.error("Cannot delete rules project '" + projectName + "'.", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete rules project.", e.getMessage()));
        }
        return null;
    }

    public String eraseProject() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        // EPBDS-225
        if (project == null) {
            return null;
        }

        if (!project.isDeleted()) {
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Cannot erase project '" + project.getName()
                            + "'. It must be marked for deletion first!"));
            return null;
        }

        try {
            project.erase();
        } catch (ProjectException e) {
            String msg = "Cannot erase project '" + project.getName() + "'.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance()
                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, null, msg));
        } finally {
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();
        }
        return null;
    }

    public String exportProjectVersion() {
        File zipFile = null;
        String zipFileName = null;
        try {
            UserWorkspaceProject p = repositoryTreeState.getSelectedProject();
            zipFile = p.exportVersion(new CommonVersionImpl(version));
            zipFileName = String.format("%s-%s.zip", p.getName(), version);
        } catch (ProjectException e) {
            String msg = "Failed to export project version.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }

        if (zipFile != null) {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            writeOutContent(response, zipFile, zipFileName);
            facesContext.responseComplete();

            zipFile.delete();
        }
        return null;
    }

    public String filter() {
        OpenLFilter<?> filter = null;
        if (StringUtils.isNotBlank(filterString)) {
            filter = new RepositoryFileExtensionFilter(filterString);
        }
        repositoryTreeState.setFilter(filter);
        return null;
    }

    public String getAttribute1() {
        return (String) getProperty(JcrNT.PROP_ATTRIBUTE + 1);
    }

    public Date getAttribute10() {
        return getDateProperty(JcrNT.PROP_ATTRIBUTE + 10);
    }

    public String getAttribute11() {
        return getNumberProperty(JcrNT.PROP_ATTRIBUTE + 11);
    }

    public String getAttribute12() {
        return getNumberProperty(JcrNT.PROP_ATTRIBUTE + 12);
    }

    public String getAttribute13() {
        return getNumberProperty(JcrNT.PROP_ATTRIBUTE + 13);
    }

    public String getAttribute14() {
        return getNumberProperty(JcrNT.PROP_ATTRIBUTE + 14);
    }

    public String getAttribute15() {
        return getNumberProperty(JcrNT.PROP_ATTRIBUTE + 15);
    }

    public String getAttribute2() {
        return (String) getProperty(JcrNT.PROP_ATTRIBUTE + 2);
    }

    public String getAttribute3() {
        return (String) getProperty(JcrNT.PROP_ATTRIBUTE + 3);
    }

    public String getAttribute4() {
        return (String) getProperty(JcrNT.PROP_ATTRIBUTE + 4);
    }

    public String getAttribute5() {
        return (String) getProperty(JcrNT.PROP_ATTRIBUTE + 5);
    }

    public Date getAttribute6() {
        return getDateProperty(JcrNT.PROP_ATTRIBUTE + 6);
    }

    public Date getAttribute7() {
        return getDateProperty(JcrNT.PROP_ATTRIBUTE + 7);
    }

    public Date getAttribute8() {
        return getDateProperty(JcrNT.PROP_ATTRIBUTE + 8);
    }

    public Date getAttribute9() {
        return getDateProperty(JcrNT.PROP_ATTRIBUTE + 9);
    }

    /**
     * Gets date type property from a rules repository.
     *
     * @param propName name of property
     * @return value of property
     */
    private Date getDateProperty(String propName) {
        Object prop = getProperty(propName);
        if (prop instanceof Date) {
            return (Date) prop;
        } else if (prop instanceof Long) {
            return new Date((Long) prop);
        } else {
            return null;
        }
    }

    public String getDeploymentProjectName() {
        // EPBDS-92 - clear newDProject dialog every time
        return null;
    }

    /**
     * Gets all deployments projects from a repository.
     *
     * @return list of deployments projects
     */
    public List<AbstractTreeNode> getDeploymentProjects() {
        return repositoryTreeState.getDeploymentRepository().getChildNodes();
    }

    public Date getEffectiveDate() {
        RulesRepositoryArtefact dataBean = ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean());
        if (dataBean != null) {
            return dataBean.getEffectiveDate();
        }
        return null;
    }

    public Date getExpirationDate() {
        RulesRepositoryArtefact dataBean = ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean());
        if (dataBean != null) {
            return dataBean.getExpirationDate();
        }
        return null;
    }

    public String getFileName() {
        return null;
    }

    public String getFilterString() {
        return filterString;
    }

    public String getFolderName() {
        return null;
    }

    public String getLineOfBusiness() {
        RulesRepositoryArtefact dataBean = ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean());
        if (dataBean != null) {
            return dataBean.getLineOfBusiness();
        }
        return null;
    }

    public int getMajor() {
        ProjectVersion v = getProjectVersion();
        if (v != null) {
            return v.getMajor();
        }
        return major;
    }

    public int getMinor() {
        ProjectVersion v = getProjectVersion();
        if (v != null) {
            return v.getMinor();
        }
        return minor;
    }

    public String getNewProjectName() {
        // EPBDS-92 - clear newProject dialog every time
        return null;
    }

    /**
     * Gets number type property from a rules repository.
     *
     * @param propName name of property
     * @return value of property
     */
    private String getNumberProperty(String propName) {
        Object prop = getProperty(propName);
        if (prop instanceof Double) {
            return String.valueOf(prop);
        } else {
            return null;
        }
    }

    public String getProjectName() {
        // EPBDS-92 - clear newProject dialog every time
        // return projectName;
        return null;
    }

    private ProjectVersion getProjectVersion() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (project != null) {
            return project.getVersion();
        }
        return null;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        /*
         * Object dataBean = FacesUtils.getFacesVariable(
         * "#{repositoryTreeController.selected.dataBean}");
         */
        return properties;
    }

    /**
     * Gets property from a rules repository.
     *
     * @param propName name of property
     * @return value of property
     */
    private Object getProperty(String propName) {
        Map<String, Object> props = getProps();
        if (props != null) {
            return props.get(propName);
        }
        return null;
    }

    /**
     * Gets all properties from a rules repository.
     *
     * @return map of properties
     */
    private Map<String, Object> getProps() {
        RulesRepositoryArtefact dataBean = ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode()
                .getDataBean());
        if (dataBean != null) {
            return dataBean.getProps();
        }
        return null;
    }

    /**
     * Gets UI name of property.
     *
     * @param propName name of property
     * @return UI name of property
     */
    private String getPropUIName(String propName) {
        if (propName == null) {
            return StringUtils.EMPTY;
        }
        String propUIName = getPropUINames().get(propName);
        if (StringUtils.isBlank(propUIName)) {
            propUIName = propName;
        }
        return propUIName;
    }

    public Map<String, String> getPropUINames() {
        return repositoryArtefactPropsHolder.getProps();
    }

    public int getRevision() {
        ProjectVersion v = getProjectVersion();
        if (v != null) {
            return v.getRevision();
        }
        return 0;
    }

    /**
     * Gets all rules projects from a rule repository.
     *
     * @return list of rules projects
     */
    public List<AbstractTreeNode> getRulesProjects() {
        return repositoryTreeState.getRulesRepository().getChildNodes();
    }

    public SelectItem[] getSelectedProjectVersions() {
        Collection<ProjectVersion> versions = repositoryTreeState.getSelectedNode().getVersions();

        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ProjectVersion version : versions) {
            selectItems.add(new SelectItem(version.getVersionName()));
        }
        return selectItems.toArray(new SelectItem[selectItems.size()]);
    }

    public String getUploadFrom() {
        return uploadFrom;
    }

    public String getVersion() {
        return version;
    }

    public String openProject() {
        try {
            repositoryTreeState.getSelectedProject().open();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Failed to open project.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }
        return null;
    }

    public String openProjectVersion() {
        try {
            repositoryTreeState.getSelectedProject().openVersion(new CommonVersionImpl(version));
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Failed to open project version.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        }
        return null;
    }

    public String refreshTree() {
        repositoryTreeState.invalidateTree();
        repositoryTreeState.invalidateSelection();
        return null;
    }

    public String selectDeploymentProject() {
        String projectName = FacesUtils.getRequestParameter("projectName");
        selectProject(projectName, repositoryTreeState.getDeploymentRepository());
        return null;
    }

    private void selectProject(String projectName, TreeRepository root) {
        for (AbstractTreeNode node : root.getChildNodes()) {
            if (node.getName().equals(projectName)) {
                repositoryTreeState.setSelectedNode(node);
                repositoryTreeState.refreshSelectedNode();
                break;
            }
        }
    }

    public String selectRulesProject() {
        String projectName = FacesUtils.getRequestParameter("projectName");
        selectProject(projectName, repositoryTreeState.getRulesRepository());
        return null;
    }

    public void setAttribute1(String attribute1) {
        setProperty(JcrNT.PROP_ATTRIBUTE + 1, attribute1);
    }

    public void setAttribute10(Date attribute10) {
        setDateProperty(JcrNT.PROP_ATTRIBUTE + 10, attribute10);
    }

    public void setAttribute11(String attribute11) {
        setNumberProperty(JcrNT.PROP_ATTRIBUTE + 11, attribute11);
    }

    public void setAttribute12(String attribute12) {
        setNumberProperty(JcrNT.PROP_ATTRIBUTE + 12, attribute12);
    }

    public void setAttribute13(String attribute13) {
        setNumberProperty(JcrNT.PROP_ATTRIBUTE + 13, attribute13);
    }

    public void setAttribute14(String attribute14) {
        setNumberProperty(JcrNT.PROP_ATTRIBUTE + 14, attribute14);
    }

    public void setAttribute15(String attribute15) {
        setNumberProperty(JcrNT.PROP_ATTRIBUTE + 15, attribute15);
    }

    public void setAttribute2(String attribute2) {
        setProperty(JcrNT.PROP_ATTRIBUTE + 2, attribute2);
    }

    public void setAttribute3(String attribute3) {
        setProperty(JcrNT.PROP_ATTRIBUTE + 3, attribute3);
    }

    public void setAttribute4(String attribute4) {
        setProperty(JcrNT.PROP_ATTRIBUTE + 4, attribute4);
    }

    public void setAttribute5(String attribute5) {
        setProperty(JcrNT.PROP_ATTRIBUTE + 5, attribute5);
    }

    public void setAttribute6(Date attribute6) {
        setDateProperty(JcrNT.PROP_ATTRIBUTE + 6, attribute6);
    }

    public void setAttribute7(Date attribute7) {
        setDateProperty(JcrNT.PROP_ATTRIBUTE + 7, attribute7);
    }

    public void setAttribute8(Date attribute8) {
        setDateProperty(JcrNT.PROP_ATTRIBUTE + 8, attribute8);
    }

    public void setAttribute9(Date attribute9) {
        setDateProperty(JcrNT.PROP_ATTRIBUTE + 9, attribute9);
    }

    /**
     * Sets date type property to rules repository.
     *
     * @param propName name of property
     * @param propValue value of property
     */
    public void setDateProperty(String propName, Date propValue) {
        if (!SPECIAL_DATE.equals(propValue)) {
            setProperty(propName, propValue);
        } else {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Specified " + getPropUIName(propName)
                            + " value is not a valid date."));
        }
    }

    public void setEffectiveDate(Date date) {
        if (!SPECIAL_DATE.equals(date)) {
            try {
                ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).setEffectiveDate(date);
            } catch (ProjectException e) {
                LOG.error("Failed to set effective date!", e);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Can not set effective date.", e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
                            "Specified effective date value is not a valid date."));
        }
    }

    public void setExpirationDate(Date date) {
        if (!SPECIAL_DATE.equals(date)) {
            try {
                ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).setExpirationDate(date);
            } catch (ProjectException e) {
                LOG.error("Failed to set expiration date!", e);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Can not set expiration date.", e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
                            "Specified expiration date value is not a valid date."));
        }
    }

    public List<UploadItem> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadItem> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setLineOfBusiness(String lineOfBusiness) {
        try {
            ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean())
                    .setLineOfBusiness(lineOfBusiness);
        } catch (ProjectException e) {
            LOG.error("Failed to set LOB!", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Can not set line of business.", e.getMessage()));
        }
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = newProjectName;
    }

    /**
     * Sets number type property to rules repository.
     *
     * @param propName name of property
     * @param propValue value of property
     */
    public void setNumberProperty(String propName, String propValue) {
        Double numberValue = null;
        try {
            if (StringUtils.isNotBlank(propValue)) {
                numberValue = Double.valueOf(propValue);
            }
            setProperty(propName, numberValue);
        } catch (NumberFormatException e) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, "Specified " + getPropUIName(propName)
                            + " value is not a number."));
        }
    }

    public void setProjectName(String newProjectName) {
        projectName = newProjectName;
    }

    /**
     * Sets property to rules repository.
     *
     * @param propName name of property
     * @param propValue value of property
     */
    private void setProperty(String propName, Object propValue) {
        try {
            Map<String, Object> props = getProps();
            if (props == null) {
                props = new HashMap<String, Object>();
            } else {
                props = new HashMap<String, Object>(props);
            }
            props.put(propName, propValue);
            ((RulesRepositoryArtefact) repositoryTreeState.getSelectedNode().getDataBean()).setProps(props);
        } catch (ProjectException e) {
            String propUIName = getPropUIName(propName);
            LOG.error("Failed to set " + propUIName + "!", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Can not set " + propUIName + ".", e.getMessage()));
        }
    }

    public void setRepositoryArtefactPropsHolder(RepositoryArtefactPropsHolder repositoryArtefactPropsHolder) {
        this.repositoryArtefactPropsHolder = repositoryArtefactPropsHolder;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setUploadFrom(String uploadFrom) {
        this.uploadFrom = uploadFrom;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String undeleteProject() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (!project.isDeleted()) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot undelete project '" + project.getName()
                            + "'.", "Project is not marked for deletion."));
            return null;
        }

        try {
            project.undelete();
            repositoryTreeState.invalidateTree();
        } catch (ProjectException e) {
            String msg = "Cannot undelete project '" + project.getName() + "'.";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
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
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("File was successfully updated."));
            repositoryTreeState.invalidateTree();
        } else {
            FacesContext.getCurrentInstance()
                    .addMessage(
                            null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMessage,
                                    "Error occured during uploading file."));
        }
        return null;
    }

    public String upload() {
        String errorMessage = uploadProject();
        if (errorMessage == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Project was uploaded successfully."));
            repositoryTreeState.invalidateTree();
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, null, errorMessage));
        }
        return null;
    }

    private String uploadAndAddFile() {
        if (!NameChecker.checkName(fileName)) {
            return "File name '" + fileName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
        }

        File uploadedFile = getLastUploadedFile().getFile();

        try {
            UserWorkspaceProjectFolder node = (UserWorkspaceProjectFolder) repositoryTreeState.getSelectedNode()
                    .getDataBean();

            ProjectResource projectResource = new FileProjectResource(new FileInputStream(uploadedFile));
            node.addResource(fileName, projectResource);

            clearUploadedFiles();
        } catch (Exception e) {
            LOG.error("Error adding file to user workspace.", e);
            return e.getMessage();
        }

        return null;
    }

    private String uploadAndUpdateFile() {
        File uploadedFile = getLastUploadedFile().getFile();
        try {
            UserWorkspaceProjectResource node = (UserWorkspaceProjectResource) repositoryTreeState.getSelectedNode()
                    .getDataBean();
            node.setContent(new FileInputStream(uploadedFile));

            clearUploadedFiles();
        } catch (Exception e) {
            LOG.error("Error updating file in user workspace.", e);
            return e.getMessage();
        }

        return null;
    }

    private UploadItem getLastUploadedFile() {
        if (!uploadedFiles.isEmpty()) {
            return uploadedFiles.get(uploadedFiles.size() - 1);
        }
        return null;
    }

    private String uploadProject() {
        UploadItem uploadedItem = getLastUploadedFile();
        File uploadedFile = uploadedItem.getFile();

        if (userWorkspace.hasProject(projectName)) {
            return "Cannot create project because project with such name already exists.";
        }

        try {
            if (uploadedFile != null && uploadedFile.isFile()) {
                if (FileTypeHelper.isZipFile(uploadedItem.getFileName())) {
                    ZipFile zipFile = new ZipFile(uploadedFile);
                    createRulesProjectFromZip(projectName, userWorkspace, zipFile, zipFilter);
                } else if (FileTypeHelper.isExcelFile(uploadedItem.getFileName())) {
                    createRulesProject(projectName, userWorkspace, new FileInputStream(uploadedFile),
                            uploadedItem.getFileName());
                }
            }
            clearUploadedFiles();
        } catch (Exception e) {
            LOG.error("Error while uploading project.", e);
            return "" + e.getMessage();
        }

        return null;
    }

    private void clearUploadedFiles() {
        for (UploadItem uploadFile : uploadedFiles) {
            uploadFile.getFile().delete();
        }
        uploadedFiles.clear();
    }

    private void writeOutContent(final HttpServletResponse res, final File content, final String theFilename) {
        if (content == null) {
            return;
        }
        FileInputStream input = null;
        try {
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
            res.setContentType("application/zip");
            res.setHeader("Content-disposition", "attachment; filename=" + theFilename);

            input = new FileInputStream(content);
            IOUtils.copy(input, res.getOutputStream());
        } catch (final IOException e) {
            String msg = "Failed to write content of '" + content.getAbsolutePath() + "' into response!";
            LOG.error(msg, e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, e.getMessage()));
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    String msg = "Failed to close content stream.";
                    LOG.error(msg, e);
                }
            }
        }
    }

    public String getNewProjectTemplate() {
        return newProjectTemplate;
    }

    public void setNewProjectTemplate(String newProjectTemplate) {
        this.newProjectTemplate = newProjectTemplate;
    }

    public SelectItem[] getNewProjectTemplates() {
        return FacesUtils.createSelectItems(projectTemplates);
    }

}
