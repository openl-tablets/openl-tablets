package org.openl.rules.webstudio.web.repository;

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.StreamException;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.ResourceTransformer;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.*;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.filter.RepositoryFileExtensionFilter;
import org.openl.rules.webstudio.util.ExportModule;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.project.CustomTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.PredefinedTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.project.TemplatesResolver;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.repository.upload.ProjectUploader;
import org.openl.rules.webstudio.web.repository.upload.ZipProjectDescriptorExtractor;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.impl.ProjectExportHelper;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.rules.webstudio.filter.IFilter;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_DELETE_DEPLOYMENT;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_DELETE_PROJECTS;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_UNLOCK_DEPLOYMENT;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_UNLOCK_PROJECTS;

/**
 * Repository tree controller. Used for retrieving data for repository tree and
 * performing repository actions.
 *
 * @author Aleh Bykhavets
 * @author Andrey Naumenko
 */
@ManagedBean
@ViewScoped
public class RepositoryTreeController {

    private static final String PROJECT_HISTORY_HOME = "project.history.home";
    private static final String CUSTOM_TEMPLATE_TYPE = "custom";

    private final Logger log = LoggerFactory.getLogger(RepositoryTreeController.class);

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @ManagedProperty(value = "#{rulesUserSession.userWorkspace}")
    private volatile UserWorkspace userWorkspace;

    @ManagedProperty(value = "#{zipFilter}")
    private PathFilter zipFilter;

    @ManagedProperty("#{projectDescriptorArtefactResolver}")
    private ProjectDescriptorArtefactResolver projectDescriptorResolver;

    private WebStudio studio = WebStudioUtils.getWebStudio(true);

    private String projectName;
    private String newProjectTemplate;
    private String folderName;
    private List<ProjectFile> uploadedFiles = new ArrayList<ProjectFile>();
    private String fileName;
    private String uploadFrom;
    private String newProjectName;
    private String version;

    private String filterString;
    private boolean hideDeleted;

    private boolean openDependencies = true;
    private AProject currentProject;

    private TemplatesResolver predefinedTemplatesResolver = new PredefinedTemplatesResolver();
    private TemplatesResolver customTemplatesResolver = new CustomTemplatesResolver();

    public PathFilter getZipFilter() {
        return zipFilter;
    }

    private TreeNode activeProjectNode;

    public void setZipFilter(PathFilter zipFilter) {
        this.zipFilter = zipFilter;
    }

    /**
     * Adds new file to active node (project or folder).
     */
    public String addFile() {
        if (getLastUploadedFile() == null) {
            FacesUtils.addErrorMessage("Please select file to be uploaded.");
            return null;
        }
        if (StringUtils.isEmpty(fileName)) {
            FacesUtils.addErrorMessage("File name must not be empty.");
            return null;
        }

        String errorMessage = uploadAndAddFile();

        if (errorMessage == null) {
            resetStudioModel();
            FacesUtils.addInfoMessage("File was uploaded successfully.");
        } else {
            FacesUtils.addErrorMessage(errorMessage);
        }

        /* Clear the load form */
        this.clearForm();

        return null;
    }

    public String addFolder() {
        AProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getData();
        String errorMessage = null;

        if (projectArtefact instanceof AProjectFolder) {
            if (folderName != null && !folderName.isEmpty()) {
                if (NameChecker.checkName(folderName)) {
                    if (!NameChecker.checkIsFolderPresent((AProjectFolder) projectArtefact, folderName)) {
                        AProjectFolder folder = (AProjectFolder) projectArtefact;

                        try {
                            AProjectFolder addedFolder = folder.addFolder(folderName);
                            repositoryTreeState.addNodeToTree(repositoryTreeState.getSelectedNode(), addedFolder);
                            resetStudioModel();
                        } catch (ProjectException e) {
                            log.error("Failed to create folder '{}'.", folderName, e);
                            errorMessage = e.getMessage();
                        }
                    } else {
                        errorMessage = "Folder name '" + folderName + "' is invalid. " + NameChecker.FOLDER_EXISTS;
                    }
                } else {

                    errorMessage = "Folder name '" + folderName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
                }
            } else {
                errorMessage = "Folder name '" + folderName + "' is invalid. " + NameChecker.FOLDER_NAME_EMPTY;
            }
        }

        if (errorMessage != null) {
            FacesUtils.addErrorMessage("Failed to create folder.", errorMessage);
        }
        return null;
    }

    public String saveProject() {
        try {
            UserWorkspaceProject project = repositoryTreeState.getSelectedProject();

            project.save();

            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (ProjectException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg);
        }
        return null;
    }

    public String editProject() {
        try {
            repositoryTreeState.getSelectedProject().edit();
            openDependenciesIfNeeded();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (ProjectException e) {
            String msg = "Failed to edit project.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    public String closeProject() {
        try {
            if (repositoryTreeState.getSelectedProject().equals(studio.getModel().getProject())) {
                studio.getModel().clearModuleInfo();
            }
            repositoryTreeState.getSelectedProject().close();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (Exception e) {
            String msg = "Failed to close project.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    public boolean getHasDependingProjects() {
        return !getDependingProjects().isEmpty();
    }

    public List<String> getDependingProjects() {
        List<String> projects = new ArrayList<String>();
        TreeNode selectedNode = getSelectedNode();
        TreeProject projectNode = selectedNode instanceof TreeProject ? (TreeProject) selectedNode : null;
        if (projectNode != null) {
            String name = projectNode.getLogicalName();

            for (ProjectDescriptor projectDescriptor : studio.getAllProjects()) {
                if (projectDescriptor.getDependencies() != null) {
                    for (ProjectDependencyDescriptor dependency : projectDescriptor.getDependencies()) {
                        if (dependency.getName().equals(name)) {
                            projects.add(projectDescriptor.getName());
                            break;
                        }
                    }
                }
            }
        }
        return projects;
    }

    public boolean getHasDependencies() {
        return !getDependencies(getSelectedProject(), false).isEmpty();
    }

    public boolean getHasDependenciesForVersion() {
        if (!getIsOpenVersionDialogOpened()) {
            return false;
        }
        if (version == null) {
            return false;
        }
        try {
            AProject selectedProject = repositoryTreeState.getSelectedProject();
            AProject newVersion = userWorkspace.getDesignTimeRepository().getProject(selectedProject.getName(),
                new CommonVersionImpl(version));
            return !getDependencies(newVersion, false).isEmpty();
        } catch (ProjectException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public Collection<String> getDependencies() {
        if (!repositoryTreeState.getCanOpen() && !repositoryTreeState.getCanEdit()) {
            // Because ui:repeat ignores the "rendered" property, here is a
            // workaround to reduce performance drop.
            return Collections.emptyList();
        }
        if (getSelectedProject() instanceof ADeploymentProject) {
            return Collections.emptyList();
        }
        List<String> dependencies = new ArrayList<String>(getDependencies(getSelectedProject(), true));
        Collections.sort(dependencies);
        return dependencies;
    }

    public Collection<String> getDependenciesForVersion() {
        if (version == null || !getIsOpenVersionDialogOpened()) {
            // Because ui:repeat ignores the "rendered" property, here is a
            // workaround to reduce performance drop.
            return Collections.emptyList();
        }
        try {
            AProject selectedProject = repositoryTreeState.getSelectedProject();
            AProject newVersion = userWorkspace.getDesignTimeRepository().getProject(selectedProject.getName(),
                new CommonVersionImpl(version));
            List<String> dependencies = new ArrayList<String>(getDependencies(newVersion, true));
            Collections.sort(dependencies);
            return dependencies;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Collection<String> getDependencies(AProject project, boolean recursive) {
        Collection<String> dependencies = new HashSet<String>();
        if (project != null) {
            calcDependencies(dependencies, project, recursive);
        }
        return dependencies;
    }

    private void calcDependencies(Collection<String> result, AProject project, boolean recursive) {
        List<ProjectDependencyDescriptor> dependencies;
        try {
            dependencies = projectDescriptorResolver.getDependencies(project);
            if (dependencies == null) {
                return;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // Skip this dependency
            return;
        }

        for (ProjectDependencyDescriptor dependency : dependencies) {
            try {
                TreeProject projectNode = repositoryTreeState.getProjectNodeByLogicalName(dependency.getName());
                if (projectNode == null) {
                    continue;
                }
                String physicalName = projectNode.getName();
                AProject dependentProject = userWorkspace.getProject(physicalName, false);
                if (canOpen(dependentProject)) {
                    result.add(dependency.getName());
                }

                if (recursive) {
                    calcDependencies(result, dependentProject, true);
                }
            } catch (ProjectException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private boolean canOpen(AProject theProject) {
        if (!(theProject instanceof UserWorkspaceProject)) {
            return false;
        }

        UserWorkspaceProject project = (UserWorkspaceProject) theProject;
        return !project.isLocalOnly() && !project.isOpenedForEditing() && !project.isOpened() && !project.isDeleted();
    }

    public String copyDeploymentProject() {
        String errorMessage = null;
        ADeploymentProject project;

        try {
            project = userWorkspace.getDDProject(projectName);
        } catch (ProjectException e) {
            log.error("Cannot obtain deployment project '{}'.", projectName, e);
            FacesUtils.addErrorMessage(e.getMessage());
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
            FacesUtils.addErrorMessage("Cannot copy deployment project.", errorMessage);
            return null;
        }

        try {
            userWorkspace.copyDDProject(project, newProjectName);
            ADeploymentProject newProject = userWorkspace.getDDProject(newProjectName);
            repositoryTreeState.addDeploymentProjectToTree(newProject);
        } catch (ProjectException e) {
            String msg = "Failed to copy deployment project.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }

        return null;
    }

    public String copyProject() {
        String errorMessage = null;
        AProject project;

        try {
            project = userWorkspace.getProject(projectName);
        } catch (ProjectException e) {
            log.error("Cannot obtain rules project '{}'.", projectName, e);
            FacesUtils.addErrorMessage(e.getMessage());
            return null;
        }

        if (project == null) {
            errorMessage = "No project is selected.";
        } else if (StringUtils.isBlank(newProjectName)) {
            errorMessage = "Project name is empty.";
        } else if (!NameChecker.checkName(newProjectName)) {
            errorMessage = "Project name '" + newProjectName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
        } else if (userWorkspace.hasProject(newProjectName)) {
            errorMessage = "Project '" + newProjectName + "' already exists.";
        }

        if (errorMessage != null) {
            FacesUtils.addErrorMessage("Cannot copy project.", errorMessage);
            return null;
        }

        try {
            userWorkspace.copyProject(project, newProjectName, new ResourceTransformer() {
                @Override
                public InputStream tranform(AProjectResource resource) throws ProjectException {
                    if (isProjectDescriptor(resource)) {
                        InputStream content = null;
                        try {
                            XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer(false);
                            content = resource.getContent();
                            ProjectDescriptor projectDescriptor = serializer.deserialize(content);
                            projectDescriptor.setName(newProjectName);
                            return IOUtils.toInputStream(serializer.serialize(projectDescriptor));
                        } catch (XStreamException e) {
                            // Can't parse rules.xml. Don't modify it.
                            log.error(e.getMessage(), e);
                        } finally {
                            IOUtils.closeQuietly(content);
                        }
                    }

                    return resource.getContent();
                }

                private boolean isProjectDescriptor(AProjectResource resource) {
                    String actualFullPath = resource.getArtefactPath().withoutFirstSegment().getStringValue();
                    String expectedFullPath = ArtefactPathImpl.SEGMENT_DELIMITER + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME;
                    return expectedFullPath.equals(actualFullPath);
                }
            });
            AProject newProject = userWorkspace.getProject(newProjectName);
            repositoryTreeState.addRulesProjectToTree(newProject);
            resetStudioModel();
        } catch (ProjectException e) {
            String msg = "Failed to copy project.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }

        return null;
    }

    public String createDeploymentConfiguration() {
        try {
            if (userWorkspace.hasDDProject(projectName)) {
                String msg = "Cannot create configuration because configuration with such name already exists.";
                FacesUtils.addErrorMessage(msg, null);

                return null;
            }

            ADeploymentProject createdProject = userWorkspace.createDDProject(projectName);
            createdProject.edit();
            // Analogous to rules project creation (to change "created by"
            // property and revision)
            createdProject.save();
            createdProject.edit();
            repositoryTreeState.addDeploymentProjectToTree(createdProject);
            FacesUtils.addInfoMessage(String.format("Deploy configuration '%s' is successfully created", projectName));
        } catch (ProjectException e) {
            String msg = "Failed to create deploy configuration '" + projectName + "'.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }

        /* Clear the load form */
        this.clearForm();

        return null;
    }

    public String createNewRulesProject() {
        String msg = validateProjectName();

        if (msg != null) {
            this.clearForm();
            FacesUtils.addErrorMessage(msg);
            return null;
        }

        String[] templateParts = newProjectTemplate.split("/");
        TemplatesResolver templatesResolver = CUSTOM_TEMPLATE_TYPE
            .equals(templateParts[0]) ? customTemplatesResolver : predefinedTemplatesResolver;
        ProjectFile[] templateFiles = templatesResolver.getProjectFiles(templateParts[1], templateParts[2]);
        if (templateFiles.length <= 0) {
            this.clearForm();
            String errorMessage = String.format("Can`t load template files: %s", newProjectTemplate);
            FacesUtils.addErrorMessage(errorMessage);
            return null;
        }

        ExcelFilesProjectCreator projectCreator = new ExcelFilesProjectCreator(projectName,
            userWorkspace,
            zipFilter,
            templateFiles);
        String creationMessage = projectCreator.createRulesProject();
        if (creationMessage == null) {
            try {
                AProject createdProject = userWorkspace.getProject(projectName);

                repositoryTreeState.addRulesProjectToTree(createdProject);
                selectProject(projectName, repositoryTreeState.getRulesRepository());

                repositoryTreeState.getSelectedProject().close();
                repositoryTreeState.refreshSelectedNode();

                resetStudioModel();

                FacesUtils.addInfoMessage("Project was created successfully.");
                /* Clear the load form */
                this.clearForm();
                this.editProject();
            } catch (ProjectException e) {
                creationMessage = e.getMessage();
            }
        } else {
            FacesUtils.addErrorMessage(creationMessage);
        }

        return creationMessage;
    }

    private String validateProjectName() {
        String msg = null;
        if (StringUtils.isBlank(projectName)) {
            msg = "Project name must not be empty.";
        } else if (!NameChecker.checkName(projectName)) {
            msg = "Specified name is not a valid project name." + " " + NameChecker.BAD_NAME_MSG;
        } else if (userWorkspace
            .hasProject(projectName) || repositoryTreeState.getProjectNodeByLogicalName(projectName) != null) {
            msg = "Cannot create project because project with such name already exists.";
        }
        return msg;
    }

    /*
     * Because of renaming 'Deployment project' to 'Deploy Configuration' the
     * method was renamed too.
     */
    public String deleteDeploymentConfiguration() {
        String projectName = FacesUtils.getRequestParameter("deploymentProjectName");

        try {
            ADeploymentProject project = userWorkspace.getDDProject(projectName);
            project.delete(userWorkspace.getUser());
            if (repositoryTreeState.isHideDeleted()) {
                TreeNode projectInTree = repositoryTreeState.getDeploymentRepository()
                    .getChild(RepositoryUtils.getTreeNodeId(project.getName()));
                repositoryTreeState.deleteNode(projectInTree);
            }

            FacesUtils.addInfoMessage("Deploy configuration was deleted successfully.");
        } catch (ProjectException e) {
            log.error("Cannot delete deploy configuration '" + projectName + "'.", e);
            FacesUtils.addErrorMessage("Failed to delete deploy configuration.", e.getMessage());
        }
        return null;
    }

    private void findModulePaths(AProjectArtefact projectArtefact, Collection<String> modulePaths) {
        if (projectArtefact.isFolder()) {
            AProjectFolder projectFolder = (AProjectFolder) projectArtefact;
            for (AProjectArtefact artifact : projectFolder.getArtefacts()) {
                findModulePaths(artifact, modulePaths);
            }
        } else {
            if (FileTypeHelper.isExcelFile(projectArtefact.getName())) {
                String modulePath = projectArtefact.getArtefactPath().withoutFirstSegment().getStringValue();
                while (modulePath.charAt(0) == '/') {
                    modulePath = modulePath.substring(1);
                }
                modulePaths.add(modulePath);
            }
        }
    }

    private void unregisterSelectedNodeInProjectDescriptor() throws ProjectException {
        TreeNode selectedNode = getSelectedNode();
        String nodeType = selectedNode.getType();
        if (UiConst.TYPE_FOLDER.equals(nodeType) || UiConst.TYPE_FILE.equals(nodeType)) {
            unregisterArtifactInProjectDescriptor(selectedNode.getData());
        }
    }

    private void unregisterArtifactInProjectDescriptor(AProjectArtefact aProjectArtefact) throws ProjectException {
        UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
        AProjectArtefact projectDescriptorArtifact;
        try {
            projectDescriptorArtifact = selectedProject
                .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        } catch (ProjectException ex) {
            // Project doesn't contain rules.xml file
            return;
        }
        Collection<String> modulePaths = new HashSet<String>();
        findModulePaths(aProjectArtefact, modulePaths);
        if (projectDescriptorArtifact instanceof AProjectResource) {
            String projectDescriptorPath = projectDescriptorArtifact.getArtefactPath().withoutFirstSegment() .getStringValue();
            if (projectDescriptorPath.equals(aProjectArtefact.getArtefactPath().withoutFirstSegment().getStringValue())) {
                // There is no need to unregister itself
                return;
            }

            AProjectResource resource = (AProjectResource) projectDescriptorArtifact;
            InputStream content = resource.getContent();
            ProjectDescriptor projectDescriptor;
            try {
                projectDescriptor = xmlProjectDescriptorSerializer.deserialize(content);
            } catch (StreamException e) {
                log.error("Broken rules.xml file. Can't remove modules from it", e);
                return;
            }
            for (String modulePath : modulePaths) {
                Iterator<Module> itr = projectDescriptor.getModules().iterator();
                while (itr.hasNext()) {
                    Module module = itr.next();
                    if (modulePath.equals(module.getRulesRootPath().getPath())) {
                        itr.remove();
                    }
                }
            }
            String xmlString = xmlProjectDescriptorSerializer.serialize(projectDescriptor);
            InputStream newContent = IOUtils.toInputStream(xmlString);
            resource.setContent(newContent);
        }
    }

    private void unregisterElementInProjectDescriptor() {
        AProjectFolder projectArtefact = (AProjectFolder) repositoryTreeState.getSelectedNode().getData();
        String childName = FacesUtils.getRequestParameter("element");
        try {
            unregisterArtifactInProjectDescriptor(projectArtefact.getArtefact(childName));
        } catch (ProjectException ex) {
            log.error(ex.getMessage(), ex);
            FacesUtils.addErrorMessage("Error deleting.", ex.getMessage());
        }
    }

    public String deleteElement() {
        AProjectFolder projectArtefact = (AProjectFolder) repositoryTreeState.getSelectedNode().getData();
        String childName = FacesUtils.getRequestParameter("element");

        try {
            studio.getModel().clearModuleInfo(); // Release resources like jars
            unregisterElementInProjectDescriptor();
            projectArtefact.deleteArtefact(childName);
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();

            FacesUtils.addInfoMessage("Element was deleted successfully.");
        } catch (ProjectException e) {
            log.error("Error deleting element.", e);
            FacesUtils.addErrorMessage("Error deleting.", e.getMessage());
        }
        return null;
    }

    public String deleteNode() {
        TreeNode selectedNode = getSelectedNode();
        AProjectArtefact projectArtefact = selectedNode.getData();
        try {
            studio.getModel().clearModuleInfo(); // Release resources like jars
            String nodeType = selectedNode.getType();
            unregisterSelectedNodeInProjectDescriptor();
            projectArtefact.delete();
            if (selectedNode != activeProjectNode) {
                boolean wasMarkedForDeletion = UiConst.TYPE_DEPLOYMENT_PROJECT.equals(nodeType) || (UiConst.TYPE_PROJECT
                    .equals(nodeType) && !((UserWorkspaceProject) projectArtefact).isLocalOnly());
                if (wasMarkedForDeletion && !repositoryTreeState.isHideDeleted()) {
                    repositoryTreeState.refreshSelectedNode();
                } else {
                    repositoryTreeState.deleteSelectedNodeFromTree();
                }
            } else {
                if (repositoryTreeState.isHideDeleted() || ((UserWorkspaceProject) projectArtefact).isLocalOnly()) {
                    repositoryTreeState.deleteNode(selectedNode);
                    repositoryTreeState.invalidateSelection();
                }
            }
            activeProjectNode = null;
            resetStudioModel();

            String nodeTypeName;
            if (UiConst.TYPE_PROJECT.equals(nodeType)) {
                nodeTypeName = "Project";
            } else if (UiConst.TYPE_DEPLOYMENT_PROJECT.equals(nodeType)) {
                nodeTypeName = "Deploy configuration";
            } else if (UiConst.TYPE_FOLDER.equals(nodeType)) {
                nodeTypeName = "Folder";
            } else {
                nodeTypeName = "File";
            }
            FacesUtils.addInfoMessage(nodeTypeName + " was deleted successfully.");
        } catch (ProjectException e) {
            log.error("Failed to delete node.", e);
            FacesUtils.addErrorMessage("Failed to delete node.", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete node.", e);
            FacesUtils.addErrorMessage("Failed to delete node.", e.getMessage());
        }

        return null;
    }

    public String unlockNode() {
        AProjectArtefact projectArtefact = repositoryTreeState.getSelectedNode().getData();
        try {
            projectArtefact.unlock(userWorkspace.getUser());
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();

            FacesUtils.addInfoMessage("File was unlocked successfully.");
        } catch (ProjectException e) {
            log.error("Failed to unlock node.", e);
            FacesUtils.addErrorMessage("Failed to unlock node.", e.getMessage());
        }

        return null;
    }

    public String unlockProject() {
        String projectName = FacesUtils.getRequestParameter("projectName");

        try {
            RulesProject project = userWorkspace.getProject(projectName);
            project.unlock(userWorkspace.getUser());
            resetStudioModel();
        } catch (ProjectException e) {
            log.error("Cannot unlock rules project '{}'.", projectName, e);
            FacesUtils.addErrorMessage("Failed to unlock rules project.", e.getMessage());
        }
        return null;
    }

    public String unlockDeploymentConfiguration() {
        String deploymentProjectName = FacesUtils.getRequestParameter("deploymentProjectName");

        try {
            ADeploymentProject deploymentProject = userWorkspace.getDDProject(deploymentProjectName);
            deploymentProject.unlock(userWorkspace.getUser());
            resetStudioModel();
        } catch (ProjectException e) {
            log.error("Cannot unlock deployment project '{}'.", deploymentProjectName, e);
            FacesUtils.addErrorMessage("Failed to unlock deployment project.", e.getMessage());
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
            FacesUtils.addErrorMessage(
                "Cannot erase project '" + project.getName() + "'. It must be marked for deletion first!");
            return null;
        }

        try {
            projectDescriptorResolver.deleteRevisionsFromCache(project);
            synchronized (userWorkspace) {
                project.erase();
            }
            deleteProjectHistory(project.getName());
            userWorkspace.refresh();

            repositoryTreeState.deleteSelectedNodeFromTree();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();

            resetStudioModel();
            FacesUtils.addInfoMessage("Project was erased successfully.");
        } catch (ProjectException e) {
            repositoryTreeState.invalidateTree();
            String msg = "Cannot erase project '" + project.getName() + "'.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg);
        }
        return null;
    }

    public void deleteProjectHistory(String projectName) {
        try {
            String projectHistoryPath = studio.getSystemConfigManager()
                .getPath(PROJECT_HISTORY_HOME) + File.separator + projectName;
            File dir = new File(projectHistoryPath);
            // Project can contain no history
            if (dir.exists()) {
                FileUtils.delete(dir);
            }
        } catch (Exception e) {
            String msg = "Failed to clean history of project '" + projectName + "'!";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
    }

    public String exportProjectVersion() {
        File zipFile = null;
        String zipFileName = null;
        try {
            AProject selectedProject = repositoryTreeState.getSelectedProject();
            AProject forExport = userWorkspace.getDesignTimeRepository().getProject(selectedProject.getName(),
                new CommonVersionImpl(version));
            zipFile = ProjectExportHelper.export(userWorkspace.getUser(), forExport);
            zipFileName = String.format("%s-%s.zip", selectedProject.getName(), version);
        } catch (ProjectException e) {
            String msg = "Failed to export project version.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }

        if (zipFile != null) {
            final FacesContext facesContext = FacesUtils.getFacesContext();
            HttpServletResponse response = (HttpServletResponse) FacesUtils.getResponse();
            ExportModule.writeOutContent(response, zipFile, zipFileName);
            facesContext.responseComplete();

            if (!zipFile.delete()) {
                log.warn("Temporary zip file {} wasn't deleted", zipFile.getName());
            }
        }
        return null;
    }

    public String exportFileVersion() {
        File file = null;
        String fileName;
        InputStream is = null;
        OutputStream os = null;
        try {
            AProject selectedProject = repositoryTreeState.getSelectedProject();
            AProject forExport = userWorkspace.getDesignTimeRepository().getProject(selectedProject.getName(),
                new CommonVersionImpl(version));
            TreeNode selectedNode = repositoryTreeState.getSelectedNode();
            fileName = selectedNode.getName();
            ArtefactPath selectedNodePath = selectedNode.getData().getArtefactPath().withoutFirstSegment();

            is = ((AProjectResource) forExport.getArtefactByPath(selectedNodePath)).getContent();
            file = File.createTempFile("export-", "-file");
            os = new FileOutputStream(file);
            IOUtils.copy(is, os);

            final FacesContext facesContext = FacesUtils.getFacesContext();
            HttpServletResponse response = (HttpServletResponse) FacesUtils.getResponse();
            ExportModule.writeOutContent(response, file, fileName);
            facesContext.responseComplete();
        } catch (Exception e) {
            String msg = "Failed to export file version.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
            FileUtils.deleteQuietly(file);
        }

        return null;
    }

    public String getCurrentNodePath() {
        AProjectArtefact projectArtefact = getSelectedNode().getData();
        return projectArtefact == null ? null
                                       : projectArtefact.getArtefactPath().withoutFirstSegment().getStringValue();
    }

    public String copyFileVersion() {
        String path = FacesUtils.getRequestParameter("copyFileForm:filePath");
        String currentRevision = FacesUtils.getRequestParameter("copyFileForm:currentRevision");
        boolean hasVersions = !repositoryTreeState.isLocalOnly() && getSelectedNode().hasVersions();
        if (StringUtils.isBlank(path)) {
            FacesUtils.addErrorMessage("Path should not be empty");
            return null;
        }
        AProject selectedProject = repositoryTreeState.getSelectedProject();
        ArtefactPath artefactPath = new ArtefactPathImpl(path);
        try {
            selectedProject.getArtefactByPath(artefactPath);
            FacesUtils.addErrorMessage(String.format("File '%s' exists already", path));
            return null;
        } catch (ProjectException ignored) {
            // Artefact by this path shouldn't exist
        }

        InputStream is = null;
        try {
            AProjectFolder folder = selectedProject;
            for (int i = 0; i < artefactPath.segmentCount() - 1; i++) {
                String segment = artefactPath.segment(i);
                if (!folder.hasArtefact(segment)) {
                    folder.addFolder(segment);
                }
                AProjectArtefact artefact = folder.getArtefact(segment);
                if (!artefact.isFolder()) {
                    FacesUtils.addErrorMessage(
                        String.format("Artefact %s is not a folder", artefact.getArtefactPath().getStringValue()));
                    return null;
                }
                folder = (AProjectFolder) folder.getArtefact(segment);
            }

            AProject forExport;
            if (hasVersions && currentRevision == null) {
                forExport = userWorkspace.getDesignTimeRepository().getProject(selectedProject.getName(),
                    new CommonVersionImpl(version));

                TreeNode selectedNode = repositoryTreeState.getSelectedNode();
                ArtefactPath selectedNodePath = selectedNode.getData().getArtefactPath().withoutFirstSegment();
                is = ((AProjectResource) forExport.getArtefactByPath(selectedNodePath)).getContent();
            } else {
                TreeNode selectedNode = repositoryTreeState.getSelectedNode();
                is = ((AProjectResource) userWorkspace.getArtefactByPath(selectedNode.getData().getArtefactPath()))
                    .getContent();
            }

            AProjectResource addedFileResource = folder
                .addResource(artefactPath.segment(artefactPath.segmentCount() - 1), is);
            fileName = addedFileResource.getName();
            repositoryTreeState
                .refreshNode(repositoryTreeState.getProjectNodeByPhysicalName(selectedProject.getName()));
            registerInProjectDescriptor(addedFileResource);
            resetStudioModel();
            is.close();
        } catch (Exception e) {
            String msg = "Failed to export file version.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }

        return null;
    }

    public boolean isHideDeleted() {
        hideDeleted = repositoryTreeState.isHideDeleted();
        return hideDeleted;
    }

    public void setHideDeleted(boolean hideDeleted) {
        this.hideDeleted = hideDeleted;
    }

    public String filter() {
        IFilter<AProjectArtefact> filter = null;
        if (StringUtils.isNotBlank(filterString)) {
            filter = new RepositoryFileExtensionFilter(filterString);
        }
        repositoryTreeState.setFilter(filter);
        repositoryTreeState.setHideDeleted(hideDeleted);
        return null;
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
    public List<TreeNode> getDeploymentProjects() {
        return repositoryTreeState.getDeploymentRepository().getChildNodes();
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFilterString() {
        return filterString;
    }

    public String getFolderName() {
        return null;
    }

    public String getVersionComment() {
        return "";
    }

    public String getNewProjectName() {
        // EPBDS-92 - clear newProject dialog every time
        return null;
    }

    public String getProjectName() {
        // EPBDS-92 - clear newProject dialog every time
        // return null;
        return projectName;
    }

    private ProjectVersion getProjectVersion() {
        AProject project = repositoryTreeState.getSelectedProject();

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

    public String getRevision() {
        ProjectVersion v = getProjectVersion();
        if (v != null) {
            return v.getRevision();
        }
        return "0";
    }

    /**
     * Gets all rules projects from a rule repository.
     *
     * @return list of rules projects
     */
    public List<TreeNode> getRulesProjects() {
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
            openDependenciesIfNeeded();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (ProjectException e) {
            String msg = "Failed to open project.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    private void openDependenciesIfNeeded() throws ProjectException {
        if (openDependencies) {
            for (String dependency : getDependencies(getSelectedProject(), true)) {
                TreeProject projectNode = repositoryTreeState.getProjectNodeByLogicalName(dependency);
                if (projectNode == null) {
                    log.error("Can't find dependency {}", dependency);
                    continue;
                }
                String physicalName = projectNode.getName();
                userWorkspace.getProject(physicalName).open();
            }
        }
    }

    public String openProjectVersion() {
        try {
            if (repositoryTreeState.getSelectedProject().isOpenedForEditing()) {
                repositoryTreeState.getSelectedProject().close();
            }

            repositoryTreeState.getSelectedProject().openVersion(version);
            openDependenciesIfNeeded();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (ProjectException e) {
            String msg = "Failed to open project version.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    public String openProjectVersion(String version) {
        this.version = version;
        openDependencies = true;
        openProjectVersion();

        return null;
    }

    public String refreshTree() {
        repositoryTreeState.invalidateTree();
        resetStudioModel();
        return null;
    }

    public String selectDeploymentProject() {
        String projectName = FacesUtils.getRequestParameter("projectName");
        selectProject(projectName, repositoryTreeState.getDeploymentRepository());
        return null;
    }

    private void selectProject(String projectName, TreeRepository root) {
        for (TreeNode node : root.getChildNodes()) {
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

    public void uploadListener(FileUploadEvent event) {
        ProjectFile file = new ProjectFile(event.getUploadedFile());
        uploadedFiles.add(file);
        String fileName = file.getName();

        setFileName(fileName);

        if (fileName.contains(".")) {
            setProjectName(fileName.substring(0, fileName.lastIndexOf('.')));

            if (FileTypeHelper.isZipFile(fileName)) {
                ProjectDescriptor projectDescriptor = ZipProjectDescriptorExtractor.getProjectDescriptorOrNull(file,
                    zipFilter);
                if (projectDescriptor != null) {
                    setProjectName(projectDescriptor.getName());
                }
            }
        } else {
            setProjectName(fileName);
        }
    }

    public boolean isUploadedFileChanged() {
        ProjectFile lastUploadedFile = getLastUploadedFile();
        if (lastUploadedFile == null || !(repositoryTreeState.getSelectedNode()
            .getData() instanceof AProjectResource)) {
            return false;
        }

        AProjectResource node = (AProjectResource) repositoryTreeState.getSelectedNode().getData();
        String lastUploadedFilePath = lastUploadedFile.getName().replace('\\', '/');
        String lastUploadedFileName = lastUploadedFilePath.substring(lastUploadedFilePath.lastIndexOf('/') + 1);

        return !lastUploadedFileName.equals(node.getName());
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

    public void setVersionComment(String versionComment) {
        try {
            repositoryTreeState.getSelectedNode().getData().setVersionComment(versionComment);
        } catch (PropertyException e) {
            log.error("Failed to set LOB!", e);
            FacesUtils.addErrorMessage("Can not set line of business.", e.getMessage());
        }
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = newProjectName;
    }

    public void setProjectName(String newProjectName) {
        projectName = newProjectName;
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
        AProject project = repositoryTreeState.getSelectedProject();
        if (!project.isDeleted()) {
            FacesUtils.addErrorMessage("Cannot undelete project '" + project.getName() + "'.",
                "Project is not marked for deletion.");
            return null;
        }

        try {
            project.undelete();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (ProjectException e) {
            String msg = "Cannot undelete project '" + project.getName() + "'.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    /**
     * Updates file (active node)
     */
    public String updateFile() {
        String errorMessage = uploadAndUpdateFile();
        if (errorMessage == null) {
            resetStudioModel();
            FacesUtils.addInfoMessage(("File was successfully updated."));
        } else {
            FacesUtils.addErrorMessage(errorMessage, "Error occured during uploading file. " + errorMessage);
        }

        /* Clear the load form */
        clearForm();

        return null;
    }

    public String upload() {
        String errorMessage = uploadProject();
        if (errorMessage == null) {
            try {
                AProject createdProject = userWorkspace.getProject(projectName);
                repositoryTreeState.addRulesProjectToTree(createdProject);
                resetStudioModel();
                FacesUtils.addInfoMessage("Project was created successfully.");
            } catch (ProjectException e) {
                FacesUtils.addErrorMessage(e.getMessage());
            }
        }

        /* Clear the load form */
        clearForm();

        return null;
    }

    public String createProjectWithFiles() {
        String errorMessage = validateProjectName();
        if (errorMessage != null) {
            FacesUtils.addErrorMessage(errorMessage);
        } else if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            FacesUtils.addErrorMessage("There are no uploaded files.");
        } else {
            errorMessage = new ProjectUploader(uploadedFiles, projectName, userWorkspace, zipFilter).uploadProject();
            if (errorMessage != null) {
                FacesUtils.addErrorMessage(errorMessage);
            } else
                try {
                    AProject createdProject = userWorkspace.getProject(projectName);
                    repositoryTreeState.addRulesProjectToTree(createdProject);
                    resetStudioModel();
                    FacesUtils.addInfoMessage("Project was created successfully.");
                } catch (ProjectException e) {
                    FacesUtils.addErrorMessage(e.getMessage());
                }
        }

        /* Clear the load form */
        clearForm();

        return null;
    }

    private void clearForm() {
        this.setFileName(null);
        this.setProjectName(null);
        this.uploadedFiles.clear();
    }

    private XmlProjectDescriptorSerializer xmlProjectDescriptorSerializer = new XmlProjectDescriptorSerializer();

    private String uploadAndAddFile() {
        if (!NameChecker.checkName(fileName)) {
            return "File name '" + fileName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
        }

        try {
            AProjectFolder node = (AProjectFolder) repositoryTreeState.getSelectedNode().getData();

            AProjectResource addedFileResource = node.addResource(fileName, getLastUploadedFile().getInput());

            repositoryTreeState.addNodeToTree(repositoryTreeState.getSelectedNode(), addedFileResource);

            registerInProjectDescriptor(addedFileResource);

            clearUploadedFiles();
        } catch (Exception e) {
            /*
             * If an error is IOException then an error will not be written to
             * the console. This error throw when upload file is exist in the
             * upload folder
             */
            if (!e.getCause().getClass().equals(java.io.IOException.class)) {
                log.error("Error adding file to user workspace.", e);
            }

            return e.getMessage();
        }

        return null;
    }

    private void registerInProjectDescriptor(AProjectResource addedFileResource) {
        if (FileTypeHelper.isExcelFile(fileName)) { // Excel. Add module to
            // rules.xml.
            try {
                UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
                AProjectArtefact projectDescriptorArtifact = selectedProject
                    .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                if (projectDescriptorArtifact instanceof AProjectResource) {
                    AProjectResource resource = (AProjectResource) projectDescriptorArtifact;
                    InputStream content = resource.getContent();
                    ProjectDescriptor projectDescriptor = xmlProjectDescriptorSerializer.deserialize(content);
                    String modulePath = addedFileResource.getArtefactPath().withoutFirstSegment().getStringValue();
                    while (modulePath.charAt(0) == '/') {
                        modulePath = modulePath.substring(1);
                    }
                    Module module = new Module();
                    module.setName(fileName.substring(0, fileName.lastIndexOf('.')));
                    module.setRulesRootPath(new PathEntry(modulePath));
                    projectDescriptor.getModules().add(module);
                    String xmlString = xmlProjectDescriptorSerializer.serialize(projectDescriptor);
                    InputStream newContent = IOUtils.toInputStream(xmlString);
                    resource.setContent(newContent);
                }
            } catch (ProjectException ex) {
                if (log.isDebugEnabled()) {
                    log.debug(ex.getMessage(), ex);
                }
            }
        }
    }

    private String uploadAndUpdateFile() {
        if (getLastUploadedFile() == null) {
            return "There are no uploaded files.";
        }

        try {
            AProjectResource node = (AProjectResource) repositoryTreeState.getSelectedNode().getData();
            node.setContent(getLastUploadedFile().getInput());

            clearUploadedFiles();
        } catch (Exception e) {
            log.error("Error updating file in user workspace.", e);
            return e.getMessage();
        }

        return null;
    }

    private ProjectFile getLastUploadedFile() {
        if (!uploadedFiles.isEmpty()) {
            return uploadedFiles.get(uploadedFiles.size() - 1);
        }
        return null;
    }

    private String uploadProject() {
        String errorMessage;

        if (StringUtils.isNotBlank(projectName)) {
            ProjectFile uploadedItem = getLastUploadedFile();
            if (uploadedItem != null) {
                ProjectUploader projectUploader = new ProjectUploader(uploadedItem,
                    projectName,
                    userWorkspace,
                    zipFilter);
                errorMessage = validateProjectName();
                if (errorMessage == null) {
                    errorMessage = projectUploader.uploadProject();
                }
            } else {
                errorMessage = "There are no uploaded files.";
            }
        } else {
            errorMessage = "Project name must not be empty.";
        }

        if (errorMessage == null) {
            clearUploadedFiles();
        } else {
            FacesUtils.addErrorMessage(errorMessage);
        }

        return errorMessage;
    }

    public void clearUploadedFiles() {
        uploadedFiles.clear();
    }

    public String getNewProjectTemplate() {
        return newProjectTemplate;
    }

    public void setNewProjectTemplate(String newProjectTemplate) {
        this.newProjectTemplate = newProjectTemplate;
    }

    public List<String> getCustomProjectCategories() {
        return customTemplatesResolver.getCategories();
    }

    public List<String> getCustomProjectTemplates(String category) {
        return customTemplatesResolver.getTemplates(category);
    }

    public List<String> getProjectTemplates(String category) {
        return predefinedTemplatesResolver.getTemplates(category);
    }

    public boolean getCanDelete() {
        return isGranted(PRIVILEGE_DELETE_PROJECTS);
    }

    public boolean getCanUnlock() {
        return isGranted(PRIVILEGE_UNLOCK_PROJECTS);
    }

    public boolean getCanUnlockDeployment() {
        return isGranted(PRIVILEGE_UNLOCK_DEPLOYMENT);
    }

    public boolean getCanDeleteDeployment() {
        return isGranted(PRIVILEGE_DELETE_DEPLOYMENT);
    }

    public void setProjectDescriptorResolver(ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    private void resetStudioModel() {
        studio.reset();
    }

    public boolean isOpenDependencies() {
        return openDependencies;
    }

    public void setOpenDependencies(boolean openDependencies) {
        this.openDependencies = openDependencies;
    }

    private AProject getSelectedProject() {
        AProjectArtefact artefact = getSelectedNode().getData();
        if (artefact instanceof AProject) {
            return (AProject) artefact;
        }
        return null;
    }

    public boolean getIsOpenVersionDialogOpened() {
        if (currentProject != getSelectedProject()) {
            currentProject = null;
            version = null;
            return false;
        }
        return currentProject != null;
    }

    public void openVersionDialogListener(AjaxBehaviorEvent event) {
        currentProject = getSelectedProject();
        version = currentProject.getVersion().getVersionName();
    }

    public void deleteRulesProjectListener(AjaxBehaviorEvent event) {
        String projectName = FacesUtils.getRequestParameter("projectName");

        try {
            activeProjectNode = repositoryTreeState.getRulesRepository()
                .getChild(RepositoryUtils.getTreeNodeId(projectName));
        } catch (Exception e) {
            log.error("Cannot delete rules project '{}'.", projectName, e);
            FacesUtils.addErrorMessage("Failed to delete rules project.", e.getMessage());
        }
    }

    public TreeNode getSelectedNode() {
        TreeNode selectedNode = repositoryTreeState.getSelectedNode();
        return activeProjectNode != null && selectedNode instanceof TreeRepository ? activeProjectNode : selectedNode;
    }
}
