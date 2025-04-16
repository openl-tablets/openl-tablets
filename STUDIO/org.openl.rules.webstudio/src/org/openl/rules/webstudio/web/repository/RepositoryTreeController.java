package org.openl.rules.webstudio.web.repository;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.UNLOCK_DEPLOYMENT;
import static org.openl.rules.security.Privileges.UNLOCK_PROJECTS;
import static org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.rest.ProjectHistoryService;
import org.openl.rules.security.Privileges;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.util.ExportFile;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.admin.FolderStructureValidators;
import org.openl.rules.webstudio.web.admin.ProjectTagsBean;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.repository.cache.ProjectVersionCacheManager;
import org.openl.rules.webstudio.web.repository.event.ProjectDeletedEvent;
import org.openl.rules.webstudio.web.repository.merge.ConflictUtils;
import org.openl.rules.webstudio.web.repository.merge.MergeConflictInfo;
import org.openl.rules.webstudio.web.repository.project.CustomTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.PredefinedTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.project.TemplatesResolver;
import org.openl.rules.webstudio.web.repository.tree.TreeDProject;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProductProject;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.repository.tree.TreeProjectGrouping;
import org.openl.rules.webstudio.web.repository.tree.TreeRepository;
import org.openl.rules.webstudio.web.repository.upload.ProjectUploader;
import org.openl.rules.webstudio.web.repository.upload.ZipProjectDescriptorExtractor;
import org.openl.rules.webstudio.web.repository.upload.zip.ProjectDescriptionException;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipFromProjectFile;
import org.openl.rules.webstudio.web.util.OpenAPIEditorService;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.webstudio.web.util.Utils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.impl.ProjectExportHelper;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.permission.AclPermissionsSets;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

/**
 * Repository tree controller. Used for retrieving data for repository tree and performing repository actions.
 *
 * @author Aleh Bykhavets
 * @author Andrey Naumenko
 */
@Service
@ViewScope
public class RepositoryTreeController {

    private static final String CUSTOM_TEMPLATE_TYPE = "custom";
    private static final String OPENED_OTHER_PROJECT = "OpenL Studio cannot open two projects with the same name. Close the currently opened project and try again.";
    private static final String NONE_REPO = "none";
    public static final String OPENAPI_DEFAULT_DATA_MODULE_PATH = "openapi.default.data.module.path";
    public static final String OPENAPI_DEFAULT_ALGORITHM_MODULE_PATH = "openapi.default.algorithm.module.path";

    private final Logger log = LoggerFactory.getLogger(RepositoryTreeController.class);

    @Autowired
    private RepositoryTreeState repositoryTreeState;

    @Autowired
    private ProjectTagsBean projectTagsBean;

    @Autowired
    private MultiUserWorkspaceManager workspaceManager;

    private volatile UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());

    @Autowired
    @Qualifier("zipFilter")
    private PathFilter zipFilter;

    @Autowired
    private ProjectDescriptorArtefactResolver projectDescriptorResolver;

    @Autowired
    private ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory;

    @Autowired
    private ZipCharsetDetector zipCharsetDetector;

    @Autowired
    @Qualifier("deployConfigRepositoryComments")
    private Comments deployConfigRepoComments;

    @Autowired
    private RepositoryAclServiceProvider aclServiceProvider;

    @Autowired
    private ProjectVersionCacheManager projectVersionCacheManager;

    private final WebStudio studio = WebStudioUtils.getWebStudio(true);

    @Autowired
    private PropertyResolver propertyResolver;

    @Autowired
    private Utils utils;

    @Autowired
    private LocalWorkspaceManager localWorkspaceManager;

    @Autowired
    private LocalUploadController localUploadController;

    @Autowired
    private OpenAPIEditorService openAPIEditorService;

    @Autowired
    private TagTypeService tagTypeService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private String repositoryId;
    private String projectName;
    private String projectFolder = "";
    private String newProjectTemplate;
    private String folderName;
    private final List<ProjectFile> uploadedFiles = new ArrayList<>();
    private String fileName;
    private String uploadFrom;
    private String newProjectName;
    private String version;

    private boolean openDependencies = true;
    private AProject currentProject;

    private final TemplatesResolver predefinedTemplatesResolver = new PredefinedTemplatesResolver();
    private TemplatesResolver customTemplatesResolver;

    private TreeNode activeProjectNode;

    private CommentValidator deployConfigCommentValidator;

    private String createProjectComment;
    private String archiveProjectComment;
    private String restoreProjectComment;
    private String eraseProjectComment;
    private boolean eraseFromRepository;

    private String modelsModuleName;
    private String algorithmsModuleName;
    private String modelsPath;
    private String algorithmsPath;
    private boolean editModelsPath = false;
    private boolean editAlgorithmsPath = false;

    private final Map<String, RepositoryConfiguration> repositoryConfigurations = new HashMap<>();
    private final Map<String, Comments> allComments = new HashMap<>();

    public void setZipFilter(PathFilter zipFilter) {
        this.zipFilter = zipFilter;
    }

    /**
     * Adds new file to active node (project or folder).
     */
    public String addFile() {
        if (getLastUploadedFile() == null) {
            WebStudioUtils.addErrorMessage("Select a file to upload.");
            return null;
        }
        if (StringUtils.isEmpty(fileName)) {
            WebStudioUtils.addErrorMessage("File name must not be empty.");
            return null;
        }

        String errorMessage = uploadAndAddFile();

        if (errorMessage == null) {
            resetStudioModel();
            WebStudioUtils.addInfoMessage("File was uploaded successfully.");
        } else {
            WebStudioUtils.addErrorMessage(errorMessage);
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
                            var repositoryAclService = folder
                                    .getProject() instanceof ADeploymentProject
                                    ? aclServiceProvider.getDeployConfigRepoAclService()
                                    : aclServiceProvider.getDesignRepoAclService();
                            if (repositoryAclService.isGranted(folder, List.of(AclPermission.ADD))) {
                                AProjectFolder addedFolder = folder.addFolder(folderName);
                                repositoryTreeState.addNodeToTree(repositoryTreeState.getSelectedNode(), addedFolder);
                            } else {
                                throw new Message(
                                        String.format("There is no permission for creating a new folder in '%s'.",
                                                ProjectArtifactUtils.extractResourceName(projectArtefact)));
                            }
                        } catch (Exception e) {
                            log.error("Failed to create folder '{}'.", folderName, e);
                            errorMessage = e.getMessage();
                        }
                    } else {
                        errorMessage = String
                                .format("Folder name '%s' is invalid. %s", folderName, NameChecker.FOLDER_EXISTS);
                    }
                } else {
                    errorMessage = String
                            .format("Folder name '%s' is invalid. %s", folderName, NameChecker.BAD_NAME_MSG);
                }
            } else {
                errorMessage = String
                        .format("Folder name '%s' is invalid. %s", folderName, NameChecker.FOLDER_NAME_EMPTY);
            }
        }

        if (errorMessage != null) {
            WebStudioUtils.addErrorMessage("Failed to create a folder.", errorMessage);
        }
        return null;
    }

    public String saveProject() {
        UserWorkspaceProject project = null;
        try {
            ConflictUtils.removeMergeConflict();
            project = repositoryTreeState.getSelectedProject();
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
            setWasSaved(true);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof MergeConflictException) {
                log.debug("Failed to save the project because of merge conflict.", cause);
                if (project instanceof RulesProject) {
                    MergeConflictInfo info = new MergeConflictInfo((MergeConflictException) cause,
                            (RulesProject) project);
                    ConflictUtils.saveMergeConflict(info);
                }
            } else {
                String msg = e.getMessage();
                log.error(msg, e);
                WebStudioUtils.addErrorMessage(msg);
            }
        }
        return null;
    }

    public void setWasSaved(boolean wasSaved) {
        if (wasSaved) {
            WebStudioUtils.addInfoMessage("Project was saved successfully.");
        }
    }

    public String closeProject() {
        try {
            UserWorkspaceProject repositoryProject = repositoryTreeState.getSelectedProject();
            var repositoryAclService = repositoryProject instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                    : aclServiceProvider.getDesignRepoAclService();
            if (!repositoryAclService.isGranted(repositoryProject, List.of(AclPermission.VIEW))) {
                WebStudioUtils.addErrorMessage(String.format("There is no permission for closing '%s' project.",
                        ProjectArtifactUtils.extractResourceName(repositoryProject)));
                return null;
            }
            ProjectHistoryService.deleteHistory(repositoryProject.getBusinessName());
            closeProjectAndReleaseResources(repositoryProject);
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
        } catch (Exception e) {
            String msg = "Failed to close project.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
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
        if (repositoryTreeState.getErrorsContainer().hasErrors()) {
            return false;
        }
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
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            if (selectedProject instanceof ADeploymentProject) {
                return false;
            }
            Repository repository = selectedProject.getDesignRepository();
            String branch = repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
            AProject newVersion = userWorkspace.getDesignTimeRepository()
                    .getProjectByPath(repository.getId(), branch, selectedProject.getRealPath(), version);
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
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            Repository repository = selectedProject.getDesignRepository();
            String branch = repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
            AProject newVersion = userWorkspace.getDesignTimeRepository()
                    .getProjectByPath(repository.getId(), branch, selectedProject.getRealPath(), version);
            List<String> dependencies = new ArrayList<>(getDependencies(newVersion, true));
            Collections.sort(dependencies);
            return dependencies;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Collection<String> getDependencies(AProject project, boolean recursive) {
        Collection<String> processedProjects = new HashSet<>();
        Collection<String> dependencies = new HashSet<>();
        if (project != null) {
            processedProjects.add(project.getBusinessName());
            calcDependencies(project, recursive, processedProjects, dependencies);
        }
        return dependencies;
    }

    /**
     * Calc project dependencies. Only closed projects will be included in the result.
     *
     * @param project           inspecting project
     * @param recursive         if false, only direct dependencies will be included. If true, dependencies of dependent projects
     *                          will be included
     * @param processedProjects collections with already checked projects. Includes every checked for dependencies
     *                          project name. Needed to avoid stack overflow.
     * @param result            calculated project names for dependencies. Doesn't include closed/archived/not existing projects.
     */
    private void calcDependencies(AProject project,
                                  boolean recursive,
                                  Collection<String> processedProjects,
                                  Collection<String> result) {
        List<ProjectDependencyDescriptor> dependencies;
        try {
            dependencies = projectDescriptorResolver.getDependencies(project);
            if (dependencies == null) {
                return;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            repositoryTreeState.getErrorsContainer().addRequestError(e.getMessage());
            // Skip this dependency
            return;
        }

        String repoId = project.getRepository().getId();
        for (ProjectDependencyDescriptor dependency : dependencies) {
            try {
                final String dependencyName = dependency.getName();
                if (processedProjects.contains(dependencyName)) {
                    continue;
                } else {
                    processedProjects.add(dependencyName);
                }
                TreeProject projectNode = repositoryTreeState.getProjectNodeByBusinessName(repoId, dependencyName);
                if (projectNode == null) {
                    projectNode = repositoryTreeState.getProjectNodeByBusinessName(null, dependencyName);
                }
                if (projectNode == null) {
                    continue;
                }
                AProjectArtefact projectArtefact = projectNode.getData();
                AProject dependentProject = userWorkspace
                        .getProject(projectArtefact.getRepository().getId(), projectArtefact.getName(), false);
                if (canOpen(dependentProject)) {
                    result.add(dependencyName);
                }

                if (recursive) {
                    calcDependencies(dependentProject, true, processedProjects, result);
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
            WebStudioUtils.addErrorMessage(e.getMessage());
            return null;
        }

        if (project == null) {
            errorMessage = "No project is selected.";
        } else if (StringUtils.isBlank(newProjectName)) {
            errorMessage = "Project name is empty.";
        } else if (!NameChecker.checkName(newProjectName)) {
            errorMessage = String.format("Project name '%s' is invalid. %s", newProjectName, NameChecker.BAD_NAME_MSG);
        } else if (userWorkspace.hasDDProject(newProjectName)) {
            errorMessage = String.format("Deployment project '%s' already exists.", newProjectName);
        }

        if (errorMessage != null) {
            WebStudioUtils.addErrorMessage("Cannot copy deployment project.", errorMessage);
            return null;
        }

        try {
            String comment = deployConfigRepoComments.copiedFrom(project.getName());
            ADeploymentProject newProject = userWorkspace.copyDDProject(project, newProjectName, comment);
            if (!aclServiceProvider.getDeployConfigRepoAclService()
                    .createAcl(newProject, AclPermissionsSets.NEW_DEPLOYMENT_CONFIGURATION_PERMISSIONS, true)) {
                String message = String.format("Granting permissions to a new deployment configuration '%s' is failed.",
                        ProjectArtifactUtils.extractResourceName(newProject));
                WebStudioUtils.addErrorMessage(message);
            }
            repositoryTreeState.addDeploymentProjectToTree(newProject);
        } catch (Exception e) {
            String msg = "Failed to copy deployment project.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
        }

        return null;
    }

    public String createDeploymentConfiguration() {
        try {
            if (StringUtils.isBlank(projectName)) {
                WebStudioUtils.addErrorMessage("Deploy Configuration name must not be empty.");
                return null;
            }
            if (!NameChecker.checkName(projectName)) {
                WebStudioUtils.addErrorMessage(
                        "Specified name is not a valid Deploy Configuration name. " + NameChecker.BAD_NAME_MSG);
                return null;
            }
            if (NameChecker.isReservedName(projectName)) {
                WebStudioUtils.addErrorMessage("Specified deploy configuration name is a reserved word.");
                return null;
            }
            if (!aclServiceProvider.getDeployConfigRepoAclService().isGranted(
                    userWorkspace.getDesignTimeRepository().getDeployConfigRepository().getId(),
                    null,
                    List.of(AclPermission.CREATE))) {
                WebStudioUtils.addErrorMessage("There is no permission for creating a new deployment configuration.");
                return null;
            }
            if (userWorkspace.hasDDProject(projectName)) {
                WebStudioUtils.addErrorMessage(
                        "Cannot create configuration because configuration with such name already exists.");
                return null;
            }
            ADeploymentProject createdProject = userWorkspace.createDDProject(projectName);
            if (!aclServiceProvider.getDeployConfigRepoAclService()
                    .createAcl(createdProject, AclPermissionsSets.NEW_DEPLOYMENT_CONFIGURATION_PERMISSIONS, true)) {
                String message = String.format("Granting permissions to a new deployment configuration '%s' is failed.",
                        ProjectArtifactUtils.extractResourceName(createdProject));
                WebStudioUtils.addErrorMessage(message);
            }
            createdProject.open();
            // Analogous to rules project creation (to change "created by"
            // property and revision)
            String comment = deployConfigRepoComments.createProject(projectName);

            createdProject.getFileData().setComment(comment);
            createdProject.save();
            createdProject.open();
            repositoryTreeState.addDeploymentProjectToTree(createdProject);
            WebStudioUtils
                    .addInfoMessage(String.format("Deploy Configuration '%s' is successfully created.", projectName));
        } catch (Exception e) {
            String msg = String.format("Failed to create Deploy Configuration '%s'.", projectName);
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
        }

        /* Clear the load form */
        this.clearForm();

        return null;
    }

    public String getCurrentDeployConfigRepositoryType() {
        return Optional.ofNullable(userWorkspace)
                .map(UserWorkspace::getDesignTimeRepository)
                .map(DesignTimeRepository::getDeployConfigRepository)
                .map(Repository::getId)
                .map(this::getRepositoryConfiguration)
                .map(RepositoryConfiguration::getType)
                .orElse(null);
    }

    public List<Repository> getCreateAllowedRepositories() {
        DesignTimeRepository designRepo = userWorkspace.getDesignTimeRepository();
        return designRepo.getRepositories()
                .stream()
                .filter(repo -> !repo.supports().branches() || !((BranchRepository) repo)
                        .isBranchProtected(((BranchRepository) repo).getBranch()))
                .filter(e -> aclServiceProvider.getDesignRepoAclService().isGranted(e.getId(), null, List.of(AclPermission.CREATE)))
                .collect(Collectors.toList());
    }

    public String getCreateAllowedRepositoriesTypes() throws JsonProcessingException {
        Map<String, String> types = getCreateAllowedRepositories().stream()
                .map(Repository::getId)
                .map(this::getRepositoryConfiguration)
                .filter(e -> aclServiceProvider.getDesignRepoAclService().isGranted(e.getId(), null, List.of(AclPermission.CREATE)))
                .collect(Collectors.toMap(RepositoryConfiguration::getConfigName, RepositoryConfiguration::getType));
        return new ObjectMapper().writeValueAsString(types);
    }

    public boolean getCanCreateNewProject() {
        return !getCreateAllowedRepositories().isEmpty();
    }

    public String createNewRulesProject() {
        String comment;
        if (StringUtils.isNotBlank(createProjectComment)) {
            comment = createProjectComment;
        } else {
            comment = getDesignRepoComments().createProject(projectName);
        }
        String msg = validateCreateProjectParams(comment);

        if (msg != null) {
            this.clearForm();
            WebStudioUtils.addErrorMessage(msg);
            return null;
        }

        String[] templateParts = newProjectTemplate.split("/");
        TemplatesResolver templatesResolver = CUSTOM_TEMPLATE_TYPE
                .equals(templateParts[0]) ? customTemplatesResolver : predefinedTemplatesResolver;
        ProjectFile[] templateFiles = templatesResolver.getProjectFiles(templateParts[1], templateParts[2]);
        if (templateFiles.length == 0) {
            this.clearForm();
            String errorMessage = String.format("Cannot load template files: %s", newProjectTemplate);
            WebStudioUtils.addErrorMessage(errorMessage);
            return null;
        }

        if (!aclServiceProvider.getDesignRepoAclService().isGranted(repositoryId, null, List.of(AclPermission.CREATE))) {
            WebStudioUtils.addErrorMessage("There is no permission for creating a new project.");
            return null;
        }

        ExcelFilesProjectCreator projectCreator = new ExcelFilesProjectCreator(repositoryId,
                projectName,
                projectFolder,
                userWorkspace,
                comment,
                zipFilter,
                projectTagsBean.saveTagsTypesAndGetTags(),
                templateFiles);
        try {
            try {
                RulesProject newRuleProject = projectCreator.createRulesProject();

                if (aclServiceProvider.getDesignRepoAclService().createAcl(newRuleProject.getDesignRepository().getId(),
                        newRuleProject.getDesignFolderName(),
                        AclPermissionsSets.NEW_PROJECT_PERMISSIONS,
                        true)) {
                    // Get just created project, because creator API doesn't create internals states for ProjectState
                    RulesProject createdProject = userWorkspace.getProject(repositoryId,
                            projectCreator.getCreatedProjectName());
                    if (!userWorkspace.isOpenedOtherProject(createdProject)) {
                        createdProject.open();
                    }

                    repositoryTreeState.addRulesProjectToTree(createdProject);
                    selectProject(createdProject.getName(), repositoryTreeState.getRulesRepository());

                    resetStudioModel();

                    WebStudioUtils.addInfoMessage("Project was created successfully.");
                    /* Clear the load form */
                    this.clearForm();

                    return null;
                } else {
                    String message = String.format("Granting permissions to a new project '%s' is failed.",
                            ProjectArtifactUtils.extractResourceName(newRuleProject));
                    WebStudioUtils.addErrorMessage(message);
                    return message;
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                WebStudioUtils.addErrorMessage(e.getMessage());
                return e.getMessage();
            }
        } finally {
            projectCreator.destroy();
        }
    }

    private String validateCreateProjectParams(String comment) {
        return Stream
                .<Supplier<String>>of(this::validateRepositoryId,
                        this::validateProjectName,
                        this::validateProjectFolder,
                        () -> validateCreateProjectComment(comment))
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String validateRepositoryId() {
        if (StringUtils.isBlank(repositoryId)) {
            return "Repository must be selected.";
        }
        return null;
    }

    private String validateProjectName() {
        try {
            String msg = null;
            if (StringUtils.isBlank(projectName)) {
                msg = "Project name must not be empty.";
            } else if (!NameChecker.checkName(projectName)) {
                msg = "Specified name is not a valid project name." + " " + NameChecker.BAD_NAME_MSG;
            } else if (NameChecker.isReservedName(projectName)) {
                msg = "Specified project name is a reserved word.";
            } else if (userWorkspace.getDesignTimeRepository().hasProject(repositoryId, projectName)) {
                msg = "Cannot create project because project with such name already exists.";
            } else {
                Repository repository = userWorkspace.getDesignTimeRepository().getRepository(repositoryId);
                if (repository.supports().mappedFolders()) {
                    String projectPath = StringUtils.isEmpty(projectFolder) ? projectName : projectFolder + projectName;
                    if (((FolderMapper) repository).getDelegate().check(projectPath) != null) {
                        return "Cannot create the project because a project with such path already exists. Try to import that project from repository or create new project with another path or name.";
                    }
                    msg = validateProjectFolder();
                    if (msg != null) {
                        return msg;
                    }
                    final Path currentPath = Paths.get(projectPath);
                    if (userWorkspace.getDesignTimeRepository()
                            .getProjects()
                            .stream()
                            .filter(proj -> proj.getRepository().getId().equals(repositoryId))
                            .map(AProjectFolder::getRealPath)
                            .map(Paths::get)
                            .anyMatch(path -> path.startsWith(currentPath) || currentPath.startsWith(path))) {
                        return "Cannot create the project because a path conflicts with an existed project.";
                    }
                }
            }
            return msg;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Internal error while creating a project: " + e.getMessage();
        }
    }

    private String validateProjectFolder() {
        try {
            FolderStructureValidators.validatePathInRepository(projectFolder);
        } catch (ValidatorException e) {
            return e.getMessage();
        }
        return null;
    }

    private String validateCreateProjectComment(String comment) {
        try {
            getDesignCommentValidator(repositoryId).validate(comment);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public boolean isOpenLProjectInFolder() {
        if (StringUtils.isEmpty(repositoryId) || StringUtils.isEmpty(projectFolder)) {
            return false;
        }

        Repository repository = userWorkspace.getDesignTimeRepository().getRepository(repositoryId);
        if (!repository.supports().mappedFolders()) {
            return false;
        }

        try {
            String projectPath = projectFolder;

            List<FileData> files = ((FolderMapper) repository).getDelegate().list(projectPath);
            if (files.isEmpty()) {
                return false;
            }
            return files.stream().anyMatch(fileData -> {
                String name = fileData.getName();
                if (name.equals(projectPath + "rules.xml")) {
                    return true;
                }
                if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
                    return name.startsWith(projectPath) && !name.substring(projectPath.length()).contains("/");
                }
                return false;
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /*
     * Because of renaming 'Deployment project' to 'Deploy Configuration' the method was renamed too.
     */
    public String deleteDeploymentConfiguration() {
        String projectName = WebStudioUtils.getRequestParameter("deploymentProjectName");

        try {
            ADeploymentProject project = userWorkspace.getDDProject(projectName);
            // projectInTree must be initialized before project was deleted
            TreeNode projectInTree = repositoryTreeState.getDeploymentRepository()
                    .getChild(RepositoryUtils.getTreeNodeId(project));
            String comment = deployConfigRepoComments.archiveProject(project.getName());
            project.delete(userWorkspace.getUser(), comment);
            if (repositoryTreeState.isHideDeleted()) {
                repositoryTreeState.deleteNode(projectInTree);
            }

            WebStudioUtils.addInfoMessage("Deploy configuration was deleted successfully.");
        } catch (Exception e) {
            log.error("Cannot delete deploy configuration '{}'.", projectName, e);
            WebStudioUtils.addErrorMessage("Failed to delete deploy configuration.", e.getMessage());
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

    private void unregisterSelectedNodeInProjectDescriptor() throws ProjectException, JAXBException, IOException {
        TreeNode selectedNode = getSelectedNode();
        String nodeType = selectedNode.getType();
        if (UiConst.TYPE_FOLDER.equals(nodeType) || UiConst.TYPE_FILE.equals(nodeType)) {
            unregisterArtifactInProjectDescriptor(selectedNode.getData());
        }
    }

    private void unregisterArtifactInProjectDescriptor(
            AProjectArtefact aProjectArtefact) throws ProjectException, JAXBException, IOException {
        UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
        AProjectArtefact projectDescriptorArtifact;
        try {
            projectDescriptorArtifact = selectedProject
                    .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        } catch (ProjectException ex) {
            // Project does not contain rules.xml file
            return;
        }
        Collection<String> modulePaths = new HashSet<>();
        findModulePaths(aProjectArtefact, modulePaths);
        if (projectDescriptorArtifact instanceof AProjectResource) {
            IProjectDescriptorSerializer serializer = projectDescriptorSerializerFactory.getSerializer(selectedProject);

            String projectDescriptorPath = projectDescriptorArtifact.getArtefactPath()
                    .withoutFirstSegment()
                    .getStringValue();
            if (projectDescriptorPath
                    .equals(aProjectArtefact.getArtefactPath().withoutFirstSegment().getStringValue())) {
                // There is no need to unregister itself
                return;
            }

            AProjectResource resource = (AProjectResource) projectDescriptorArtifact;
            InputStream content = resource.getContent();
            ProjectDescriptor projectDescriptor;
            try {
                projectDescriptor = serializer.deserialize(content);
            } catch (JAXBException e) {
                log.error("Broken rules.xml file. Cannot remove modules from it", e);
                return;
            } finally {
                IOUtils.closeQuietly(content);
            }
            List<String> removedModuleNames = new ArrayList<>();
            for (String modulePath : modulePaths) {
                projectDescriptor.getModules().removeIf(module -> {
                    boolean contains = modulePath.equals(module.getRulesRootPath().getPath());
                    if (contains) {
                        removedModuleNames.add(module.getName());
                    }
                    return contains;
                });
            }
            boolean projectDescriptorChanged = !removedModuleNames.isEmpty();
            OpenAPI openAPI = projectDescriptor.getOpenapi();
            final FileData fileData = aProjectArtefact.getFileData();
            if (openAPI != null) {
                final String algorithmModuleName = openAPI.getAlgorithmModuleName();
                final String projectModelsModuleName = openAPI.getModelModuleName();
                if (removedModuleNames.contains(algorithmModuleName)) {
                    openAPI.setAlgorithmModuleName(null);
                }
                if (removedModuleNames.contains(projectModelsModuleName)) {
                    openAPI.setModelModuleName(null);
                }
                if (fileData != null) {
                    final String name = fileData.getName();
                    final String rootName = projectDescriptor.getName();
                    String filePath = name.substring(name.lastIndexOf(rootName) + rootName.length() + 1);
                    if (openAPI.getPath() != null && filePath.equals(openAPI.getPath())) {
                        projectDescriptor.setOpenapi(null);
                        projectDescriptorChanged = true;
                    }
                }
            }
            if (projectDescriptorChanged) {
                String xmlString = serializer.serialize(projectDescriptor);
                InputStream newContent = IOUtils.toInputStream(xmlString);
                if (!aclServiceProvider.getDesignRepoAclService().isGranted(resource, List.of(AclPermission.EDIT))) {
                    throw new Message(String.format("There is no permission for modifying '%s' file.",
                            ProjectArtifactUtils.extractResourceName(resource)));
                }
                resource.setContent(newContent);
            }
        }
    }

    public String deleteElement() {
        AProjectArtefact artefact = repositoryTreeState.getSelectedNode().getData();
        String childName = WebStudioUtils.getRequestParameter("element");
        AProjectArtefact childArtefact = ((TreeNode) repositoryTreeState.getSelectedNode()
                .getChild(RepositoryUtils.getTreeNodeId(artefact.getRepository().getId(), childName))).getData();
        var repositoryAclService = getSelectedProject() instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                : aclServiceProvider.getDesignRepoAclService();
        if (!repositoryAclService.isGranted(childArtefact, List.of(AclPermission.DELETE))) {
            WebStudioUtils.addErrorMessage(String.format("There is no permission for deleting '%s' file.",
                    ProjectArtifactUtils.extractResourceName(childArtefact)));
            return null;
        }
        try {
            studio.getModel().clearModuleInfo(); // Release resources like jars
            unregisterArtifactInProjectDescriptor(childArtefact);
            childArtefact.delete();
            if (getSelectedProject() instanceof ADeploymentProject) {
                repositoryAclService.deleteAcl(childArtefact);
            }
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();

            WebStudioUtils.addInfoMessage("Element was deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting element.", e);
            WebStudioUtils.addErrorMessage("Error deleting.", e.getMessage());
        }
        return null;
    }

    public String getCurrentNodeRepositoryType() {
        return Optional.ofNullable(getSelectedNode().getData())
                .map(AProjectArtefact::getProject)
                .map(AProjectArtefact::getRepository)
                .map(Repository::getId)
                .map(this::getRepositoryConfiguration)
                .map(RepositoryConfiguration::getType)
                .orElse(null);
    }

    public String getRepositoryType(String repositoryId) {
        return getRepositoryConfiguration(repositoryId).getType();
    }

    private RepositoryConfiguration getRepositoryConfiguration(String repositoryId) {
        return repositoryConfigurations.computeIfAbsent(repositoryId,
                k -> new RepositoryConfiguration(k, propertyResolver));
    }

    public String deleteNode() {
        TreeNode selectedNode = getSelectedNode();
        AProjectArtefact projectArtefact = selectedNode.getData();
        if (projectArtefact == null) {
            activeProjectNode = null;
            WebStudioUtils.addErrorMessage("Element is already deleted.");
            return null;
        }
        AProject p = projectArtefact.getProject();
        boolean localOnly = p instanceof UserWorkspaceProject && ((UserWorkspaceProject) p).isLocalOnly();
        String repositoryId = p.getRepository().getId();
        if (isSupportsBranches(repositoryId) && projectArtefact.getVersion() == null && !localOnly) {
            activeProjectNode = null;
            WebStudioUtils.addErrorMessage("Failed to delete the project. The project does not exist in the branch.");
            return null;
        }
        var repositoryAclService = projectArtefact
                .getProject() instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService() : aclServiceProvider.getDesignRepoAclService();
        if (!repositoryAclService.isGranted(projectArtefact, List.of(AclPermission.DELETE))) {
            throw new Message(String.format("There is no permission for deleting '%s' project.",
                    ProjectArtifactUtils.extractResourceName(projectArtefact)));
        }
        try {
            studio.getModel().clearModuleInfo(); // Release resources like jars
            String nodeType = selectedNode.getType();
            unregisterSelectedNodeInProjectDescriptor();
            if (projectArtefact instanceof RulesProject) {
                RulesProject project = (RulesProject) projectArtefact;
                if (!userWorkspace.hasProject(project.getRepository().getId(), project.getName())) {
                    WebStudioUtils.addInfoMessage("Project was already deleted before.");
                    return null;
                }
                if (projectArtefact.isLocked() && !((RulesProject) projectArtefact).isLockedByMe()) {
                    WebStudioUtils.addErrorMessage("Project is locked by other user. Cannot archive it.");
                    return null;
                }
                File workspacesRoot = userWorkspace.getLocalWorkspace().getLocation().getParentFile();
                closeProjectForAllUsers(workspacesRoot, (RulesProject) projectArtefact);
            }
            if (projectArtefact instanceof UserWorkspaceProject) {
                UserWorkspaceProject project = (UserWorkspaceProject) projectArtefact;
                String comment;
                if (project instanceof RulesProject && isUseCustomCommentForProject()) {
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
                repositoryAclService.deleteAcl(projectArtefact);
            }
            TreeNode parent = selectedNode.getParent();
            if (parent != null && parent.getData() != null) {
                parent.refresh();
            }

            if (projectArtefact instanceof UserWorkspaceProject) {
                if (repositoryTreeState.isHideDeleted() || ((UserWorkspaceProject) projectArtefact).isLocalOnly()) {
                    if (selectedNode != activeProjectNode) {
                        repositoryTreeState.deleteSelectedNodeFromTree();
                    } else {
                        repositoryTreeState.deleteNode(selectedNode);
                        repositoryTreeState.invalidateSelection();
                    }
                    if (isSupportsBranches(repositoryId)) {
                        repositoryTreeState.invalidateTree();
                    }
                } else {
                    repositoryTreeState.refreshSelectedNode();
                }
            } else {
                repositoryTreeState.deleteSelectedNodeFromTree();
            }

            activeProjectNode = null;
            if (projectArtefact instanceof UserWorkspaceProject) {
                workspaceManager.refreshWorkspaces();
            }
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
            WebStudioUtils.addInfoMessage(nodeTypeName + " was deleted successfully.");
            eventPublisher.publishEvent(new ProjectDeletedEvent(projectArtefact));
        } catch (Exception e) {
            log.error("Failed to delete node.", e);
            WebStudioUtils.addErrorMessage("Failed to delete node.", e.getMessage());
        }

        return null;
    }

    private boolean isValidComment(UserWorkspaceProject project, String comment) {
        CommentValidator commentValidator = project instanceof RulesProject ? getDesignCommentValidator(project)
                : deployConfigCommentValidator;

        try {
            commentValidator.validate(comment);
        } catch (Exception e) {
            WebStudioUtils.addErrorMessage(e.getMessage());
            return false;
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
            WebStudioUtils.addInfoMessage(nodeTypeName + " was deleted already.");
            return null;
        }
        try {
            projectArtefact.unlock();
            repositoryTreeState.refreshSelectedNode();
            if (projectArtefact instanceof RulesProject) {
                File workspacesRoot = userWorkspace.getLocalWorkspace().getLocation().getParentFile();
                closeProjectForAllUsers(workspacesRoot, (RulesProject) projectArtefact);
            }
            resetStudioModel();

            WebStudioUtils.addInfoMessage(nodeTypeName + " was unlocked successfully.");
        } catch (Exception e) {
            log.error("Failed to unlock node.", e);
            WebStudioUtils.addErrorMessage("Failed to unlock node.", e.getMessage());
        }

        return null;
    }

    public String unlockProject() {
        try {
            RulesProject project = userWorkspace.getProject(repositoryId, projectName);
            if (project == null) {
                // It was deleted by other user
                return null;
            }
            project.forceUnlock();
            File workspacesRoot = userWorkspace.getLocalWorkspace().getLocation().getParentFile();
            closeProjectForAllUsers(workspacesRoot, project);
            resetStudioModel();

            WebStudioUtils.addInfoMessage("Project was unlocked successfully.");
        } catch (Exception e) {
            log.error("Cannot unlock rules project '{}'.", projectName, e);
            WebStudioUtils.addErrorMessage("Failed to unlock rules project.", e.getMessage());
        }
        return null;
    }

    /**
     * Closes unlocked project for all users. All unsaved changes will be lost.
     */
    private void closeProjectForAllUsers(File workspacesRoot, RulesProject project) throws ProjectException {
        String projectName = project.getName();
        String businessName = project.getBusinessName();
        String branch = project.getBranch();
        String repoId = project.getRepository().getId();

        // Needed to update UI of current user
        TreeProject projectNode = repositoryTreeState.getProjectNodeByPhysicalName(repoId, projectName);
        try {
            ProjectHistoryService.deleteHistory(businessName);
            if (projectNode != null) {
                AProjectArtefact artefact = projectNode.getData();
                if (artefact instanceof RulesProject) {
                    ((RulesProject) artefact).close();
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        // List all folders. Those folders - workspaces for each user (except for reserved .locks folder)
        File[] files = workspacesRoot.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String userId = file.getName();
                    // Check for reserved folder name
                    if (!LockEngineImpl.LOCKS_FOLDER_NAME.equals(userId)) {
                        try {
                            LocalRepository repository = localWorkspaceManager.getWorkspace(userId)
                                    .getRepository(repoId);
                            repository.initialize();

                            ProjectState projectState = repository.getProjectState(businessName);
                            String savedRepoId = projectState.getRepositoryId();
                            FileData savedData = projectState.getFileData();
                            String savedBranch = savedData == null ? null : savedData.getBranch();

                            if (savedRepoId != null && savedRepoId.equals(repoId)) {
                                if (branch == null && savedBranch == null || branch != null && branch
                                        .equals(savedBranch)) {
                                    FileData fileData = new FileData();
                                    fileData.setName(businessName);
                                    repository.delete(fileData);
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
        try {
            ADeploymentProject deploymentProject = userWorkspace.getDDProject(projectName);
            if (deploymentProject == null) {
                // It was deleted by other user
                return null;
            }
            deploymentProject.unlock();
            resetStudioModel();

            WebStudioUtils.addInfoMessage("Deploy configuration was unlocked successfully.");
        } catch (Exception e) {
            log.error("Cannot unlock deployment project '{}'.", projectName, e);
            WebStudioUtils.addErrorMessage("Failed to unlock deployment project.", e.getMessage());
        }
        return null;
    }

    public String eraseProject() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        // EPBDS-225
        if (project == null) {
            return null;
        }

        String nodeType = getSelectedNode().getType();
        if (!project.isDeleted()) {
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();
            WebStudioUtils.addErrorMessage(
                    "Cannot erase project '" + project.getBusinessName() + "'. It must be marked for deletion first.");
            return null;
        }

        try {
            projectDescriptorResolver.deleteRevisionsFromCache(project);
            synchronized (userWorkspace) {
                String comment;
                if (project instanceof RulesProject && isUseCustomCommentForProject()) {
                    comment = eraseProjectComment;
                    if (!isValidComment(project, comment)) {
                        return null;
                    }
                } else {
                    Comments comments = getComments(project);
                    comment = comments.eraseProject(project.getBusinessName());
                }
                try {
                    Repository designRepository = project.getDesignRepository();
                    boolean mappedFolders = designRepository.supports().mappedFolders();
                    if (!mappedFolders || eraseFromRepository) {
                        project.erase(userWorkspace.getUser(), comment);
                        var repositoryAclService = project instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                                : aclServiceProvider.getDesignRepoAclService();
                        repositoryAclService.deleteAcl(project);
                    } else {
                        ((FolderMapper) designRepository).removeMapping(project.getFolderPath());
                    }
                } catch (ProjectException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof MergeConflictException) {
                        log.debug("Failed to erase the project because of merge conflict.", cause);
                        // Try to erase second time. It should resolve the issue if conflict in
                        // openl-projects.properties file.
                        project.erase(userWorkspace.getUser(), comment);
                        var repositoryAclService = project instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                                : aclServiceProvider.getDesignRepoAclService();
                        repositoryAclService.deleteAcl(project);
                    } else {
                        throw e;
                    }
                }
            }
            workspaceManager.refreshWorkspaces();

            repositoryTreeState.deleteSelectedNodeFromTree();
            repositoryTreeState.invalidateTree();
            repositoryTreeState.invalidateSelection();

            resetStudioModel();
            if (UiConst.TYPE_DEPLOYMENT_PROJECT.equals(nodeType)) {
                WebStudioUtils.addInfoMessage("Deploy configuration was erased successfully.");
            } else {
                WebStudioUtils.addInfoMessage("Project was erased successfully.");
            }
        } catch (Exception e) {
            repositoryTreeState.invalidateTree();
            String msg = e.getCause() instanceof IOException ? e
                    .getMessage() : "Cannot erase project '" + project.getBusinessName() + "'.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg);
        }
        return null;
    }

    public void deleteBranch() {
        UserWorkspaceProject project = getSelectedProject();
        if (!(project instanceof RulesProject)) {
            return;
        }
        try {
            String branch = project.getBranch();

            Repository mainRepo = userWorkspace.getDesignTimeRepository()
                    .getRepository(project.getRepository().getId());
            if (mainRepo != null && mainRepo.supports().branches() && !((BranchRepository) mainRepo).getBranch()
                    .equals(branch)) {
                ProjectHistoryService.deleteHistory(project.getBusinessName());
                closeProjectAndReleaseResources(project);

                // Delete secondary branch
                ((BranchRepository) mainRepo).deleteBranch(null, branch);
                workspaceManager.refreshWorkspaces();

                repositoryTreeState.invalidateTree();
                repositoryTreeState.invalidateSelection();

                resetStudioModel();
                WebStudioUtils.addInfoMessage("Branch '" + branch + "' was deleted successfully.");
            }
        } catch (Exception e) {
            String msg = "Cannot delete the branch '" + project.getBranch() + "'.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg);
        }
    }

    public String exportProjectVersion() {
        File zipFile = null;
        String zipFileName = null;
        try {
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            Repository repository = selectedProject.getDesignRepository();
            String branch = repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
            AProject forExport = userWorkspace.getDesignTimeRepository()
                    .getProjectByPath(repository.getId(), branch, selectedProject.getRealPath(), version);
            zipFile = ProjectExportHelper.export(userWorkspace.getUser(), forExport);
            String suffix = RepositoryUtils.buildProjectVersion(forExport.getFileData());
            zipFileName = String.format("%s-%s.zip", selectedProject.getBusinessName(), suffix);
        } catch (Exception e) {
            String msg = "Failed to export project version.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
        }

        if (zipFile != null) {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) WebStudioUtils.getExternalContext().getResponse();
            ExportFile.writeOutContent(response, zipFile, zipFileName);
            facesContext.responseComplete();

            if (!zipFile.delete()) {
                log.warn("Temporary zip file {} has not been deleted", zipFile.getName());
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
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            Repository repository = selectedProject.getDesignRepository();
            String branch = repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
            AProject forExport = userWorkspace.getDesignTimeRepository()
                    .getProjectByPath(repository.getId(), branch, selectedProject.getRealPath(), version);
            TreeNode selectedNode = repositoryTreeState.getSelectedNode();
            fileName = selectedNode.getName();
            ArtefactPath selectedNodePath = selectedNode.getInternalArtifactPath();

            is = ((AProjectResource) forExport.getArtefactByPath(selectedNodePath)).getContent();
            file = File.createTempFile("export-", "-file");
            os = new FileOutputStream(file);
            is.transferTo(os);

            final FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) WebStudioUtils.getExternalContext().getResponse();
            ExportFile.writeOutContent(response, file, fileName);
            facesContext.responseComplete();
        } catch (Exception e) {
            String msg = "Failed to export file version.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
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
        String path = WebStudioUtils.getRequestParameter("copyFileForm:filePath");
        String currentRevision = WebStudioUtils.getRequestParameter("copyFileForm:currentRevision");
        boolean hasVersions = !repositoryTreeState.isLocalOnly() && getSelectedNode().hasVersions();
        if (StringUtils.isBlank(path)) {
            WebStudioUtils.addErrorMessage("Path should not be an empty.");
            return null;
        }
        UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
        var repositoryAclService = selectedProject instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                : aclServiceProvider.getDesignRepoAclService();

        ArtefactPath artefactPath = new ArtefactPathImpl(path);
        try {
            selectedProject.getArtefactByPath(artefactPath);
            WebStudioUtils.addErrorMessage(String.format("File '%s' is already exists.", path));
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
                    WebStudioUtils.addErrorMessage(
                            String.format("Artefact '%s' is not a folder.", artefact.getArtefactPath().getStringValue()));
                    return null;
                }
                folder = (AProjectFolder) folder.getArtefact(segment);
            }

            AProject forExport;
            String repositoryId = selectedProject.getRepository().getId();
            if (hasVersions && currentRevision == null) {
                Repository repository = selectedProject.getDesignRepository();
                String branch = repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
                forExport = userWorkspace.getDesignTimeRepository()
                        .getProjectByPath(repositoryId, branch, selectedProject.getRealPath(), version);

                TreeNode selectedNode = repositoryTreeState.getSelectedNode();
                ArtefactPath selectedNodePath = selectedNode.getInternalArtifactPath();
                is = ((AProjectResource) forExport.getArtefactByPath(selectedNodePath)).getContent();
            } else {
                TreeNode selectedNode = repositoryTreeState.getSelectedNode();
                ArtefactPath pathForSelection = selectedNode.getData().getArtefactPath();
                String projectName = pathForSelection.segment(0);
                AProject uwp = userWorkspace.getProject(repositoryId, projectName);

                ArtefactPath pathInProject = pathForSelection.withoutFirstSegment();

                is = ((AProjectResource) uwp.getArtefactByPath(pathInProject)).getContent();
            }
            if (!aclServiceProvider.getDesignRepoAclService().isGranted(folder, List.of(AclPermission.ADD))) {
                throw new Message(String.format("There is no permission for creating '%s/%s' file.",
                        folder.getArtefactPath().getStringValue(),
                        artefactPath.segment(artefactPath.segmentCount() - 1)));
            }
            AProjectResource addedFileResource = folder
                    .addResource(artefactPath.segment(artefactPath.segmentCount() - 1), is);
            if ((selectedProject instanceof ADeploymentProject || !aclServiceProvider.getDesignRepoAclService()
                    .hasAcl(addedFileResource)) && !repositoryAclService
                    .createAcl(addedFileResource, AclPermissionsSets.NEW_FILE_PERMISSIONS, true)) {
                String message = String.format("Granting permissions to a new file '%s' is failed.",
                        ProjectArtifactUtils.extractResourceName(addedFileResource));
                WebStudioUtils.addErrorMessage(message);
            }
            fileName = addedFileResource.getName();
            repositoryTreeState
                    .refreshNode(repositoryTreeState.getProjectNodeByPhysicalName(repositoryId, selectedProject.getName()));
            registerInProjectDescriptor(addedFileResource);
            resetStudioModel();
            is.close();
        } catch (Exception e) {
            String msg = "Failed to export file version.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }

        return null;
    }

    public boolean isHideDeleted() {
        return repositoryTreeState.isHideDeleted();
    }

    public void setHideDeleted(boolean hideDeleted) {
        repositoryTreeState.setHideDeleted(hideDeleted);
    }

    public void filter() {
        repositoryTreeState.filter();
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
        TreeRepository deploymentRepository = repositoryTreeState.getDeploymentRepository();
        if (deploymentRepository == null) {
            return null;
        }
        return deploymentRepository.getChildNodes();
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFolderName() {
        return null;
    }

    public String getVersionComment() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        Comments comments = getComments(project);

        if (project != null && project.isOpenedOtherVersion()) {
            FileData fileData = project.getFileData();
            String name = Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).orElse(null);
            return comments.restoredFrom(fileData.getVersion(), name, fileData.getModifiedAt());
        }

        return project == null ? StringUtils.EMPTY : comments.saveProject(project.getBusinessName());
    }

    private Comments getComments(UserWorkspaceProject project) {
        if (project == null || project.getDesignRepository() == null) {
            return getComments(Comments.DESIGN_CONFIG_REPO_ID);
        }
        if (project instanceof ADeploymentProject) {
            return deployConfigRepoComments;
        }
        repositoryId = project.getDesignRepository().getId();
        return getDesignRepoComments();
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
        String folderToShow = this.projectFolder;
        if (!folderToShow.startsWith("/")) {
            folderToShow = "/" + folderToShow;
        }
        return folderToShow;
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
     * Gets rules projects.
     *
     * @return list of rules projects
     */
    public List<TreeNode> getRulesProjects() {
        final TreeNode node = repositoryTreeState.getSelectedNode();
        final List<TreeNode> rulesProjects = getRulesProjects(node);
        rulesProjects.sort(Comparator.comparing(treeNode -> treeNode.getName().toLowerCase()));
        return rulesProjects;
    }

    private List<TreeNode> getRulesProjects(TreeNode node) {
        List<TreeNode> list = new ArrayList<>();
        for (TreeNode treeNode : node.getChildNodes()) {
            if (UiConst.TYPE_PROJECT.equals(treeNode.getType())) {
                list.add(treeNode);
            } else if (UiConst.TYPE_GROUP.equals(treeNode.getType())) {
                list.addAll(getRulesProjects(treeNode));
            }
        }
        return list;
    }

    public SelectItem[] getSelectedProjectVersions() {
        Collection<ProjectVersion> versions = repositoryTreeState.getSelectedNode().getVersions();
        return toSelectItems(versions);
    }

    public SelectItem[] toSelectItems(Collection<ProjectVersion> versions) {
        if (versions == null) {
            return new SelectItem[0];
        }
        List<SelectItem> selectItems = new ArrayList<>();
        for (ProjectVersion version : versions) {
            if (!version.isDeleted()) {
                selectItems.add(new SelectItem(version.getVersionName(), utils.getDescriptiveVersion(version)));
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
            UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
            var repositoryAclService = project instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                    : aclServiceProvider.getDesignRepoAclService();
            if (!repositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
                throw new Message(String.format("There is no permission for opening '%s' project.",
                        ProjectArtifactUtils.extractResourceName(project)));
            }
            if (userWorkspace.isOpenedOtherProject(project)) {
                WebStudioUtils.addErrorMessage(OPENED_OTHER_PROJECT);
                return null;
            }
            project.open();
            // User workspace is changed when the project was opened, so we must refresh it to calc dependencies.
            // resetStudioModel() should internally refresh workspace.
            resetStudioModel();
            openDependenciesIfNeeded();
            repositoryTreeState.refreshSelectedNode();
        } catch (Exception e) {
            String msg = "Failed to open project.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    private void openDependenciesIfNeeded() throws ProjectException {
        if (openDependencies) {
            AProject selectedProject = getSelectedProject();
            if (selectedProject == null) {
                return;
            }
            boolean openedAnyDependency = false;
            String repoId = selectedProject.getRepository().getId();
            for (String dependency : getDependencies(selectedProject, true)) {
                TreeProject projectNode = repositoryTreeState.getProjectNodeByBusinessName(repoId, dependency);
                if (projectNode == null) {
                    projectNode = repositoryTreeState.getProjectNodeByBusinessName(null, dependency);
                }
                if (projectNode == null) {
                    log.error("Cannot find dependency {}", dependency);
                    continue;
                }
                AProjectArtefact projectArtefact = projectNode.getData();
                RulesProject project = userWorkspace.getProject(projectArtefact.getRepository().getId(),
                        projectArtefact.getName());
                if (!userWorkspace.isOpenedOtherProject(project)) {
                    project.open();
                    openedAnyDependency = true;
                }
            }

            if (openedAnyDependency) {
                // If opened any dependency, we need to refresh workspace again
                resetStudioModel();
            }
        }
    }

    public String openProjectVersion() {
        try {
            UserWorkspaceProject repositoryProject = repositoryTreeState.getSelectedProject();
            if (repositoryProject == null) {
                WebStudioUtils.addErrorMessage("Project version is not selected.");
                return null;
            }
            var repositoryAclService = repositoryProject instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                    : aclServiceProvider.getDesignRepoAclService();
            if (!repositoryAclService.isGranted(repositoryProject, List.of(AclPermission.VIEW))) {
                throw new Message(String.format("There is no permission for opening '%s' project.",
                        ProjectArtifactUtils.extractResourceName(repositoryProject)));
            }
            if (!UiConst.TYPE_DEPLOYMENT_PROJECT.equals(repositoryTreeState.getSelectedNode().getType())) {
                boolean openedSimilarToHistoric = false;
                if (repositoryProject instanceof RulesProject) {
                    RulesProject project = (RulesProject) repositoryProject;
                    AProject historic = new AProject(project.getDesignRepository(),
                            project.getDesignFolderName(),
                            version);
                    openedSimilarToHistoric = userWorkspace.isOpenedOtherProject(historic);
                }
                if (openedSimilarToHistoric || userWorkspace.isOpenedOtherProject(repositoryProject)) {
                    WebStudioUtils.addErrorMessage(OPENED_OTHER_PROJECT);
                    // To avoid unnecessary request for the version when it's not needed (from
                    // getHasDependenciesForVersion())
                    version = null;
                    return null;
                }
            }

            if (repositoryProject.isOpened()) {
                studio.getModel().clearModuleInfo();
                repositoryProject.releaseMyLock();
            }

            repositoryProject.openVersion(version);
            openDependenciesIfNeeded();
            repositoryTreeState.refreshSelectedNode();
            resetStudioModel();
            // To avoid unnecessary request for the version when it's not needed (from getHasDependenciesForVersion())
            version = null;
        } catch (Exception e) {
            String msg = "Failed to open project version.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
        }
        return null;
    }

    private void closeProjectAndReleaseResources(UserWorkspaceProject repositoryProject) throws ProjectException {
        // We must release module info because it can hold jars.
        // We cannot rely on studio.getProject() to determine if closing project is compiled inside studio.getModel()
        // because project could be changed or cleared before (See studio.reset() usages). Also that project can be
        // a dependency of other. That's why we must always clear moduleInfo when closing a project.
        studio.getModel().clearModuleInfo();
        repositoryProject.close();
    }

    public void setProjectVersion(String version) {
        this.version = version;
        openDependencies = true;
        openProjectVersion();
    }

    public String refreshTree() {
        repositoryTreeState.invalidateTree();
        repositoryTreeState.invalidateSelection();
        resetStudioModel();
        return null;
    }

    public String selectDeploymentProject() {
        String repositoryId = WebStudioUtils.getRequestParameter("repositoryId");
        String projectName = WebStudioUtils.getRequestParameter("projectName");
        setRepositoryId(repositoryId);
        selectProject(projectName, repositoryTreeState.getDeploymentRepository());
        return null;
    }

    private void selectProject(String projectName, TreeRepository root) {
        for (TreeNode node : root.getChildNodes()) {
            if (trySelectProject(projectName, node)) {
                break;
            }
        }
    }

    private boolean trySelectProject(String projectName, TreeNode node) {
        if (node instanceof TreeProjectGrouping) {
            for (TreeNode child : node.getChildNodes()) {
                if (trySelectProject(projectName, child)) {
                    return true;
                }
            }
        } else if (node instanceof TreeProject || node instanceof TreeDProject) {
            if (node.getData().getName().equals(projectName) && repositoryId
                    .equals(node.getData().getRepository().getId())) {
                repositoryTreeState.setSelectedNode(node);
                return true;
            }
        }
        return false;
    }

    public String selectRulesProject() {
        String repositoryId = WebStudioUtils.getRequestParameter("repositoryId");
        String projectName = WebStudioUtils.getRequestParameter("projectName");
        setRepositoryId(repositoryId);
        selectProject(projectName, repositoryTreeState.getRulesRepository());
        return null;
    }

    public void setRulesProject(String businessName) {
        // Find first found project with a given business name in current repository in any path.
        findProjectNodeToOpen(businessName, repositoryTreeState.getRulesRepository().getChildNodes());
    }

    private boolean findProjectNodeToOpen(String businessName, List<TreeNode> nodes) {
        if (nodes == null) {
            return false;
        }
        for (TreeNode node : nodes) {
            if (node instanceof TreeProject) {
                RulesProject project = (RulesProject) node.getData();
                if (project.getBusinessName().equals(businessName) && repositoryId
                        .equals(project.getRepository().getId())) {
                    repositoryTreeState.setSelectedNode(node);
                    return true;
                }
            } else if (node instanceof TreeProjectGrouping) {
                boolean found = findProjectNodeToOpen(businessName, node.getChildNodes());
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }

    public void uploadListener(FileUploadEvent event) {
        try {
            ProjectFile file = new ProjectFile(event.getUploadedFile());
            uploadedFiles.add(file);
            String fileName = file.getName();

            setFileName(fileName);

            if (fileName.contains(".")) {
                if (!FileTypeHelper.isPossibleOpenAPIFile(fileName)) {
                    setProjectName(fileName.substring(0, fileName.lastIndexOf('.')));
                } else {
                    setProjectName("");
                }
                if (FileTypeHelper.isZipFile(fileName)) {
                    Charset charset = zipCharsetDetector.detectCharset(new ZipFromProjectFile(file));

                    if (charset == null) {
                        log.warn("Cannot detect a charset for the zip file");
                        charset = StandardCharsets.UTF_8;
                    }

                    ProjectDescriptor projectDescriptor = ZipProjectDescriptorExtractor
                            .getProjectDescriptorOrNull(file, zipFilter, charset);
                    if (projectDescriptor != null) {
                        setProjectName(projectDescriptor.getName());
                    }
                    setCreateProjectComment(getDesignRepoComments().createProject(getProjectName()));
                }
            } else {
                setProjectName(fileName);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage("Error occurred during uploading file.", e.getMessage());
        }
    }
    
    public void loadTagsFromUploadedFile() throws IOException {
        boolean tagsAreReadFromProject = false;
        ProjectFile file = getLastUploadedFile();
        if (file != null && FileTypeHelper.isZipFile(file.getName())) {
            try (ZipInputStream zipInputStream = new ZipInputStream(file.getInput())) {
                for (ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry()) {
                    if (RulesProjectTags.TAGS_FILE_NAME.equals(entry.getName())) {
                        var readTags = new HashMap<String, String>();
                        try {
                            PropertiesUtils.load(zipInputStream, readTags::put);
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                        projectTagsBean.initTagsFromPreexisting(readTags);
                        tagsAreReadFromProject = true;
                        break;
                    }
                }
            }
        }
        if (! tagsAreReadFromProject) {
            projectTagsBean.clearTags();
        }
        
    }

    /**
     * Remove uploaded files.
     *
     * @param fileNames file names split by '\n' symbol. If empty, all files will be removed.
     */
    public void setFileNamesToRemove(String fileNames) {
        if (fileNames.isEmpty()) {
            clearUploadedFiles();
        } else {
            List<String> toRemove = Arrays.asList(fileNames.split("\n"));
            for (Iterator<ProjectFile> iterator = uploadedFiles.iterator(); iterator.hasNext(); ) {
                ProjectFile file = iterator.next();
                if (toRemove.contains(file.getName())) {
                    file.destroy();
                    iterator.remove();
                }
            }
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

    public void setFolderName(String folderName) {
        this.folderName = StringUtils.trim(folderName);
    }

    public void setVersionComment(String versionComment) {
        FileData fileData = repositoryTreeState.getSelectedNode().getData().getFileData();
        if (fileData != null) {
            fileData.setComment(versionComment);
        }
    }

    public String getRepositoryId() {
        return StringUtils.isBlank(repositoryId) ? NONE_REPO : repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = NONE_REPO.equals(repositoryId) ? null : repositoryId;
    }

    private CommentValidator getDesignCommentValidator(UserWorkspaceProject project) {
        Repository designRepository = project.getDesignRepository();
        String repositoryId = designRepository == null ? null : designRepository.getId();
        return getDesignCommentValidator(repositoryId);
    }

    private CommentValidator getDesignCommentValidator(String repositoryId) {
        return StringUtils.isEmpty(repositoryId) ? CommentValidator.forRepo("")
                : CommentValidator.forRepo(repositoryId);
    }

    private Comments getDesignRepoComments() {
        return StringUtils.isEmpty(repositoryId) ? getComments(Comments.DESIGN_CONFIG_REPO_ID)
                : getComments(repositoryId);
    }

    private Comments getComments(String repositoryId) {
        return allComments.computeIfAbsent(repositoryId, k -> new Comments(propertyResolver, k));
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = StringUtils.trim(newProjectName);
    }

    public void setProjectName(String newProjectName) {
        projectName = StringUtils.trim(newProjectName);
    }

    public void setProjectFolder(String projectFolder) {
        String folder = StringUtils.trimToEmpty(projectFolder).replace('\\', '/');
        if (folder.startsWith("/")) {
            folder = folder.substring(1);
        }
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
            WebStudioUtils.addErrorMessage("Cannot undelete project '" + project.getBusinessName() + "'.",
                    "Project is not marked for deletion.");
            return null;
        }

        try {
            String comment;
            if (project instanceof RulesProject && isUseCustomCommentForProject()) {
                comment = restoreProjectComment;
                if (!isValidComment(project, comment)) {
                    return null;
                }
            } else {
                Comments comments = getComments(project);
                comment = comments.restoreProject(project.getBusinessName());
            }
            project.undelete(userWorkspace.getUser(), comment);
            repositoryTreeState.refreshSelectedNode();
            workspaceManager.refreshWorkspaces();
            resetStudioModel();
        } catch (Exception e) {
            String msg = "Cannot undelete project '" + project.getBusinessName() + "'.";
            log.error(msg, e);
            WebStudioUtils.addErrorMessage(msg, e.getMessage());
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
            WebStudioUtils.addInfoMessage("File was successfully updated.");
        } else {
            WebStudioUtils.addErrorMessage(errorMessage, "Error occurred during uploading file. " + errorMessage);
        }

        /* Clear the load form */
        clearForm();

        return null;
    }

    public String upload() {
        String errorMessage = uploadProject();
        if (errorMessage == null) {
            try {
                RulesProject createdProject = userWorkspace.getProject(repositoryId, projectName);
                repositoryTreeState.addRulesProjectToTree(createdProject);
                selectProject(createdProject.getName(), repositoryTreeState.getRulesRepository());
                resetStudioModel();
                WebStudioUtils.addInfoMessage("Project was created successfully.");
            } catch (Exception e) {
                WebStudioUtils.addErrorMessage(e.getMessage());
            }
        }

        /* Clear the load form */
        clearForm();

        return null;
    }

    public String createProjectWithFiles() {
        try {
            String comment;
            if (StringUtils.isNotBlank(createProjectComment)) {
                comment = createProjectComment;
            } else {
                comment = getDesignRepoComments().createProject(projectName);
            }
            String errorMessage = validateCreateProjectParams(comment);
            if (errorMessage != null) {
                WebStudioUtils.addErrorMessage(errorMessage);
                return errorMessage;
            }
            if (uploadedFiles.isEmpty()) {
                errorMessage = "There are no uploaded files.";
                WebStudioUtils.addErrorMessage(errorMessage);
                return errorMessage;
            }
            Map<String, String> tags = projectTagsBean.saveTagsTypesAndGetTags();
            final ProjectUploader uploader = new ProjectUploader(repositoryId,
                    uploadedFiles,
                    projectName,
                    projectFolder,
                    userWorkspace,
                    aclServiceProvider.getDesignRepoAclService(),
                    comment,
                    zipFilter,
                    zipCharsetDetector,
                    modelsPath,
                    algorithmsPath,
                    modelsModuleName,
                    algorithmsModuleName,
                    tags);
            RulesProject uploadedProject;
            try {
                uploadedProject = uploader.uploadProject();
            } catch (ProjectException e) {
                WebStudioUtils.addErrorMessage(e.getMessage());
                return e.getMessage();
            }
            try {
                repositoryTreeState.addRulesProjectToTree(uploadedProject);
                selectProject(uploadedProject.getName(), repositoryTreeState.getRulesRepository());
                resetStudioModel();
                WebStudioUtils.addInfoMessage("Project was created successfully.");
                return null;
            } catch (Exception e) {
                WebStudioUtils.addErrorMessage(e.getMessage());
                return e.getMessage();
            }
        } finally {
            /* Clear the load form */
            clearForm();
        }
    }

    private void clearForm() {
        this.setFileName(null);
        this.setProjectName(null);
        this.setProjectFolder("");
        this.setCreateProjectComment(null);
        clearUploadedFiles();
    }

    private String uploadAndAddFile() {
        if (!NameChecker.checkName(fileName)) {
            return "File name '" + fileName + "' is invalid. " + NameChecker.BAD_NAME_MSG;
        }

        try {
            AProjectFolder node = (AProjectFolder) repositoryTreeState.getSelectedNode().getData();

            ProjectFile lastUploadedFile = getLastUploadedFile();
            if (lastUploadedFile == null) {
                return "Upload the file";
            }
            var repositoryAclService = node
                    .getProject() instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                    : aclServiceProvider.getDesignRepoAclService();
            if (!repositoryAclService.isGranted(node, List.of(AclPermission.ADD))) {
                throw new Message(String.format("There is no permission for creating '%s/%s' file.",
                        ProjectArtifactUtils.extractResourceName(node),
                        fileName));
            }
            AProjectResource addedFileResource = node.addResource(fileName, lastUploadedFile.getInput());
            TreeNode t = repositoryTreeState.getSelectedNode();
            Stack<AProjectFolder> projectFolders = new Stack<>();
            while (t.getData() instanceof AProjectFolder && !repositoryAclService.hasAcl(t.getData())) {
                projectFolders.push((AProjectFolder) t.getData());
                t = t.getParent();
            }
            while (!projectFolders.isEmpty()) {
                AProjectFolder p = projectFolders.pop();
                if ((node.getProject() instanceof ADeploymentProject || !repositoryAclService
                        .hasAcl(p)) && !repositoryAclService.createAcl(p, AclPermissionsSets.NEW_FILE_PERMISSIONS, true)) {
                    String message = String.format("Granting permissions to a new folder '%s' is failed.",
                            ProjectArtifactUtils.extractResourceName(p));
                    WebStudioUtils.addErrorMessage(message);
                }
            }
            if ((node.getProject() instanceof ADeploymentProject || !repositoryAclService
                    .hasAcl(addedFileResource)) && !repositoryAclService
                    .createAcl(addedFileResource, AclPermissionsSets.NEW_FILE_PERMISSIONS, true)) {
                String message = String.format("Granting permissions to a new file '%s' is failed.",
                        ProjectArtifactUtils.extractResourceName(addedFileResource));
                WebStudioUtils.addErrorMessage(message);
            }
            repositoryTreeState.addNodeToTree(repositoryTreeState.getSelectedNode(), addedFileResource);

            registerInProjectDescriptor(addedFileResource);

            clearUploadedFiles();
        } catch (Exception e) {
            /*
             * If an error is IOException then an error will not be written to the console. This error throw when upload
             * file is exist in the upload folder
             */
            if (e.getCause() == null || e.getCause().getClass() != IOException.class) {
                log.error("Error adding a file to user workspace.", e);
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
                    IProjectDescriptorSerializer serializer = projectDescriptorSerializerFactory
                            .getSerializer(selectedProject);

                    AProjectResource resource = (AProjectResource) projectDescriptorArtifact;
                    if (!aclServiceProvider.getDesignRepoAclService().isGranted(resource, List.of(AclPermission.EDIT))) {
                        throw new Message(String.format("There is no permission for modifying '%s' file.",
                                ProjectArtifactUtils.extractResourceName(resource)));
                    }
                    content = resource.getContent();
                    ProjectDescriptor projectDescriptor = serializer.deserialize(content);
                    String modulePath = addedFileResource.getArtefactPath().withoutFirstSegment().getStringValue();
                    while (modulePath.charAt(0) == '/') {
                        modulePath = modulePath.substring(1);
                    }
                    Module module = new Module();
                    module.setName(FileUtils.getBaseName(fileName));
                    module.setRulesRootPath(new PathEntry(modulePath));
                    ProjectDescriptorManager descriptorManager = new ProjectDescriptorManager();
                    if (!descriptorManager.isCoveredByWildcardModule(projectDescriptor, module)) {
                        projectDescriptor.getModules().add(module);
                    }
                    String xmlString = serializer.serialize(projectDescriptor);
                    InputStream newContent = IOUtils.toInputStream(xmlString);
                    resource.setContent(newContent);
                }
            } catch (ProjectException | JAXBException | IOException ex) {
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
            var repositoryAclService = node
                    .getProject() instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                    : aclServiceProvider.getDesignRepoAclService();
            if (!repositoryAclService.isGranted(node, List.of(AclPermission.EDIT))) {
                throw new Message(String.format("There is no permission for modifying '%s' file.",
                        ProjectArtifactUtils.extractResourceName(node)));
            }
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

    enum OpenAPIModule {
        MODEL,
        ALGORITHM
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
                    comment = getDesignRepoComments().createProject(projectName);
                }
                String pathForModels = extractPath(editModelsPath, modelsPath, OpenAPIModule.MODEL);
                String pathForAlgorithms = extractPath(editAlgorithmsPath, algorithmsPath, OpenAPIModule.ALGORITHM);
                Map<String, String> tags = projectTagsBean.saveTagsTypesAndGetTags();
                ProjectUploader projectUploader = new ProjectUploader(repositoryId,
                        uploadedItem,
                        projectName,
                        projectFolder,
                        userWorkspace,
                        aclServiceProvider.getDesignRepoAclService(),
                        comment,
                        zipFilter,
                        zipCharsetDetector,
                        pathForModels,
                        pathForAlgorithms,
                        modelsModuleName,
                        algorithmsModuleName,
                        tags);
                errorMessage = validateCreateProjectParams(comment);
                if (errorMessage == null) {
                    try {
                        projectUploader.uploadProject();
                        projectName = projectUploader.getCreatedProjectName();
                    } catch (ProjectException e) {
                        errorMessage = e.getMessage();
                    }
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
                        errorMessage = "Cannot detect a charset for the zip file";
                        throw new IllegalArgumentException(errorMessage);
                    }

                    ZipProjectDescriptorExtractor.getProjectDescriptorOrThrow(lastUploadedFile, zipFilter, charset);
                }
            }

        } catch (ProjectDescriptionException | IllegalArgumentException e) {
            WebStudioUtils.addWarnMessage("Warning: " + e.getMessage());
        } catch (Exception ignored) {
        }

        if (errorMessage == null) {
            clearUploadedFiles();
            clearOpenAPIFields();
        } else {
            WebStudioUtils.addErrorMessage(errorMessage);
        }

        return errorMessage;
    }

    private String extractPath(boolean editorWasEnabled, String current, OpenAPIModule moduleType) {
        String pathForModels;
        if (!editorWasEnabled) {
            if (moduleType == OpenAPIModule.MODEL) {
                if (StringUtils.isNotBlank(modelsModuleName)) {
                    pathForModels = openAPIEditorService.generateModulePath(modelsModuleName);
                } else {
                    pathForModels = propertyResolver.getProperty(OPENAPI_DEFAULT_DATA_MODULE_PATH);
                }
            } else {
                if (StringUtils.isNotBlank(algorithmsModuleName)) {
                    pathForModels = openAPIEditorService.generateModulePath(algorithmsModuleName);
                } else {
                    pathForModels = propertyResolver.getProperty(OPENAPI_DEFAULT_ALGORITHM_MODULE_PATH);
                }
            }
        } else {
            pathForModels = current;
        }
        return pathForModels;
    }

    public void clearUploadedFiles() {
        for (ProjectFile uploadedFile : uploadedFiles) {
            uploadedFile.destroy();
        }
        uploadedFiles.clear();
    }

    public void clearOpenAPIFields() {
        setEditModelsPath(false);
        setEditAlgorithmsPath(false);
        setAlgorithmsModuleName(propertyResolver.getProperty("openapi.default.algorithm.module.name"));
        setModelsModuleName(propertyResolver.getProperty("openapi.default.data.module.name"));
        setAlgorithmsPath(propertyResolver.getProperty(OPENAPI_DEFAULT_ALGORITHM_MODULE_PATH));
        setModelsPath(propertyResolver.getProperty(OPENAPI_DEFAULT_DATA_MODULE_PATH));
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

    public boolean canDelete(UserWorkspaceProject project) {
        try {
            if (project.isLocalOnly()) {
                // any user can delete own local project
                return true;
            }
            if (!aclServiceProvider.getDesignRepoAclService().isGranted(project, List.of(AclPermission.DELETE))) {
                return false;
            }
            boolean unlocked = !project.isLocked() || project.isLockedByUser(userWorkspace.getUser());
            if (!unlocked) {
                return false;
            }
            return isMainBranch(project) && !isCurrentBranchProtected(project);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean canDeleteBranch(UserWorkspaceProject project) {
        try {
            if (project.isLocalOnly()) {
                return false;
            }
            boolean unlocked = !project.isLocked() || project.isLockedByUser(userWorkspace.getUser());
            if (!unlocked) {
                return false;
            }
            if (isMainBranch(project) || isCurrentBranchProtected(project)) {
                return false;
            }
            for (AProjectArtefact artefact : project.getArtefacts()) {
                if (aclServiceProvider.getDesignRepoAclService().isGranted(artefact,
                        List.of(AclPermission.EDIT, AclPermission.DELETE, AclPermission.ADD))) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean isMainBranch(UserWorkspaceProject selectedProject) {
        boolean mainBranch = true;
        Repository designRepository = selectedProject.getDesignRepository();
        if (designRepository.supports().branches()) {
            String branch = selectedProject.getBranch();
            if (!((BranchRepository) designRepository).getBaseBranch().equals(branch)) {
                mainBranch = false;
            }
        }
        return mainBranch;
    }

    private boolean isCurrentBranchProtected(UserWorkspaceProject selectedProject) {
        Repository repo = selectedProject.getDesignRepository();
        if (repo != null && repo.supports().branches()) {
            return ((BranchRepository) repo).isBranchProtected(selectedProject.getBranch());
        }
        return false;
    }

    public boolean getCanUnlock() {
        return isGranted(UNLOCK_PROJECTS);
    }

    public boolean getCanUnlockDeployment() {
        return isGranted(UNLOCK_DEPLOYMENT);
    }

    public boolean getCanDeleteDeployment() {
        UserWorkspaceProject selectedProject = getSelectedProject();
        return getCanDeleteDeployment(selectedProject);
    }

    public boolean getCanDeleteDeployment(UserWorkspaceProject project) {
        if (project instanceof ADeploymentProject) {
            return aclServiceProvider.getDeployConfigRepoAclService().isGranted(project, List.of(AclPermission.DELETE)) && !project
                    .isBranchProtected() && (!project.isLocked() || project.isLockedByMe());
        }
        return false;
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

    public UserWorkspaceProject getSelectedProject() {
        AProjectArtefact artefact = getSelectedNode().getData();
        if (artefact instanceof UserWorkspaceProject) {
            return (UserWorkspaceProject) artefact;
        }
        return null;
    }

    /**
     * Determine show or not current project content is some page (Open Version dialog or Rules Deploy Configuration
     * tab).
     *
     * @return false if selected project is changed or true if {@link #selectCurrentProjectForOpen()} is invoked
     */
    public boolean isCurrentProjectSelected() {
        AProject selectedProject = getSelectedProject();
        if (currentProject == null || selectedProject == null || currentProject != selectedProject && !currentProject
                .getName()
                .equals(selectedProject.getName())) {
            currentProject = null;
            return false;
        }
        return currentProject != null;
    }

    /**
     * Mark (select) current project for open in some page (Open Version dialog or Rules Deploy Configuration tab)
     */
    public void selectCurrentProjectForOpen() {
        currentProject = getSelectedProject();
        if (currentProject == null || currentProject.getVersion() == null || currentProject.isLastVersion()) {
            version = null;
        } else {
            version = currentProject.getVersion().getVersionName();
        }
    }

    public void deleteRulesProjectListener(AjaxBehaviorEvent event) {
        String repositoryId = WebStudioUtils.getRequestParameter("repositoryId");
        final String projectName = WebStudioUtils.getRequestParameter("projectName");

        try {
            activeProjectNode = repositoryTreeState.findNodeById(repositoryTreeState.getRulesRepository(),
                    RepositoryUtils.getTreeNodeId(repositoryId, projectName));
        } catch (Exception e) {
            log.error("Cannot delete rules project '{}'.", projectName, e);
            WebStudioUtils.addErrorMessage("Failed to delete rules project.", e.getMessage());
        }
    }

    /**
     * Checks if design repository supports branches
     */
    public boolean isAnySupportsBranches() {
        return userWorkspace.getDesignTimeRepository()
                .getRepositories()
                .stream()
                .anyMatch(repository -> repository.supports().branches());
    }

    public boolean isSupportsBranches(String repositoryId) {
        try {
            Repository repository = userWorkspace.getDesignTimeRepository().getRepository(repositoryId);
            return repository != null && repository.supports().branches();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<Repository> getNonFlatRepositories() {
        return userWorkspace.getDesignTimeRepository()
                .getRepositories()
                .stream()
                .filter(r -> r.supports().mappedFolders())
                .collect(Collectors.toList());
    }

    @Deprecated
    public boolean isSupportsMappedFolders() {
        return repositoryId != null && isSupportsMappedFolders(repositoryId);
    }

    public boolean isSupportsMappedFolders(String repositoryId) {
        try {
            Repository repository = userWorkspace.getDesignTimeRepository().getRepository(repositoryId);
            return repository != null && repository.supports().mappedFolders();
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

            String businessNameBeforeSwitch = selectedProject.getBusinessName();

            selectedProject.setBranch(branch);
            if (selectedProject.getLastHistoryVersion() == null) {
                // move back to previous branch! Because the project is not present in new branch
                selectedProject.setBranch(previousBranch);
                log.warn(
                        "Current project does not exists in '{}' branch! Project branch was switched to the previous one",
                        branch);
            }

            if (opened) {
                if (selectedProject.isDeleted()) {
                    selectedProject.close();
                } else {
                    // Update files
                    ProjectHistoryService.deleteHistory(businessNameBeforeSwitch);
                    if (!userWorkspace.isOpenedOtherProject(selectedProject)) {
                        selectedProject.open();
                    }
                }
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
        if (!(selectedProject instanceof RulesProject) || !isSupportsBranches(
                selectedProject.getRepository().getId())) {
            return;
        }
        String branch = (String) value;
        BranchRepository repository = (BranchRepository) selectedProject.getDesignRepository();
        try {
            boolean exists = !repository.forBranch(branch)
                    .list(((RulesProject) selectedProject).getDesignFolderName())
                    .isEmpty();
            WebStudioUtils.validate(exists, "Current project does not exist in this branch.");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public List<String> getProjectBranches() {
        try {
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            if (!(selectedProject instanceof RulesProject)) {
                return Collections.emptyList();
            }

            List<String> branches = new ArrayList<>(((BranchRepository) userWorkspace.getDesignTimeRepository()
                    .getRepository(selectedProject.getRepository().getId()))
                    .getBranches(((RulesProject) selectedProject).getDesignFolderName()));
            String projectBranch = getProjectBranch();
            if (projectBranch != null && !branches.contains(projectBranch)) {
                branches.add(projectBranch);
                branches.sort(String.CASE_INSENSITIVE_ORDER);
            }

            return branches;
        } catch (AccessDeniedException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public TreeNode getSelectedNode() {
        TreeNode selectedNode = repositoryTreeState.getSelectedNode();
        return activeProjectNode != null && (selectedNode instanceof TreeRepository || selectedNode instanceof TreeProjectGrouping) ? activeProjectNode
                : selectedNode;
    }

    public void resetActiveProjectNode() {
        activeProjectNode = null;
    }

    public boolean isRenamed(RulesProject project) {
        if (repositoryTreeState.getErrorsContainer().hasErrors()) {
            return false;
        }

        return project != null && !getLogicalName(project).equals(project.getName());
    }

    public String getLogicalName(RulesProject project) {
        return project == null ? null : projectDescriptorResolver.getLogicalName(project);
    }

    public void commentValidator(FacesContext context, UIComponent toValidate, Object value) {
        String comment = (String) value;

        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (project instanceof RulesProject) {
            getDesignCommentValidator(project).validate(comment);
        } else if (project instanceof ADeploymentProject) {
            deployConfigCommentValidator.validate(comment);
        }
    }

    public List<String> getCommentParts(AProjectArtefact artefact, ProjectVersion version) {
        String comment = version.getVersionComment();
        if (artefact instanceof RulesProject) {
            repositoryId = ((RulesProject) artefact).getDesignRepository().getId();
            List<String> commentParts = getDesignRepoComments().getCommentParts(comment);
            if (commentParts.size() == 3) {
                String name = commentParts.get(1);
                if (repositoryTreeState.getProjectNodeByBusinessName(repositoryId, name) != null) {
                    return new ArrayList<>(commentParts);
                }
            }

        }

        return new ArrayList<>(Collections.singletonList(comment));
    }

    /**
     * Used when create a project.
     */
    public boolean isUseCustomComment() {
        if (StringUtils.isEmpty(repositoryId)) {
            return false;
        }
        return Boolean.parseBoolean(propertyResolver
                .getProperty(Comments.REPOSITORY_PREFIX + repositoryId + ".comment-template.use-custom-comments"));
    }

    /**
     * Used when delete/undelete/erase a project.
     */
    public boolean isUseCustomCommentForProject() {
        // Only projects are supported for now. Deploy configs can be supported in future.
        UserWorkspaceProject selectedProject = getSelectedProject();
        if (selectedProject == null) {
            return false;
        }
        Repository repository = selectedProject.getDesignRepository();
        if (repository == null) {
            return false;
        }
        repositoryId = repository.getId();
        boolean projectUseCustomComment = isUseCustomComment();
        return projectUseCustomComment && !selectedProject.isLocalOnly();
    }

    public String getCreateProjectComment() {
        return createProjectComment;
    }

    public String retrieveCreateProjectCommentTemplate() {
        return isUseCustomComment() ? getDesignRepoComments().getCreateProjectTemplate() : null;
    }

    public void setCreateProjectComment(String createProjectComment) {
        this.createProjectComment = createProjectComment;
    }

    public String getArchiveProjectComment() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (project == null && activeProjectNode.getData() instanceof UserWorkspaceProject) {
            project = (UserWorkspaceProject) activeProjectNode.getData();
        }
        Comments comments = getComments(project);
        return project == null ? StringUtils.EMPTY : comments.archiveProject(project.getBusinessName());
    }

    public void setArchiveProjectComment(String archiveProjectComment) {
        this.archiveProjectComment = archiveProjectComment;
    }

    public String getRestoreProjectComment() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (project == null && activeProjectNode.getData() instanceof UserWorkspaceProject) {
            project = (UserWorkspaceProject) activeProjectNode.getData();
        }
        Comments comments = getComments(project);
        return project == null ? StringUtils.EMPTY : comments.restoreProject(project.getBusinessName());
    }

    public void setRestoreProjectComment(String restoreProjectComment) {
        this.restoreProjectComment = restoreProjectComment;
    }

    public String getEraseProjectComment() {
        UserWorkspaceProject project = repositoryTreeState.getSelectedProject();
        if (project == null && activeProjectNode.getData() instanceof UserWorkspaceProject) {
            project = (UserWorkspaceProject) activeProjectNode.getData();
        }
        Comments comments = getComments(project);
        return project == null ? StringUtils.EMPTY : comments.eraseProject(project.getBusinessName());
    }

    public void setProjectVersionCacheManager(ProjectVersionCacheManager projectVersionCacheManager) {
        this.projectVersionCacheManager = projectVersionCacheManager;
    }

    public String getBusinessVersion(TreeProductProject version) {
        try {
            String businessVersion = projectVersionCacheManager
                    .getDesignBusinessVersionOfDeployedProject(version.getData().getProject());
            return businessVersion != null ? businessVersion : version.getVersionName();
        } catch (IOException e) {
            log.error("Error during getting project design version", e);
            return version.getVersionName();
        }
    }

    public boolean isShowFullPath() {
        return isGranted(Privileges.ADMIN);
    }

    public String getFullPath(AProjectArtefact artefact) {
        if (artefact == null) {
            return null;
        }
        var repositoryAclService = artefact instanceof ADeploymentProject ? aclServiceProvider.getDeployConfigRepoAclService()
                : aclServiceProvider.getDesignRepoAclService();
        return (artefact instanceof ADeploymentProject ? RepositoryAclServiceProvider.REPO_TYPE_DEPLOY_CONFIG
                : RepositoryAclServiceProvider.REPO_TYPE_DESIGN) + "/" + repositoryAclService
                .getFullPath(artefact);
    }

    public void setEraseProjectComment(String eraseProjectComment) {
        this.eraseProjectComment = eraseProjectComment;
    }

    public void setProjectDescriptorSerializerFactory(
            ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory) {
        this.projectDescriptorSerializerFactory = projectDescriptorSerializerFactory;
    }

    public void setZipCharsetDetector(ZipCharsetDetector zipCharsetDetector) {
        this.zipCharsetDetector = zipCharsetDetector;
    }

    public void setDeployConfigRepoComments(Comments deployConfigRepoComments) {
        this.deployConfigRepoComments = deployConfigRepoComments;
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @PostConstruct
    public void init() {
        customTemplatesResolver = new CustomTemplatesResolver(
                propertyResolver.getProperty(DynamicPropertySource.OPENL_HOME));
        String designRepoForDeployConfig = propertyResolver.getProperty(USE_REPOSITORY_FOR_DEPLOY_CONFIG);
        if (StringUtils.isBlank(designRepoForDeployConfig)) {
            deployConfigCommentValidator = CommentValidator.forRepo(Comments.DEPLOY_CONFIG_REPO_ID);
        } else {
            deployConfigCommentValidator = CommentValidator.forRepo(designRepoForDeployConfig);
        }
        eraseFromRepository = false;
    }

    @PreDestroy
    public void destroy() {
        clearUploadedFiles();
    }

    public void openNewProjectDialog() {
        clearUploadedFiles();
        projectTagsBean.clearTags();

        List<Repository> repositories = userWorkspace.getDesignTimeRepository().getRepositories();
        if (repositories.size() == 1) {
            repositoryId = repositories.get(0).getId();
        } else {
            repositoryId = null;
        }
        localUploadController.setRepositoryId(repositoryId);

        projectName = null;
        projectFolder = "";
    }

    public void tryImportFromRepo() {
        if (isOpenLProjectInFolder()) {
            importFromRepo();
        }
    }
    private FolderMapper findAndValidateMappedRepository() throws IOException {
        Repository mappedRepo = userWorkspace.getDesignTimeRepository().getRepository(repositoryId);
        if (!mappedRepo.supports().mappedFolders()) {
            throw new IllegalArgumentException("Repository " + repositoryId + " has flat folder structure.");
        }
        Repository repository = ((FolderMapper) mappedRepo).getDelegate();
        FileData fileData = repository.check(projectFolder);
        if (fileData == null) {
            WebStudioUtils.addErrorMessage("Project doesn't exist in the path " + projectFolder + ".");
            clearForm();
            return null;
        }

        return (FolderMapper) mappedRepo;
    }

    public void readTagsFromImportedProject() {
        try {
            var mappedRepo = findAndValidateMappedRepository();
            if (mappedRepo == null) {
                return;
            }
            Repository repository = mappedRepo.getDelegate();
            
            var tagsFileNameBuilder = new StringBuilder(projectFolder);
            if (! projectFolder.endsWith("/")) {
                tagsFileNameBuilder.append("/");
            }
            tagsFileNameBuilder.append(RulesProjectTags.TAGS_FILE_NAME);
            
            var tagsFile = repository.read(tagsFileNameBuilder.toString());
            if (tagsFile != null) {
                Map<String, String> existingTags = new HashMap<>();
                try (InputStream projectTagsFileStream = tagsFile.getStream()) {
                    PropertiesUtils.load(projectTagsFileStream, existingTags::put);
                }
                projectTagsBean.initTagsFromPreexisting(existingTags);
            } else {
                projectTagsBean.clearTags();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage("Cannot read the project: " + e.getMessage());
            clearForm();
        }
    }
    
    public void importFromRepo() {
        String msg = validateImportFromRepoParams();
        if (msg != null) {
            WebStudioUtils.addErrorMessage(msg);
            clearForm();
            return;
        }

        try {
            var mappedRepo = findAndValidateMappedRepository();
            if (mappedRepo == null) {
                return;
            }
            mappedRepo.addMapping(projectFolder);

            workspaceManager.refreshWorkspaces();
            repositoryTreeState.invalidateTree();
            Optional<RulesProject> importedProject = userWorkspace.getProjectByPath(repositoryId, projectFolder);
            if (importedProject.isPresent()) {
                var project = importedProject.get();
                var tags = projectTagsBean.saveTagsTypesAndGetTags();
                var existingTags = project.getLocalTags();
                if (!existingTags.equals(tags)) {
                    project.saveTags(tags);
                }

                selectProject(project.getName(), repositoryTreeState.getRulesRepository());
            }

            resetStudioModel();
            WebStudioUtils.addInfoMessage("Project was imported successfully.");
            clearForm();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage("Cannot import the project: " + e.getMessage());
            clearForm();
        }
    }

    public String validateImportFromRepoParams() {
        String msg = validateRepositoryId();
        if (msg != null) {
            return msg;
        }

        if (StringUtils.isBlank(projectFolder)) {
            WebStudioUtils.addErrorMessage("Path must not be empty.");
        }

        msg = validateProjectFolder();
        return msg;
    }

    public void changeModelsFilePathInputState() {
        setEditModelsPath(!editModelsPath);
        setModelsPath(editModelsPath ? openAPIEditorService.generateModulePath(modelsModuleName)
                : propertyResolver.getProperty(OPENAPI_DEFAULT_DATA_MODULE_PATH));
        setAlgorithmsPath(openAPIEditorService.generateModulePath(algorithmsModuleName));
    }

    public void changeAlgorithmsFilePathInputState() {
        setEditAlgorithmsPath(!editAlgorithmsPath);
        setAlgorithmsPath(editAlgorithmsPath ? openAPIEditorService.generateModulePath(algorithmsModuleName)
                : propertyResolver.getProperty(OPENAPI_DEFAULT_ALGORITHM_MODULE_PATH));
        setModelsPath(openAPIEditorService.generateModulePath(modelsModuleName));
    }

    public boolean getEraseFromRepository() {
        return eraseFromRepository;
    }

    public void setEraseFromRepository(boolean eraseFromRepository) {
        this.eraseFromRepository = eraseFromRepository;
    }

    public void setModelsModuleName(String modelsModuleName) {
        this.modelsModuleName = modelsModuleName;
    }

    public String getModelsModuleName() {
        return modelsModuleName;
    }

    public String getAlgorithmsModuleName() {
        return algorithmsModuleName;
    }

    public void setAlgorithmsModuleName(String algorithmsModuleName) {
        this.algorithmsModuleName = algorithmsModuleName;
    }

    public String getModelsPath() {
        return openAPIEditorService.generateModulePath(modelsModuleName);
    }

    public void setModelsPath(String modelsPath) {
        this.modelsPath = modelsPath;
    }

    public String getAlgorithmsPath() {
        return openAPIEditorService.generateModulePath(algorithmsModuleName);
    }

    public void setAlgorithmsPath(String algorithmsPath) {
        this.algorithmsPath = algorithmsPath;
    }

    public boolean isEditModelsPath() {
        return editModelsPath;
    }

    public void setEditModelsPath(boolean editModelsPath) {
        this.editModelsPath = editModelsPath;
    }

    public boolean isEditAlgorithmsPath() {
        return editAlgorithmsPath;
    }

    public void setEditAlgorithmsPath(boolean editAlgorithmsPath) {
        this.editAlgorithmsPath = editAlgorithmsPath;
    }

    public boolean isMergedIntoMain(UserWorkspaceProject project) {
        if (project == null || project.getDesignRepository() == null || !project.getDesignRepository()
                .supports()
                .branches()) {
            return false;
        }

        try {
            BranchRepository repository = ((BranchRepository) project.getDesignRepository());
            return repository.isMergedInto(project.getBranch(), repository.getBaseBranch());
        } catch (AccessDeniedException e) {
            return false;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public String getMainBranch(UserWorkspaceProject project) {
        if (project == null || project.getDesignRepository() == null || !project.getDesignRepository()
                .supports()
                .branches()) {
            return null;
        }
        return ((BranchRepository) project.getDesignRepository()).getBaseBranch();
    }

    public void checkBranchIsDeletable() {
        UserWorkspaceProject project = getSelectedProject();
        String message = "Branch cannot be deleted: ";

        if (project == null) {
            WebStudioUtils.addErrorMessage(message + " project associated with this branch is absent.");
        } else if (project.isLocalOnly()) {
            WebStudioUtils.addErrorMessage(message + " project associated with this branch is local.");
        } else if (project.isDeleted()) {
            WebStudioUtils.addErrorMessage(message + " project associated with this branch is archived.");
        } else {
            if (!hasPermissionsForArtefactsInProject(project)) {
                WebStudioUtils.addErrorMessage(message + " access denied.");
            }
        }
    }

    private boolean hasPermissionsForArtefactsInProject(UserWorkspaceProject project) {
        if (project == null) {
            return false;
        }
        for (AProjectArtefact artefact : project.getArtefacts()) {
            if (aclServiceProvider.getDesignRepoAclService().isGranted(artefact,
                    List.of(AclPermission.EDIT, AclPermission.DELETE, AclPermission.ADD))) {
                return true;
            }
        }
        return false;
    }

    public boolean isBranchDeletable() {
        UserWorkspaceProject project = getSelectedProject();
        boolean f = project != null && !project.isLocalOnly() && !project.isDeleted();
        if (!f) {
            return false;
        }
        return hasPermissionsForArtefactsInProject(project);
    }

    public boolean isTagsAreConfigured() {
        return !tagTypeService.getAllTagTypes().isEmpty();
    }

    public void forceUpdateVersionsBean() {
        setVersion(null);
    }

}
