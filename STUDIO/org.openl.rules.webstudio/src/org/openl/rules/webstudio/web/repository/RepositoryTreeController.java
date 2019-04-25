package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.*;
import static org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.filter.RepositoryFileExtensionFilter;
import org.openl.rules.webstudio.util.ExportFile;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.project.*;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.repository.upload.ProjectDescriptorUtils;
import org.openl.rules.webstudio.web.repository.upload.ProjectUploader;
import org.openl.rules.webstudio.web.repository.upload.ZipProjectDescriptorExtractor;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipFromProjectFile;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.impl.ProjectExportHelper;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.StreamException;

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

    @ManagedProperty(value = "#{projectDescriptorSerializerFactory}")
    private ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory;

    @ManagedProperty(value = "#{zipCharsetDetector}")
    private ZipCharsetDetector zipCharsetDetector;

    @ManagedProperty(value = "#{systemConfig}")
    private Map<String, Object> config;

    @ManagedProperty(value = "#{designRepositoryComments}")
    private Comments designRepoComments;

    @ManagedProperty(value = "#{deployConfigRepositoryComments}")
    private Comments deployConfigRepoComments;

    private WebStudio studio = WebStudioUtils.getWebStudio(true);

    private String projectName;
    private String projectFolder = "";
    private String newProjectTemplate;
    private String folderName;
    private List<ProjectFile> uploadedFiles = new ArrayList<>();
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

    private boolean projectUseCustomComment;
    private CommentValidator designCommentValidator;
    private CommentValidator deployConfigCommentValidator;

    private String createProjectComment;
    private String archiveProjectComment;
    private String restoreProjectComment;
    private String eraseProjectComment;

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
            AProjectFolder folder = (AProjectFolder) projectArtefact;
            if (folderName != null && !folderName.isEmpty()) {
                if (NameChecker.checkName(folderName)) {
                    if (!NameChecker.checkIsFolderPresent(folder, folderName)) {
                        try {
                            AProjectFolder addedFolder = folder.addFolder(folderName);
                            repositoryTreeState.addNodeToTree(repositoryTreeState.getSelectedNode(), addedFolder);
                            resetStudioModel();
                        } catch (Exception e) {
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
            if (!project.isModified()) {
                log.warn("Tried to save a project without any changes.");
                return null;
            }
            if (project instanceof RulesProject) {
                studio.saveProject((RulesProject) project);
            } else {
                project.save();
            }

            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (Exception e) {
            String msg = e.getMessage();
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg);
        }
        return null;
    }

    public String closeProject() {
        try {
            UserWorkspaceProject repositoryProject = repositoryTreeState.getSelectedProject();
            closeProjectAndReleaseResources(repositoryProject);
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
        List<String> projects = new ArrayList<>();
        TreeNode selectedNode = getSelectedNode();
        TreeProject projectNode = selectedNode instanceof TreeProject ? (TreeProject) selectedNode : null;
        if (projectNode != null) {
            String name = projectNode.getName();

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
        if (!isCurrentProjectSelected()) {
            return false;
        }
        if (version == null) {
            return false;
        }
        try {
            AProject selectedProject = repositoryTreeState.getSelectedProject();
            if (selectedProject instanceof ADeploymentProject) {
                return false;
            }
            AProject newVersion = userWorkspace.getDesignTimeRepository().getProject(selectedProject.getName(),
                new CommonVersionImpl(version));
            return !getDependencies(newVersion, false).isEmpty();
        } catch (Exception e) {
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
        List<String> dependencies = new ArrayList<>(getDependencies(getSelectedProject(), true));
        Collections.sort(dependencies);
        return dependencies;
    }

    public Collection<String> getDependenciesForVersion() {
        if (version == null || !isCurrentProjectSelected()) {
            // Because ui:repeat ignores the "rendered" property, here is a
            // workaround to reduce performance drop.
            return Collections.emptyList();
        }
        try {
            AProject selectedProject = repositoryTreeState.getSelectedProject();
            AProject newVersion = userWorkspace.getDesignTimeRepository().getProject(selectedProject.getName(),
                new CommonVersionImpl(version));
            List<String> dependencies = new ArrayList<>(getDependencies(newVersion, true));
            Collections.sort(dependencies);
            return dependencies;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Collection<String> getDependencies(AProject project, boolean recursive) {
        Collection<String> dependencies = new HashSet<>();
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
                TreeProject projectNode = repositoryTreeState.getProjectNodeByPhysicalName(dependency.getName());
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
        } catch (Exception e) {
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
            String comment = deployConfigRepoComments.copiedFrom(project.getName());
            userWorkspace.copyDDProject(project, newProjectName, comment);
            ADeploymentProject newProject = userWorkspace.getDDProject(newProjectName);
            repositoryTreeState.addDeploymentProjectToTree(newProject);
        } catch (Exception e) {
            String msg = "Failed to copy deployment project.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }

        return null;
    }

    public String createDeploymentConfiguration() {
        try {
            if (StringUtils.isBlank(projectName)) {
                FacesUtils.addErrorMessage("Deploy Configuration name must not be empty.");
                return null;
            }
            if (!NameChecker.checkName(projectName)) {
                FacesUtils.addErrorMessage("Specified name is not a valid Deploy Configuration name." + " " + NameChecker.BAD_NAME_MSG);
                return null;
            }
            if (userWorkspace.hasDDProject(projectName)) {
                String msg = "Cannot create configuration because configuration with such name already exists.";
                FacesUtils.addErrorMessage(msg, null);

                return null;
            }

            ADeploymentProject createdProject = userWorkspace.createDDProject(projectName);
            createdProject.open();
            // Analogous to rules project creation (to change "created by"
            // property and revision)
            String comment = deployConfigRepoComments.createProject(projectName);
            
            createdProject.getFileData().setComment(comment);
            createdProject.save();
            createdProject.open();
            repositoryTreeState.addDeploymentProjectToTree(createdProject);
            FacesUtils.addInfoMessage(String.format("Deploy configuration '%s' is successfully created", projectName));
        } catch (Exception e) {
            String msg = "Failed to create deploy configuration '" + projectName + "'.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }

        /* Clear the load form */
        this.clearForm();

        return null;
    }

    public String createNewRulesProject() {
        String comment;
        if (StringUtils.isNotBlank(createProjectComment)) {
            comment = createProjectComment;
        } else {
            comment = designRepoComments.createProject(projectName);
        }
        String msg = validateCreateProjectParams(comment);

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

        ExcelFilesProjectCreator projectCreator = new ExcelFilesProjectCreator(projectName, projectFolder, userWorkspace,
            comment,
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
                this.openProject();
            } catch (Exception e) {
                creationMessage = e.getMessage();
            }
        } else {
            FacesUtils.addErrorMessage(creationMessage);
        }

        return creationMessage;
    }

    private String validateCreateProjectParams(String comment) {
        String msg = validateProjectName();
        if (msg != null) {
            return msg;
        }

        msg = validateCreateProjectComment(comment);
        if (msg != null) {
            return msg;
        }

        return msg;
    }

    private String validateProjectName() {
        try {
            String msg = null;
            if (StringUtils.isBlank(projectName)) {
                msg = "Project name must not be empty.";
            } else if (!NameChecker.checkName(projectName)) {
                msg = "Specified name is not a valid project name." + " " + NameChecker.BAD_NAME_MSG;
            } else if (userWorkspace
                .hasProject(projectName)) {
                msg = "Cannot create project because project with such name already exists.";
            }
            return msg;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Internal error: " + e.getMessage();
        }
    }

    private String validateCreateProjectComment(String comment) {
        try {
            if (designCommentValidator.isValidationEnabled()) {
                designCommentValidator.validate(comment);
            }
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /*
     * Because of renaming 'Deployment project' to 'Deploy Configuration' the
     * method was renamed too.
     */
    public String deleteDeploymentConfiguration() {
        String projectName = FacesUtils.getRequestParameter("deploymentProjectName");

        try {
            ADeploymentProject project = userWorkspace.getDDProject(projectName);
            // projectInTree must be initialized before project was deleted
            TreeNode projectInTree = repositoryTreeState.getDeploymentRepository()
                    .getChild(RepositoryUtils.getTreeNodeId(project.getName()));
            String comment = deployConfigRepoComments.archiveProject(project.getName());
            project.delete(userWorkspace.getUser(), comment);
            if (repositoryTreeState.isHideDeleted()) {
                repositoryTreeState.deleteNode(projectInTree);
            }

            FacesUtils.addInfoMessage("Deploy configuration was deleted successfully.");
        } catch (Exception e) {
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
        Collection<String> modulePaths = new HashSet<>();
        findModulePaths(aProjectArtefact, modulePaths);
        if (projectDescriptorArtifact instanceof AProjectResource) {
            IProjectDescriptorSerializer serializer = projectDescriptorSerializerFactory.getSerializer(selectedProject);

            String projectDescriptorPath = projectDescriptorArtifact.getArtefactPath().withoutFirstSegment() .getStringValue();
            if (projectDescriptorPath.equals(aProjectArtefact.getArtefactPath().withoutFirstSegment().getStringValue())) {
                // There is no need to unregister itself
                return;
            }

            AProjectResource resource = (AProjectResource) projectDescriptorArtifact;
            InputStream content = resource.getContent();
            ProjectDescriptor projectDescriptor;
            try {
                projectDescriptor = serializer.deserialize(content);
            } catch (StreamException e) {
                log.error("Broken rules.xml file. Can't remove modules from it", e);
                return;
            } finally {
                IOUtils.closeQuietly(content);
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
            String xmlString = serializer.serialize(projectDescriptor);
            InputStream newContent = IOUtils.toInputStream(xmlString);
            resource.setContent(newContent);
        }
    }

    public String deleteElement() {
        AProjectFolder projectArtefact = (AProjectFolder) repositoryTreeState.getSelectedNode().getData();
        String childName = FacesUtils.getRequestParameter("element");
        AProjectArtefact childArtefact = ((TreeNode) repositoryTreeState.getSelectedNode()
                .getChild(RepositoryUtils.getTreeNodeId(childName))).getData();

        try {
            studio.getModel().clearModuleInfo(); // Release resources like jars
            unregisterArtifactInProjectDescriptor(childArtefact);
            childArtefact.delete();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();

            FacesUtils.addInfoMessage("Element was deleted successfully.");
        } catch (Exception e) {
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
            if (projectArtefact instanceof RulesProject) {
                if (projectArtefact.isLocked() && !((RulesProject) projectArtefact).isLockedByMe()) {
                    FacesUtils.addErrorMessage("Project is locked by other user. Can't archive it.");
                    return null;
                }
                File workspacesRoot = userWorkspace.getLocalWorkspace().getLocation().getParentFile();
                String branch = ((RulesProject) projectArtefact).getBranch();
                closeProjectForAllUsers(workspacesRoot, selectedNode.getName(), branch);
            }
            if (projectArtefact instanceof UserWorkspaceProject) {
                UserWorkspaceProject project = (UserWorkspaceProject) projectArtefact;

                String comment;
                if (project instanceof RulesProject && isUseCustomComment()) {
                    comment = archiveProjectComment;
                    if (!isValidComment(project, comment)) {
                        return null;
                    }
                } else {
                    Comments comments = getComments(project);
                    comment = comments.archiveProject(project.getName());
                }
                project.delete(comment);
            } else {
                projectArtefact.delete();
            }
            TreeNode parent = selectedNode.getParent();
            if (parent != null && parent.getData() != null ){
                parent.refresh();
            }
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
                } else {
                    repositoryTreeState.refreshSelectedNode();
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
        } catch (Exception e) {
            log.error("Failed to delete node.", e);
            FacesUtils.addErrorMessage("Failed to delete node.", e.getMessage());
        }

        return null;
    }

    private boolean isValidComment(UserWorkspaceProject project, String comment) {
        CommentValidator commentValidator = project instanceof RulesProject ?
                                            designCommentValidator :
                                            deployConfigCommentValidator;

        if (commentValidator.isValidationEnabled()) {
            try {
                commentValidator.validate(comment);
            } catch (Exception e) {
                FacesUtils.addErrorMessage(e.getMessage());
                return false;
            }
        }
        return true;
    }

    public String unlockSelectedProject() {
        return unlockNode("Project");
    }

    public String unlockSelectedDeployConfiguration() {
        return unlockNode("Deploy configuration");
    }

    private String unlockNode(String nodeTypeName) {
        TreeNode selectedNode = repositoryTreeState.getSelectedNode();
        AProjectArtefact projectArtefact = selectedNode.getData();
        if (projectArtefact == null) {
            FacesUtils.addInfoMessage(nodeTypeName + " was deleted already.");
            return null;
        }
        try {
            projectArtefact.unlock();
            repositoryTreeState.refreshSelectedNode();
            if (projectArtefact instanceof RulesProject) {
                File workspacesRoot = userWorkspace.getLocalWorkspace().getLocation().getParentFile();
                String branch = ((RulesProject) projectArtefact).getBranch();
                closeProjectForAllUsers(workspacesRoot, projectArtefact.getName(), branch);
            }
            resetStudioModel();

            FacesUtils.addInfoMessage(nodeTypeName + " was unlocked successfully.");
        } catch (Exception e) {
            log.error("Failed to unlock node.", e);
            FacesUtils.addErrorMessage("Failed to unlock node.", e.getMessage());
        }

        return null;
    }

    public String unlockProject() {
        String projectName = FacesUtils.getRequestParameter("projectName");

        try {
            RulesProject project = userWorkspace.getProject(projectName);
            if (project == null) {
                // It was deleted by other user
                return null;
            }
            project.unlock();
            File workspacesRoot = userWorkspace.getLocalWorkspace().getLocation().getParentFile();
            closeProjectForAllUsers(workspacesRoot, projectName, project.getBranch());
            resetStudioModel();
        } catch (Exception e) {
            log.error("Cannot unlock rules project '{}'.", projectName, e);
            FacesUtils.addErrorMessage("Failed to unlock rules project.", e.getMessage());
        }
        return null;
    }

    /**
     * Closes unlocked project for all users. All unsaved changes will be lost.
     */
    private void closeProjectForAllUsers(File workspacesRoot, String projectName, String branch) throws ProjectException {
        // Needed to update UI of current user
        TreeProject projectNode = repositoryTreeState.getProjectNodeByPhysicalName(projectName);
        if (projectNode != null) {
            AProjectArtefact artefact = projectNode.getData();
            if (artefact instanceof RulesProject) {
                ((RulesProject) artefact).close();
            }
        }

        // List all folders. Those folders - workspaces for each user (except for reserved .locks folder)
        File[] files = workspacesRoot.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String userName = file.getName();
                    // Check for reserved folder name
                    if (!LockEngineImpl.LOCKS_FOLDER_NAME.equals(userName)) {
                        try (LocalRepository repository = new LocalRepository(file)) {
                            repository.initialize();

                            FileData savedData = repository.getProjectState(projectName).getFileData();
                            String savedBranch = savedData == null ? null : savedData.getBranch();

                            if (branch == null && savedBranch == null || branch != null && branch.equals(savedBranch)) {
                                FileData fileData = new FileData();
                                fileData.setName(projectName);
                                if (!repository.delete(fileData)) {
                                    if (repository.check(fileData.getName()) != null) {
                                        log.warn("Can't close project because resource '" + fileData.getName() + "' is used");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Log exception and skip current user
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    public String unlockDeploymentConfiguration() {
        String deploymentProjectName = FacesUtils.getRequestParameter("deploymentProjectName");

        try {
            ADeploymentProject deploymentProject = userWorkspace.getDDProject(deploymentProjectName);
            if (deploymentProject == null) {
                // It was deleted by other user
                return null;
            }
            deploymentProject.unlock();
            resetStudioModel();
        } catch (Exception e) {
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
                Repository mainRepo = userWorkspace.getDesignTimeRepository().getRepository();
                if (project instanceof RulesProject && mainRepo.supports().branches() && !((BranchRepository) mainRepo).getBranch().equals(project.getBranch())) {
                    // Delete secondary branch
                    ((BranchRepository) mainRepo).deleteBranch(null, project.getBranch());
                } else {
                    String comment;
                    if (project instanceof RulesProject && isUseCustomComment()) {
                        comment = eraseProjectComment;
                        if (!isValidComment(project, comment)) {
                            return null;
                        }
                    } else {
                        Comments comments = getComments(project);
                        comment = comments.eraseProject(project.getName());
                    }
                    project.erase(userWorkspace.getUser(), comment);
                }
            }
            deleteProjectHistory(project.getName());
            userWorkspace.refresh();

            repositoryTreeState.deleteSelectedNodeFromTree();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();

            resetStudioModel();
            FacesUtils.addInfoMessage("Project was erased successfully.");
        } catch (Exception e) {
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
                .getStringProperty(PROJECT_HISTORY_HOME) + File.separator + projectName;
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
        } catch (Exception e) {
            String msg = "Failed to export project version.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }

        if (zipFile != null) {
            final FacesContext facesContext = FacesUtils.getFacesContext();
            HttpServletResponse response = (HttpServletResponse) FacesUtils.getResponse();
            ExportFile.writeOutContent(response, zipFile, zipFileName);
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
            ExportFile.writeOutContent(response, file, fileName);
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
        } catch (Exception ignored) {
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
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        Comments comments = getComments(project);

        if (project != null && project.isOpenedOtherVersion()) {
            return comments.restoredFrom(project.getHistoryVersion());
        }


        return comments.saveProject(project == null ? "" : project.getName());
    }

    private Comments getComments(UserWorkspaceProject project) {
        return project instanceof ADeploymentProject ? deployConfigRepoComments : designRepoComments;
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

    public String getProjectFolder() {
        return projectFolder;
    }

    private ProjectVersion getProjectVersion() {
        AProject project = repositoryTreeState.getSelectedProject();

        if (project != null) {
            return project.getVersion();
        }
        return null;
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

        List<SelectItem> selectItems = new ArrayList<>();
        for (ProjectVersion version : versions) {
            if (!version.isDeleted()) {
                selectItems.add(new SelectItem(version.getVersionName()));
            }
        }
        return selectItems.toArray(new SelectItem[0]);
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
        } catch (Exception e) {
            String msg = "Failed to open project.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    private void openDependenciesIfNeeded() throws ProjectException {
        if (openDependencies) {
            for (String dependency : getDependencies(getSelectedProject(), true)) {
                TreeProject projectNode = repositoryTreeState.getProjectNodeByPhysicalName(dependency);
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
            UserWorkspaceProject repositoryProject = repositoryTreeState.getSelectedProject();

            if (repositoryProject.isOpened()) {
                studio.getModel().clearModuleInfo();
                repositoryProject.releaseMyLock();
            }

            repositoryProject.openVersion(version);
            openDependenciesIfNeeded();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (Exception e) {
            String msg = "Failed to open project version.";
            log.error(msg, e);
            FacesUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    private void closeProjectAndReleaseResources(UserWorkspaceProject repositoryProject) throws ProjectException {
        // We must release module info because it can hold jars.
        // We can't rely on studio.getProject() to determine if closing project is compiled inside studio.getModel()
        // because project could be changed or cleared before (See studio.reset() usages). Also that project can be
        // a dependency of other. That's why we must always clear moduleInfo when closing a project.
        studio.getModel().clearModuleInfo();
        repositoryProject.close();
    }

    public String openProjectVersion(String version) {
        this.version = version;
        openDependencies = true;
        openProjectVersion();

        return null;
    }

    public String refreshTree() {
        repositoryTreeState.invalidateTree();
        repositoryTreeState.invalidateSelection();
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
                Charset charset = zipCharsetDetector.detectCharset(new ZipFromProjectFile(file));

                if (charset == null) {
                    log.warn("Can't detect a charset for the zip file");
                    charset = StandardCharsets.UTF_8;
                }

                ProjectDescriptor projectDescriptor = ZipProjectDescriptorExtractor.getProjectDescriptorOrNull(file, zipFilter, charset);
                if (projectDescriptor != null) {
                    setProjectName(projectDescriptor.getName());
                    setCreateProjectComment(designRepoComments.createProject(projectDescriptor.getName()));
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
        this.fileName = StringUtils.trim(fileName);
    }

    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    public void setFolderName(String folderName) {
        this.folderName = StringUtils.trim(folderName);
    }

    public void setVersionComment(String versionComment) {
        FileData fileData = repositoryTreeState.getSelectedNode().getData().getFileData();
        if (fileData != null) {
            fileData.setComment(versionComment);
        }
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = StringUtils.trim(newProjectName);
    }

    public void setProjectName(String newProjectName) {
        projectName = StringUtils.trim(newProjectName);
    }

    public void setProjectFolder(String projectFolder) {
        String folder = StringUtils.trimToEmpty(projectFolder).replace('\\', '/');
        if (!folder.isEmpty() && !folder.endsWith("/")) {
            folder += '/';
        }
        this.projectFolder = folder;
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
            FacesUtils.addErrorMessage("Cannot undelete project '" + project.getName() + "'.",
                "Project is not marked for deletion.");
            return null;
        }

        try {
            String comment;
            if (project instanceof RulesProject && isUseCustomComment()) {
                comment = restoreProjectComment;
                if (!isValidComment(project, comment)) {
                    return null;
                }
            } else {
                Comments comments = getComments(project);
                comment = comments.restoreProject(project.getName());
            }
            project.undelete(userWorkspace.getUser(), comment);
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (Exception e) {
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
            } catch (Exception e) {
                FacesUtils.addErrorMessage(e.getMessage());
            }
        }

        /* Clear the load form */
        clearForm();

        return null;
    }

    public String createProjectWithFiles() {
        String comment;
        if (StringUtils.isNotBlank(createProjectComment)) {
            comment = createProjectComment;
        } else {
            comment = designRepoComments.createProject(projectName);
        }
        String errorMessage = validateCreateProjectParams(comment);
        if (errorMessage != null) {
            FacesUtils.addErrorMessage(errorMessage);
        } else if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            FacesUtils.addErrorMessage("There are no uploaded files.");
        } else {
            errorMessage = new ProjectUploader(uploadedFiles,
                    projectName,
                    projectFolder,
                    userWorkspace,
                    comment,
                    zipFilter,
                    zipCharsetDetector
            ).uploadProject();
            if (errorMessage != null) {
                FacesUtils.addErrorMessage(errorMessage);
            } else
                try {
                    AProject createdProject = userWorkspace.getProject(projectName);
                    repositoryTreeState.addRulesProjectToTree(createdProject);
                    resetStudioModel();
                    FacesUtils.addInfoMessage("Project was created successfully.");
                } catch (Exception e) {
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
        this.setProjectFolder("");
        this.setCreateProjectComment(null);
        this.uploadedFiles.clear();
    }

    private String uploadAndAddFile() {
        if (!NameChecker.checkName(fileName)) {
            return "File name '" + fileName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
        }

        try {
            AProjectFolder node = (AProjectFolder) repositoryTreeState.getSelectedNode().getData();

            ProjectFile lastUploadedFile = getLastUploadedFile();
            if (lastUploadedFile == null) {
                return "Please upload the file";
            }
            AProjectResource addedFileResource = node.addResource(fileName, lastUploadedFile.getInput());

            repositoryTreeState.addNodeToTree(repositoryTreeState.getSelectedNode(), addedFileResource);

            registerInProjectDescriptor(addedFileResource);

            clearUploadedFiles();
        } catch (Exception e) {
            /*
             * If an error is IOException then an error will not be written to
             * the console. This error throw when upload file is exist in the
             * upload folder
             */
            if (e.getCause() == null || !e.getCause().getClass().equals(java.io.IOException.class)) {
                log.error("Error adding file to user workspace.", e);
            }

            return e.getMessage();
        }

        return null;
    }

    private void registerInProjectDescriptor(AProjectResource addedFileResource) {
        if (FileTypeHelper.isExcelFile(fileName)) { // Excel. Add module to
            // rules.xml.
            InputStream content = null;
            try {
                UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
                AProjectArtefact projectDescriptorArtifact = selectedProject
                    .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                if (projectDescriptorArtifact instanceof AProjectResource) {
                    IProjectDescriptorSerializer serializer = projectDescriptorSerializerFactory.getSerializer(selectedProject);

                    AProjectResource resource = (AProjectResource) projectDescriptorArtifact;
                    content = resource.getContent();
                    ProjectDescriptor projectDescriptor = serializer.deserialize(content);
                    String modulePath = addedFileResource.getArtefactPath().withoutFirstSegment().getStringValue();
                    while (modulePath.charAt(0) == '/') {
                        modulePath = modulePath.substring(1);
                    }
                    Module module = new Module();
                    module.setName(fileName.substring(0, fileName.lastIndexOf('.')));
                    module.setRulesRootPath(new PathEntry(modulePath));
                    projectDescriptor.getModules().add(module);
                    String xmlString = serializer.serialize(projectDescriptor);
                    InputStream newContent = IOUtils.toInputStream(xmlString);
                    resource.setContent(newContent);
                }
            } catch (ProjectException ex) {
                if (log.isDebugEnabled()) {
                    log.debug(ex.getMessage(), ex);
                }
            } finally {
                IOUtils.closeQuietly(content);
            }
        }
    }

    private String uploadAndUpdateFile() {
        ProjectFile lastUploadedFile = getLastUploadedFile();
        if (lastUploadedFile == null) {
            return "There are no uploaded files.";
        }

        try {
            AProjectResource node = (AProjectResource) repositoryTreeState.getSelectedNode().getData();
            node.setContent(lastUploadedFile.getInput());

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
                String comment;
                if (StringUtils.isNotBlank(createProjectComment)) {
                    comment = createProjectComment;
                } else {
                    comment = designRepoComments.createProject(projectName);
                }

                ProjectUploader projectUploader = new ProjectUploader(uploadedItem,
                    projectName, projectFolder, userWorkspace,
                    comment,
                    zipFilter,
                    zipCharsetDetector);
                errorMessage = validateCreateProjectParams(comment);
                if (errorMessage == null) {
                    errorMessage = projectUploader.uploadProject();
                }
            } else {
                errorMessage = "There are no uploaded files.";
            }
        } else {
            errorMessage = "Project name must not be empty.";
        }

        try {
            if (errorMessage != null) {
                final ProjectFile lastUploadedFile = getLastUploadedFile();
                if (lastUploadedFile != null) {
                    Charset charset = zipCharsetDetector.detectCharset(new ZipFromProjectFile(lastUploadedFile));

                    if (charset == null) {
                        errorMessage = "Can't detect a charset for the zip file";
                        throw new IllegalArgumentException(errorMessage);
                    }

                    ZipProjectDescriptorExtractor.getProjectDescriptorOrThrow(lastUploadedFile, zipFilter, charset);
                }
            }

        } catch (XStreamException e) {
            // Add warning that uploaded project contains incorrect rules.xml
            FacesUtils.addWarnMessage("Warning: " + ProjectDescriptorUtils.getErrorMessage(e));
        } catch (Exception ignored) {
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
        return isGranted(DELETE_PROJECTS);
    }

    public boolean getCanUnlock() {
        return isGranted(UNLOCK_PROJECTS);
    }

    public boolean getCanUnlockDeployment() {
        return isGranted(UNLOCK_DEPLOYMENT);
    }

    public boolean getCanDeleteDeployment() {
        return isGranted(DELETE_DEPLOYMENT);
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

    /**
     * Determine show or not current project content is some page (Open Version dialog or Rules Deploy Configuration tab).
     *
     * @return false if selected project is changed or true if {@link #selectCurrentProjectForOpen(AjaxBehaviorEvent)} is invoked
     */
    public boolean isCurrentProjectSelected() {
        if (currentProject != getSelectedProject()) {
            currentProject = null;
            version = null;
            return false;
        }
        return currentProject != null;
    }

    /**
     * Mark (select) current project for open in some page (Open Version dialog or Rules Deploy Configuration tab)
     */
    public void selectCurrentProjectForOpen(AjaxBehaviorEvent event) {
        currentProject = getSelectedProject();
        if (currentProject == null || currentProject.getVersion() == null || currentProject.isLastVersion()) {
            version = null;
        } else {
            version = currentProject.getVersion().getVersionName();
        }
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

    /**
     * Checks if design repository supports branches
     */
    public boolean isSupportsBranches() {
        try {
            return userWorkspace.getDesignTimeRepository().getRepository().supports().branches();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean isSupportsMappedFolders() {
        try {
            return userWorkspace.getDesignTimeRepository().getRepository().supports().mappedFolders();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public String getProjectBranch() {
        try {
            return repositoryTreeState.getSelectedProject().getBranch();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public void setProjectBranch(String branch) {
        try {
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            String previousBranch = selectedProject.getBranch();
            if (branch == null || branch.equals(previousBranch)) {
                return;
            }

            boolean opened = selectedProject.isOpened();
            if (opened) {
                studio.getModel().clearModuleInfo();
                selectedProject.releaseMyLock();
            }

            selectedProject.setBranch(branch);
            if (selectedProject.getVersion() == null) {
                //move back to previous branch! Because the project is not present in new branch
                selectedProject.setBranch(previousBranch);
                log.warn("Current project does not exists in '{}' branch! Project branch was switched to the previous one", branch);
            }

            if (opened) {
                // Update files
                selectedProject.open();
            }

            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
            version = null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void validateProjectForBranch(FacesContext context, UIComponent toValidate, Object value) {
        UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
        if (!isSupportsBranches() || !(selectedProject instanceof RulesProject)) {
            return;
        }
        String branch = (String) value;
        BranchRepository repository = (BranchRepository) selectedProject.getDesignRepository();
        try {
            boolean exists = !repository.forBranch(branch)
                    .listHistory(((RulesProject) selectedProject).getDesignFolderName()).isEmpty();
            FacesUtils.validate(exists, "Current project does not exist in this branch!");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public List<SelectItem> getProjectBranches() {
        try {
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();

            Collection<String> branches = ((BranchRepository) userWorkspace.getDesignTimeRepository()
                    .getRepository()).getBranches(selectedProject.getName());

            return Arrays.asList(FacesUtils.createSelectItems(branches));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public TreeNode getSelectedNode() {
        TreeNode selectedNode = repositoryTreeState.getSelectedNode();
        return activeProjectNode != null && selectedNode instanceof TreeRepository ? activeProjectNode : selectedNode;
    }

    public boolean isRenamed(RulesProject project) {
        return project != null && !getLogicalName(project).equals(project.getName());
    }

    public String getLogicalName(RulesProject project) {
        return project == null ? null : projectDescriptorResolver.getLogicalName(project);
    }

    public void commentValidator(FacesContext context, UIComponent toValidate, Object value) {
        String comment = (String) value;

        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (project instanceof RulesProject) {
            designCommentValidator.validate(comment);
        } else if (project instanceof ADeploymentProject) {
            deployConfigCommentValidator.validate(comment);
        }
    }

    public String getProjectReference(AProjectArtefact artefact, ProjectVersion version) {
        if (artefact instanceof RulesProject) {
            String comment = version.getVersionComment();
            String name = designRepoComments.parseSourceOfCopy(comment);
            if (repositoryTreeState.getProjectNodeByPhysicalName(name) == null) {
                return "";
            }
            return name;
        }

        return "";
    }
    
    public boolean isUseCustomComment() {
        // Only projects are supported for now. Deploy configs can be supported in future.
        return repositoryTreeState.getSelectedProject() != null ? projectUseCustomComment && !repositoryTreeState.getSelectedProject().isLocalOnly() : projectUseCustomComment; 
    }

    public String getCreateProjectComment() {
        return createProjectComment;
    }

    public String retrieveCreateProjectCommentTemplate() {
        return projectUseCustomComment ? designRepoComments.getCreateProjectTemplate() : null;
    }

    public void setCreateProjectComment(String createProjectComment) {
        this.createProjectComment = createProjectComment;
    }

    public String getArchiveProjectComment() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (project == null) {
            project = repositoryTreeState.getProject(getSelectedNode());
        }
        Comments comments = getComments(project);
        return comments.archiveProject(project.getName());
    }

    public void setArchiveProjectComment(String archiveProjectComment) {
        this.archiveProjectComment = archiveProjectComment;
    }

    public String getRestoreProjectComment() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        Comments comments = getComments(project);
        return comments.restoreProject(project.getName());
    }

    public void setRestoreProjectComment(String restoreProjectComment) {
        this.restoreProjectComment = restoreProjectComment;
    }

    public String getEraseProjectComment() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        Comments comments = getComments(project);
        return comments.eraseProject(project.getName());
    }

    public void setEraseProjectComment(String eraseProjectComment) {
        this.eraseProjectComment = eraseProjectComment;
    }

    public void setProjectDescriptorSerializerFactory(ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory) {
        this.projectDescriptorSerializerFactory = projectDescriptorSerializerFactory;
    }

    public void setZipCharsetDetector(ZipCharsetDetector zipCharsetDetector) {
        this.zipCharsetDetector = zipCharsetDetector;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;

        projectUseCustomComment = Boolean.parseBoolean(config.get("design-repository.comment-template.use-custom-comments").toString());

        designCommentValidator = CommentValidator.forDesignRepo(config);

        boolean separateDeployConfigRepo = Boolean.parseBoolean(config.get(USE_SEPARATE_DEPLOY_CONFIG_REPO).toString());

        if (separateDeployConfigRepo) {
            deployConfigCommentValidator = CommentValidator.forDeployConfigRepo(config);
        } else {
            deployConfigCommentValidator = designCommentValidator;
        }
    }

    public void setDesignRepoComments(Comments designRepoComments) {
        this.designRepoComments = designRepoComments;
    }

    public void setDeployConfigRepoComments(Comments deployConfigRepoComments) {
        this.deployConfigRepoComments = deployConfigRepoComments;
    }
}
