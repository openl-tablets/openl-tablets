package org.openl.rules.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.ValidationException;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.rest.ProjectHistoryService;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.ui.tree.view.Profile;
import org.openl.rules.ui.tree.view.RulesTreeView;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentRepositoriesUtil;
import org.openl.rules.webstudio.web.repository.merge.ConflictUtils;
import org.openl.rules.webstudio.web.repository.merge.MergeConflictInfo;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.ZipProjectDescriptorExtractor;
import org.openl.rules.webstudio.web.repository.upload.zip.DefaultZipEntryCommand;
import org.openl.rules.webstudio.web.repository.upload.zip.FilePathsCollector;
import org.openl.rules.webstudio.web.repository.upload.zip.ProjectDescriptionException;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipFromProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipWalker;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

/**
 * TODO Remove JSF dependency TODO Separate user session from app session TODO Move settings to separate UserSettings
 * class
 *
 * @author snshor
 */
public class WebStudio implements DesignTimeRepositoryListener {

    private final Logger log = LoggerFactory.getLogger(WebStudio.class);

    private static final Comparator<Module> MODULES_COMPARATOR = Comparator.comparing(Module::getName,
            String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<ProjectDescriptor> PROJECT_DESCRIPTOR_COMPARATOR = Comparator
            .comparing(ProjectDescriptor::getName, String.CASE_INSENSITIVE_ORDER);

    public static final String RULES_TREE_VIEW = "rules.tree.view";
    public static final String RULES_TREE_VIEW_DEFAULT = "rules.tree.view.default";
    public static final String TABLE_VIEW = "table.view";
    public static final String TABLE_FORMULAS_SHOW = "table.formulas.show";
    public static final String TEST_TESTS_PERPAGE = "test.tests.perpage";
    public static final String TEST_FAILURES_ONLY = "test.failures.only";
    public static final String TEST_FAILURES_PERTEST = "test.failures.pertest";
    public static final String TEST_RESULT_COMPLEX_SHOW = "test.result.complex.show";
    public static final String TRACE_REALNUMBERS_SHOW = "trace.realNumbers.show";

    private final WebStudioLinkBuilder linkBuilder = new WebStudioLinkBuilder(this);

    private String workspacePath;
    private String tableUri;
    private final ProjectModel model;
    private final ProjectResolver projectResolver;
    private Map<String, List<ProjectDescriptor>> projects = null;

    private RulesTreeView treeView;
    private String tableView;
    private boolean showRealNumbers;
    private boolean showFormulas;
    private int testsPerPage;
    private boolean testsFailuresOnly;
    private int testsFailuresPerTest;
    private boolean showComplexResult;

    private String currentRepositoryId;
    private ProjectDescriptor currentProject;
    private Module currentModule;

    private boolean collapseProperties = true;

    private final UserSettingManagementService userSettingsManager;

    private boolean needRestart = false;
    private boolean forcedCompile = true;
    private boolean needCompile = true;
    private boolean manualCompile = false;
    private final Map<String, Object> externalProperties;

    private final List<ProjectFile> uploadedFiles = new ArrayList<>();

    private final RulesUserSession rulesUserSession;

    private final PropertyResolver propertyResolver;

    private final RepositoryAclService designRepositoryAclService;

    private final SimpleRepositoryAclService productionRepositoryAclService;

    private final DeploymentManager deploymentManager;

    private final Authentication authentication;
    private final ProjectDescriptorArtefactResolver pdArtefactResolver;
    private final PathFilter zipFilter;
    private final ZipCharsetDetector zipCharsetDetector;
    private final ProjectDescriptorSerializerFactory pdSerializerFactory;

    /**
     * Projects that are currently processed, for example saved. Projects's state can be in intermediate state, and it
     * can affect their modified status.
     */
    private final Set<String> frozenProjects = Collections.synchronizedSet(new HashSet<>());
    private boolean needRedirect;

    public WebStudio(RulesUserSession rulesUserSession,
                     TestSuiteExecutor testSuiteExecutor,
                     UserSettingManagementService userSettingManagementService,
                     RepositoryAclService designRepositoryAclService,
                     SimpleRepositoryAclService productionRepositoryAclService,
                     ProjectDescriptorArtefactResolver projectDescriptorArtefactResolver,
                     PathFilter zipFilter,
                     ZipCharsetDetector zipCharsetDetector,
                     ProjectDescriptorSerializerFactory projectDescriptorSerializerFactory,
                     PropertyResolver propertyResolver,
                     DeploymentManager deploymentManager

    ) {
        model = new ProjectModel(this, testSuiteExecutor);
        this.userSettingsManager = userSettingManagementService;
        this.designRepositoryAclService = designRepositoryAclService;
        this.productionRepositoryAclService = productionRepositoryAclService;
        this.pdArtefactResolver = projectDescriptorArtefactResolver;
        this.zipFilter = zipFilter;
        this.zipCharsetDetector = zipCharsetDetector;
        this.pdSerializerFactory = projectDescriptorSerializerFactory;
        this.rulesUserSession = rulesUserSession;
        this.propertyResolver = propertyResolver;
        this.deploymentManager = deploymentManager;
        authentication = SecurityContextHolder.getContext().getAuthentication();
        initWorkspace(rulesUserSession.getUserWorkspace());
        initUserSettings();
        projectResolver = ProjectResolver.getInstance();
        externalProperties = new HashMap<>();
        copyExternalProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY);
        copyExternalProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY);
        copyExternalProperty(OpenLSystemProperties.DISPATCHING_VALIDATION);
    }

    public RepositoryAclService getDesignRepositoryAclService() {
        return designRepositoryAclService;
    }

    private void copyExternalProperty(String key) {
        String value = Props.text(key);
        externalProperties.put(key, value);
    }

    private void initWorkspace(UserWorkspace userWorkspace) {
        if (userWorkspace == null) {
            return;
        }

        workspacePath = userWorkspace.getLocalWorkspace().getLocation().getAbsolutePath();
        userWorkspace.getDesignTimeRepository().addListener(this);
    }

    private void initUserSettings() {
        String userName = rulesUserSession.getUserName();

        treeView = getTreeView(userSettingsManager.getStringProperty(userName, RULES_TREE_VIEW_DEFAULT));
        tableView = userSettingsManager.getStringProperty(userName, TABLE_VIEW);
        showFormulas = userSettingsManager.getBooleanProperty(userName, TABLE_FORMULAS_SHOW);
        testsPerPage = userSettingsManager.getIntegerProperty(userName, TEST_TESTS_PERPAGE);
        testsFailuresOnly = userSettingsManager.getBooleanProperty(userName, TEST_FAILURES_ONLY);
        testsFailuresPerTest = userSettingsManager.getIntegerProperty(userName, TEST_FAILURES_PERTEST);
        showComplexResult = userSettingsManager.getBooleanProperty(userName, TEST_RESULT_COMPLEX_SHOW);
        showRealNumbers = userSettingsManager.getBooleanProperty(userName, TRACE_REALNUMBERS_SHOW);
    }

    public RulesTreeView[] getTreeViews() {
        return Profile.TREE_VIEWS;
    }

    public void saveProject(HttpSession session) {
        RulesProject project = null;
        try {
            ConflictUtils.removeMergeConflict();
            project = getCurrentProject();
            if (project == null) {
                return;
            }
            saveProject(project);
        } catch (Exception e) {
            String msg;
            Throwable cause = e.getCause();
            if (cause instanceof FileNotFoundException) {
                if (e.getMessage().contains(".xls")) {
                    msg = "Failed to save the project. Close the module Excel file and try again.";
                } else {
                    msg = "Failed to save the project because some resources are used";
                }
                log.debug(msg, e);
            } else if (cause instanceof MergeConflictException) {
                MergeConflictInfo info = new MergeConflictInfo((MergeConflictException) cause, project);
                ConflictUtils.saveMergeConflict(info);
                msg = "Failed to save the project because of merge conflict.";
                log.debug(msg, e);
                return;
            } else {
                msg = "Failed to save the project. See logs for details.";
                log.error(msg, e);
            }

            throw new Message(msg);
        }
    }

    public boolean isMergeConflict() {
        return ConflictUtils.getMergeConflict() != null;
    }

    public boolean isRenamed(RulesProject project) {
        return project != null && !getLogicalName(project).equals(project.getName());
    }

    public String getLogicalName(RulesProject project) {
        return project == null ? null : pdArtefactResolver.getLogicalName(project);
    }

    public void saveProject(RulesProject project) throws ProjectException {
        InputStream content = null;
        try {
            String projectName = project.getName();
            freezeProject(projectName);
            String logicalName = getLogicalName(project);
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
            boolean renameProject = !logicalName.equals(project.getName());
            if (renameProject) {
                if (!project.getDesignRepository().supports().mappedFolders()) {
                    getModel().clearModuleInfo();

                    // Revert project name in rules.xml
                    var serializer = pdSerializerFactory.getSerializer(project);
                    AProjectResource artefact = (AProjectResource) project
                            .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                    content = artefact.getContent();
                    ProjectDescriptor projectDescriptor = serializer.deserialize(content);
                    projectDescriptor.setName(project.getName());
                    if (!designRepositoryAclService.isGranted(artefact, List.of(AclPermission.EDIT))) {
                        throw new Message(String.format("There is no permission for modifying '%s' file.",
                                ProjectArtifactUtils.extractResourceName(artefact)));
                    }
                    artefact.setContent(IOUtils.toInputStream(serializer.serialize(projectDescriptor)));
                    resetProjects();
                } else {
                    FileMappingData mappingData = project.getFileData().getAdditionalData(FileMappingData.class);
                    if (mappingData != null) {
                        mappingData
                                .setExternalPath(userWorkspace.getDesignTimeRepository().getRulesLocation() + logicalName);
                    }
                }
            }
            ProjectHistoryService.deleteHistory(projectName);
            project.save();
            Repository repository = project.getDesignRepository();
            if (repository.supports().branches()) {
                BranchRepository branchRepository = (BranchRepository) repository;
                if (!branchRepository.getBranch().equals(branchRepository.getBaseBranch())) {
                    // Rename only on base branch.
                    renameProject = false;
                }
            }
            if (renameProject) {
                if (repository.supports().mappedFolders()) {
                    LocalWorkspace localWorkspace = rulesUserSession.getUserWorkspace().getLocalWorkspace();
                    File repoRoot = localWorkspace.getRepository(project.getRepository().getId()).getRoot();
                    String prevPath = project.getFolderPath();
                    int index = prevPath.lastIndexOf('/');
                    String newPath = prevPath.substring(0, index + 1) + logicalName;
                    boolean renamed = new File(repoRoot, prevPath).renameTo(new File(repoRoot, newPath));
                    if (!renamed) {
                        log.warn("Cannot rename folder from {} to {}", prevPath, newPath);
                    }
                }
            }
            userWorkspace.refresh();
            if (model.isModified()) {
                // Project sources were modified while saving, probably conflicts resolved automatically
                // Require modules reload and recompilation
                resetProjects();
            }
        } catch (IOException | JAXBException e) {
            throw new ProjectException(e.getMessage(), e);
        } finally {
            releaseProject(project.getName());
            IOUtils.closeQuietly(content);
        }
    }

    public RulesProject getCurrentProject() {
        if (currentProject != null) {
            String projectFolder = currentProject.getProjectFolder().getFileName().toString();
            return getProject(currentRepositoryId, projectFolder);
        }
        return null;
    }

    public RulesDeploy getCurrentProjectRulesDeploy() {
        try {
            RulesProject currentProject = getCurrentProject();
            if (currentProject.hasArtefact(DeploymentManager.RULES_DEPLOY_XML)) {
                try {
                    AProjectArtefact artefact = currentProject.getArtefact(DeploymentManager.RULES_DEPLOY_XML);
                    if (artefact instanceof AProjectResource) {
                        try (InputStream content = ((AProjectResource) artefact).getContent()) {
                            IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();
                            return rulesDeploySerializer.deserialize(content);
                        }
                    }
                } catch (ProjectException ignore) {
                }
            }
            return null;
        } catch (IOException | JAXBException e) {
            if (StringUtils.isNotBlank(e.getMessage())) {
                throw new Message("Invalid Rules Deploy Configuration: " + e.getMessage());
            }
            throw new Message("Invalid Rules Deploy Configuration.");
        }
    }

    public ProjectJacksonObjectMapperFactoryBean getCurrentProjectJacksonObjectMapperFactoryBean() {
        var compiledOpenClass = getModel().getCompiledOpenClass();
        var objectMapperFactory = new ProjectJacksonObjectMapperFactoryBean();
        objectMapperFactory.setRulesDeploy(getCurrentProjectRulesDeploy());
        objectMapperFactory.setXlsModuleOpenClass((XlsModuleOpenClass) compiledOpenClass
                .getOpenClassWithErrors());
        ClassLoader classLoader = compiledOpenClass.getClassLoader();
        objectMapperFactory.setClassLoader(classLoader);
        return objectMapperFactory;
    }

    public RulesProject getProject(String repositoryId, String name) {
        UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
        try {
            return userWorkspace.getProject(repositoryId, name, false);
        } catch (ProjectException e) {
            return null;
        }
    }

    public ProjectDescriptor getCurrentProjectDescriptor() {
        return currentProject;
    }

    public Module getCurrentModule() {
        return currentModule;
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the ProjectResolver.
     */
    public ProjectResolver getProjectResolver() {
        return projectResolver;
    }

    public RulesTreeView getTreeView() {
        return treeView;
    }

    public String getTableView() {
        return tableView;
    }

    public void setTableView(String tableView) {
        this.tableView = tableView;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), TABLE_VIEW, tableView);
    }

    public boolean isShowHeader() {
        return tableView.equals(IXlsTableNames.VIEW_DEVELOPER);
    }

    public void setShowHeader(boolean showHeader) {
        setTableView(showHeader ? IXlsTableNames.VIEW_DEVELOPER : IXlsTableNames.VIEW_BUSINESS);
    }

    public ProjectModel getModel() {
        return model;
    }

    public String getTableUri() {
        return tableUri;
    }

    /**
     * Returns path on the file system to user workspace this instance of web studio works with.
     *
     * @return path to openL projects workspace, i.e. folder containing openL projects.
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    public synchronized List<ProjectDescriptor> getAllProjects() {
        List<ProjectDescriptor> allProjects = new ArrayList<>();
        getProjects().values().forEach(allProjects::addAll);
        allProjects.sort(PROJECT_DESCRIPTOR_COMPARATOR);
        return allProjects;
    }

    public synchronized Map<String, List<ProjectDescriptor>> getProjects() {
        if (projects == null) {
            try {
                projects = new HashMap<>();
                LocalWorkspace localWorkspace = rulesUserSession.getUserWorkspace().getLocalWorkspace();

                for (AProject project : localWorkspace.getProjects()) {
                    try {
                        String repoId = project.getRepository().getId();
                        List<ProjectDescriptor> projectDescriptors = projects.computeIfAbsent(repoId,
                                k -> new ArrayList<>());
                        File repoRoot = localWorkspace.getRepository(project.getRepository().getId()).getRoot();
                        File folder = new File(repoRoot, project.getFolderPath());
                        ProjectDescriptor resolvedDescriptor = projectResolver.resolve(folder);
                        if (resolvedDescriptor != null) {
                            resolvedDescriptor.getModules().sort(MODULES_COMPARATOR);
                            projectDescriptors.add(resolvedDescriptor);
                        }
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                projects = null;
                return Collections.emptyMap();
            }
        }
        return projects;
    }

    public void compile() {
        needCompile = true;
    }

    public synchronized void resetProjects() {
        doResetProjects();
    }

    private void doResetProjects() {
        forcedCompile = true;
        projects = null;
        rulesUserSession.getUserWorkspace().syncProjects();
        rulesUserSession.getUserWorkspace().refresh();
        model.resetSourceModified();
    }

    public synchronized void reset() {
        doResetProjects();
        currentModule = null;
        currentProject = null;
    }

    private void reset(ReloadType reloadType) {
        try {
            model.reset(reloadType, currentModule);
        } catch (Exception e) {
            log.error("Error when trying to reset studio model", e);
        }
    }

    public String getCurrentRepositoryType() {
        return new RepositoryConfiguration(currentRepositoryId, propertyResolver).getType();
    }

    public boolean isAutoCompile() {
        return Props.bool(AdministrationSettings.AUTO_COMPILE);
    }

    public boolean isManualCompileNeeded() {
        return !isAutoCompile() && needCompile;
    }

    public void invokeManualCompile() {
        manualCompile = true;
    }

    public synchronized void init(String repositoryId, String branchName, String projectName, String moduleName) {
        try {
            log.debug("Repository id='{}' Branch='{}'  Project='{}'  Module='{}'",
                    repositoryId,
                    branchName,
                    projectName,
                    moduleName);
            currentRepositoryId = repositoryId;
            ProjectDescriptor project = getProjectByName(currentRepositoryId, projectName);
            needRedirect = false;
            if (StringUtils.isNotBlank(projectName) && project == null) {
                // Not empty project name is requested but it's not found
                handleProjectNotFound();
                return;
            }
            // switch current project branch to the selected
            if (branchName != null && project != null) {
                String newProjectName = setProjectBranch(project, branchName);
                if (newProjectName != null) {
                    projectName = newProjectName;
                    needRedirect = true;
                }

                // reload project descriptor. Because it might be changed
                project = getProjectByName(currentRepositoryId, projectName);
                if (StringUtils.isNotBlank(projectName) && project == null) {
                    // Not empty project name is requested but it's not found
                    handleProjectNotFound();
                    return;
                }
            }
            Module module = getModule(project, moduleName);
            if (StringUtils.isNotBlank(moduleName) && module == null) {
                // Not empty module name is requested but it's not found
                handleProjectNotFound();
                return;
            }
            boolean anotherModuleOpened = currentModule != module;
            boolean anotherProjectOpened = !(model.getModuleInfo() != null && project != null && model.getModuleInfo()
                    .getProject()
                    .getName()
                    .equals(project.getName()));
            currentModule = module;
            currentProject = project;
            if (currentProject != null) {
                // Validate the permission to read the project. If the project has read permission, then all modules of
                // the project has the read permission too.
                RulesProject rulesProject = getProject(repositoryId,
                        currentProject.getProjectFolder().getFileName().toString());
                if (rulesProject != null && module != null) {
                    log.debug(
                            "Check permission for repository id '{}', project path in the repository '{}', module path in the project '{}'.",
                            repositoryId,
                            rulesProject.getLocalFolderName(),
                            module.getRulesRootPath().getPath());
                } else {
                    if (rulesProject != null) {
                        log.debug("Check permission for repository id '{}', project path in the repository '{}'.",
                                repositoryId,
                                rulesProject.getLocalFolderName());
                    }
                }
            }
            if (module != null && (needCompile && (isAutoCompile() || manualCompile) || forcedCompile || anotherModuleOpened || anotherProjectOpened)) {
                if (forcedCompile) {
                    reset(ReloadType.FORCED);
                } else if (needCompile) {
                    reset(ReloadType.SINGLE);
                } else if (anotherProjectOpened) {
                    model.setModuleInfo(module, ReloadType.SINGLE);
                } else if (anotherModuleOpened) {
                    model.setModuleInfo(module, ReloadType.NO);
                } else {
                    model.setModuleInfo(module);
                }
                model.buildProjectTree(); // Reason: tree should be built
                // before accessing the ProjectModel.
                // Is is related to UI: rendering of
                // frames is asynchronous and we
                // should build tree before the
                // 'content' frame
                needCompile = false;
                forcedCompile = false;
                manualCompile = false;
            }
        } catch (Exception e) {
            log.error("Failed initialization. Project='{}'  Module='{}'", projectName, moduleName, e);
            handleProjectNotFound();
        }
    }

    private void handleProjectNotFound() {
        var facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
            facesContext.responseComplete();
        } else {
            // faces context may not be available in OpenL Studio if it's used from REST API
            throw new NotFoundException("project.identifier.message");
        }
    }

    public boolean isNeedRedirect() {
        return needRedirect;
    }

    public Module getModule(ProjectDescriptor project, final String moduleName) {
        if (project == null) {
            return null;
        }
        return CollectionUtils.findFirst(project.getModules(),
                module -> module.getName() != null && module.getName().equals(moduleName));
    }

    public String updateModule() {
        ProjectFile uploadedFile = getLastUploadedFile();
        if (uploadedFile == null) {
            // TODO Display message - e.getMessage()
            return null;
        }

        InputStream stream = null;
        try {
            tryLockProject();

            stream = uploadedFile.getInput();

            Module module = getCurrentModule();
            File sourceFile = module.getRulesPath().toFile();

            ProjectHistoryService.init(model.getHistoryStoragePath(), sourceFile);
            LocalRepository repository = rulesUserSession.getUserWorkspace()
                    .getLocalWorkspace()
                    .getRepository(currentRepositoryId);

            File projectFolder = getCurrentProjectDescriptor().getProjectFolder().toFile();
            String relativePath = getRelativePath(projectFolder, sourceFile);
            FileData data = new FileData();
            data.setName(projectFolder.getName() + "/" + relativePath);
            repository.save(data, stream);
            ProjectHistoryService.save(model.getHistoryStoragePath(), sourceFile);
        } catch (FileNotFoundException e) {
            log.debug("An error occurred during the module update. Close the module Excel file and try again.", e);
            throw new IllegalStateException(
                    "An error occurred during the module update. Close the module Excel file and try again.",
                    e);
        } catch (Exception e) {
            log.error("Error updating file in user workspace.", e);
            throw new IllegalStateException("Error while updating the module.", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        model.resetSourceModified(); // Because we rewrite a file in the
        // workspace
        compile();
        clearUploadedFiles();

        return null;
    }

    public synchronized String updateProject() {
        ProjectFile lastUploadedFile = getLastUploadedFile();
        if (lastUploadedFile == null) {
            // TODO Replace exceptions with FacesUtils.addErrorMessage()
            throw new IllegalArgumentException("No file has been uploaded. Upload a .zip file to update the project.");
        }
        if (!FileTypeHelper.isZipFile(FilenameUtils.getName(lastUploadedFile.getName()))) {
            // TODO Replace exceptions with FacesUtils.addErrorMessage()
            throw new IllegalArgumentException("Wrong filename extension. Select a .zip file to upload.");
        }
        ProjectDescriptor projectDescriptor;
        try {
            initProjectHistory();
            tryLockProject();

            projectDescriptor = getCurrentProjectDescriptor();

            List<String> filesInProject = getFilesInProject(zipFilter);
            var charset = zipCharsetDetector.detectCharset(new ZipFromProjectFile(lastUploadedFile), filesInProject);
            if (charset == null) {
                throw new Message("Cannot detect a charset for the zip file");
            }

            String errorMessage = validateUploadedFiles(lastUploadedFile, zipFilter, projectDescriptor, charset);
            if (errorMessage != null) {
                // TODO Replace exceptions with FacesUtils.addErrorMessage()
                throw new Message(errorMessage);
            }

            final CommonUser user = rulesUserSession.getUserWorkspace().getUser();
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
            final LocalRepository repository = userWorkspace.getLocalWorkspace().getRepository(currentRepositoryId);
            // project folder is not the same as project name
            final String projectPath = projectDescriptor.getProjectFolder().getFileName().toString();

            // Release resources that can be deleted or replaced
            getModel().clearModuleInfo();

            ZipWalker zipWalker = new ZipWalker(lastUploadedFile, zipFilter, charset);

            FilePathsCollector filesCollector = new FilePathsCollector();
            zipWalker.iterateEntries(filesCollector);
            List<String> filesInZip = filesCollector.getFilePaths();

            final File projectFolder = projectDescriptor.getProjectFolder().toFile();
            Collection<File> files = getProjectFiles(projectFolder, zipFilter);
            RulesProject rulesProject = getCurrentProject();

            List<FileData> absentResources = new ArrayList<>();
            // Delete absent files in project
            for (File file : files) {
                String relative = getRelativePath(projectFolder, file);
                if (!filesInZip.contains(relative)) {
                    if (!designRepositoryAclService.isGranted(rulesProject.getArtefact(relative),
                            List.of(AclPermission.DELETE))) {
                        throw new Message(String.format("There is no permission for deleting '%s' file.",
                                projectPath + "/" + relative));
                    }
                    FileData absentFileData = new FileData();
                    absentFileData.setAuthor(user.getUserInfo());
                    absentFileData.setComment("Uploaded from external source");
                    absentFileData.setName(projectPath + "/" + relative);
                    absentResources.add(absentFileData);
                } else {
                    if (!designRepositoryAclService.isGranted(rulesProject.getArtefact(relative),
                            List.of(AclPermission.EDIT))) {
                        throw new Message(String.format("There is no permission for modifying '%s' file.",
                                projectPath + "/" + relative));
                    }
                }
            }
            for (String fileInZip : filesInZip) {
                if (!rulesProject.hasArtefact(fileInZip) && !designRepositoryAclService.isGranted(rulesProject,
                        List.of(AclPermission.ADD))) {
                    throw new Message(String.format("There is no permission for creating '%s' file.",
                            ProjectArtifactUtils.extractResourceName(rulesProject) + "/" + fileInZip));
                }
            }
            repository.delete(absentResources);
            // Update/create other files in project
            zipWalker.iterateEntries(new DefaultZipEntryCommand() {
                @Override
                public boolean execute(String filePath, InputStream inputStream) throws IOException {
                    FileData data = new FileData();
                    data.setAuthor(user.getUserInfo());
                    data.setComment("Uploaded from external source");
                    data.setName(projectPath + "/" + filePath);
                    repository.save(data, inputStream);
                    return true;
                }
            });
            doResetProjects();
        } catch (ValidationException e) {
            // TODO Replace exceptions with FacesUtils.addErrorMessage()
            throw e;
        } catch (Message e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while updating project in user workspace.", e);
            // TODO Replace exceptions with FacesUtils.addErrorMessage()
            throw new IllegalStateException("Error while updating project in user workspace.", e);
        }

        storeProjectHistory();

        clearUploadedFiles();

        return null;

    }

    public void storeProjectHistory() {
        currentProject = resolveProject(getCurrentProjectDescriptor());
        if (currentProject == null) {
            log.warn("The project has not been resolved after update.");
        } else {
            processProjectHistory(currentProject, ProjectHistoryService::save);
        }
    }

    public void initProjectHistory() {
        processProjectHistory(currentProject, ProjectHistoryService::init);
    }

    private static void processProjectHistory(ProjectDescriptor project, BiConsumer<String, File> func) {
        for (Module module : project.getModules()) {
            File moduleFile = module.getRulesPath().toFile();
            String moduleHistoryPath = project.getProjectFolder()
                    .resolve(FolderHelper.resolveHistoryFolder(module))
                    .toString();
            func.accept(moduleHistoryPath, moduleFile);
        }
    }

    private void tryLockProject() {
        RulesProject currentProject = getCurrentProject();
        if (!currentProject.tryLock()) {
            throw new Message("Project is locked by other user");
        }
    }

    public ProjectDescriptor resolveProject(ProjectDescriptor oldProjectDescriptor) {
        File projectFolder = oldProjectDescriptor.getProjectFolder().toFile();
        model.resetSourceModified(); // Because we rewrite a file in the
        // workspace

        ProjectDescriptor newProjectDescriptor = null;
        try {
            newProjectDescriptor = projectResolver.resolve(projectFolder);
        } catch (ProjectResolvingException e) {
            log.warn(e.getMessage(), e);
        }

        List<ProjectDescriptor> localProjects = getAllProjects();
        // Replace project descriptor in the list of all projects
        for (int i = 0; i < localProjects.size(); i++) {
            if (localProjects.get(i) == oldProjectDescriptor) {
                if (newProjectDescriptor != null) {
                    localProjects.set(i, newProjectDescriptor);
                } else {
                    localProjects.remove(i);
                }
                break;
            }
        }
        // Project can be fully changed and renamed, we must force compile
        forcedCompile = true;

        // Note that "newProjectDescriptor == null" is correct case too: it
        // means that it's not OpenL project anymore:
        // newly updated project does not contain rules.xml nor xls file. Such
        // projects are not shown in Editor but
        // are shown in Repository.
        // In this case we must show the list of all projects in Editor.
        return newProjectDescriptor;
    }

    public synchronized void forceUpdateProjectDescriptor(String repoId,
                                                          ProjectDescriptor newProjectDescriptor,
                                                          ProjectDescriptor oldProjectDescriptor) {
        newProjectDescriptor.getModules().sort(MODULES_COMPARATOR);
        if (currentProject.equals(oldProjectDescriptor)) {
            currentProject = newProjectDescriptor;
        }
        List<ProjectDescriptor> descriptors = projects.get(repoId);
        if (descriptors.remove(oldProjectDescriptor)) {
            descriptors.add(newProjectDescriptor);
        }
    }

    public boolean isUploadedProjectStructureChanged() {
        ProjectFile lastUploadedFile = getLastUploadedFile();
        if (lastUploadedFile == null) {
            return false;
        }
        try {
            List<String> filesInProject = getFilesInProject(zipFilter);

            Charset charset = zipCharsetDetector.detectCharset(new ZipFromProjectFile(lastUploadedFile),
                    filesInProject);
            if (charset == null) {
                return true;
            }
            ZipWalker zipWalker = new ZipWalker(lastUploadedFile, zipFilter, charset);

            FilePathsCollector filesCollector = new FilePathsCollector();
            zipWalker.iterateEntries(filesCollector);
            List<String> filesInZip = filesCollector.getFilePaths();

            for (String filePath : filesInProject) {
                if (!filesInZip.contains(filePath)) {
                    // Deleted file
                    return true;
                }
            }

            for (String filePath : filesInZip) {
                if (!filesInProject.contains(filePath)) {
                    // Added file
                    return true;
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    private List<String> getFilesInProject(PathFilter filter) {
        final File projectFolder = getCurrentProjectDescriptor().getProjectFolder().toFile();
        Collection<File> files = getProjectFiles(projectFolder, filter);
        final List<String> filesInProject = new ArrayList<>();
        for (File file : files) {
            filesInProject.add(getRelativePath(projectFolder, file));
        }
        return filesInProject;
    }

    public boolean isUploadedModuleChanged() {
        ProjectFile lastUploadedFile = getLastUploadedFile();
        if (lastUploadedFile == null) {
            return false;
        }

        Module module = getCurrentModule();
        if (module != null) {
            String moduleFullPath = module.getRulesPath().toString().replace('\\', '/');
            String lastUploadedFilePath = lastUploadedFile.getName().replace('\\', '/');

            String moduleFileName = moduleFullPath.substring(moduleFullPath.lastIndexOf('/') + 1);
            String lastUploadedFileName = lastUploadedFilePath.substring(lastUploadedFilePath.lastIndexOf('/') + 1);

            return !lastUploadedFileName.equals(moduleFileName);
        }

        return false;
    }

    private String validateUploadedFiles(ProjectFile zipFile,
                                         PathFilter zipFilter,
                                         ProjectDescriptor oldProjectDescriptor,
                                         Charset charset) throws IOException {
        ProjectDescriptor newProjectDescriptor;
        try {
            newProjectDescriptor = ZipProjectDescriptorExtractor
                    .getProjectDescriptorOrThrow(zipFile, zipFilter, charset);
        } catch (ProjectDescriptionException e) {
            return e.getMessage();
        }
        if (newProjectDescriptor != null && !newProjectDescriptor.getName().equals(oldProjectDescriptor.getName())) {
            return validateProjectName(newProjectDescriptor.getName());
        }

        return null;
    }

    private String validateProjectName(String projectName) {
        String msg = null;
        if (StringUtils.isBlank(projectName)) {
            msg = "Project name must not be empty.";
        } else if (!NameChecker.checkName(projectName)) {
            msg = NameChecker.BAD_PROJECT_NAME_MSG;
        } else if (isProjectExists(projectName)) {
            msg = "Failed to update the project. Another project with the same name already exists in Repository.";
        }
        return msg;
    }

    private Collection<File> getProjectFiles(File projectFolder, final PathFilter filter) {
        IOFileFilter fileFilter = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                String path = file.getPath().replace(File.separator, "/");
                if (file.isDirectory() && !path.endsWith("/")) {
                    path += "/";
                }
                return filter.accept(path);
            }

            @Override
            public boolean accept(File dir, String name) {
                return accept(new File(dir, name));
            }
        };
        return FileUtils.listFiles(projectFolder, fileFilter, fileFilter);
    }

    private String getRelativePath(File baseFolder, File file) {
        return baseFolder.toURI().relativize(file.toURI()).getPath().replace("\\", "/");
    }

    private ProjectFile getLastUploadedFile() {
        if (!uploadedFiles.isEmpty()) {
            return uploadedFiles.get(uploadedFiles.size() - 1);
        }
        return null;
    }

    public AProject getProjectByName(final String name) {
        try {
            AProject project = getProjectFromWorkspace(name);
            if (project != null) {
                return project;
            }

            // Probably a project was renamed in local workspace
            for (List<ProjectDescriptor> descriptors : getProjects().values()) {
                Optional<ProjectDescriptor> descriptor = descriptors.stream()
                        .filter(p -> p.getName().equals(name))
                        .findFirst();
                if (descriptor.isPresent()) {
                    String folderName = descriptor.get().getProjectFolder().getFileName().toString();
                    project = getProjectFromWorkspace(folderName);
                    if (project != null) {
                        break;
                    }
                }
            }

            if (project == null) {
                log.warn("Projects descriptor is found but the project is not found.");
            }
            return project;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private AProject getProjectFromWorkspace(String name) {
        LocalWorkspace localWorkspace = rulesUserSession.getUserWorkspace().getLocalWorkspace();

        for (AProject workspaceProject : localWorkspace.getProjects()) {
            if (workspaceProject.getName().equals(name)) {
                return workspaceProject;
            }
        }
        return null;
    }

    public ProjectDescriptor getProjectByName(String repositoryId, final String name) {
        return CollectionUtils.findFirst(getProjects().get(repositoryId), project -> project.getName().equals(name));
    }

    public ProjectDependencyDescriptor getProjectDependency(final String dependencyName) {
        List<ProjectDependencyDescriptor> dependencies = getCurrentProjectDescriptor().getDependencies();
        return CollectionUtils.findFirst(dependencies, dependency -> dependency.getName().equals(dependencyName));
    }

    /**
     * Checks if there is any project with specified name in repository.
     *
     * @param name physical or logical project name
     * @return true only if there is a project with specified name and it is not current project
     */
    public boolean isProjectExists(final String name) {
        HttpSession session = WebStudioUtils.getSession();
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);

        // The order of getting projects is important!
        Collection<RulesProject> projects = userWorkspace.getProjects(); // #1
        RulesProject currentProject = getCurrentProject(); // #3

        return projects.stream()
                .anyMatch(p -> p != currentProject && p.getName()
                        .equals(name) && (p.isOpened() || p.getDesignRepository().getId().equals(currentRepositoryId)));
    }

    private void setTreeView(RulesTreeView treeView) {
        this.treeView = treeView;
        model.redraw();
        userSettingsManager.setProperty(rulesUserSession.getUserName(), RULES_TREE_VIEW, treeView.getName());
    }

    private void setDefaultTreeView(RulesTreeView treeView) {
        userSettingsManager.setProperty(rulesUserSession.getUserName(), RULES_TREE_VIEW_DEFAULT, treeView.getName());
    }

    public String getCurrentUsername() {
        return rulesUserSession.getUserName();
    }

    public void setTreeView(String name) {
        RulesTreeView mode = getTreeView(name);
        if (mode != null) {
            setTreeView(mode);
        } else {
            log.error("Cannot find RulesTreeView for name {}", name);
        }
    }

    private RulesTreeView getTreeView(String name) {
        for (RulesTreeView mode : Profile.TREE_VIEWS) {
            if (name.equals(mode.getName())) {
                return mode;
            }
        }
        return null;
    }

    public void setDefaultTreeView(String name) {
        RulesTreeView mode = getTreeView(name);
        if (mode != null) {
            setDefaultTreeView(mode);
        } else {
            log.error("Can't find RulesTreeView for name {}", name);
        }
    }

    public void setTableUri(String tableUri) {
        this.tableUri = tableUri;
    }

    public boolean isUpdateSystemProperties() {
        return Props.bool(AdministrationSettings.UPDATE_SYSTEM_PROPERTIES);
    }

    public boolean isShowFormulas() {
        return showFormulas;
    }

    public void setShowFormulas(boolean showFormulas) {
        this.showFormulas = showFormulas;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), TABLE_FORMULAS_SHOW, showFormulas);
    }

    public int getTestsPerPage() {
        return testsPerPage;
    }

    public void setTestsPerPage(int testsPerPage) {
        this.testsPerPage = testsPerPage;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), TEST_TESTS_PERPAGE, testsPerPage);
    }

    public boolean isTestsFailuresOnly() {
        return testsFailuresOnly;
    }

    public void setTestsFailuresOnly(boolean testsFailuresOnly) {
        this.testsFailuresOnly = testsFailuresOnly;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), TEST_FAILURES_ONLY, testsFailuresOnly);
    }

    public int getTestsFailuresPerTest() {
        return testsFailuresPerTest;
    }

    public void setTestsFailuresPerTest(int testsFailuresPerTest) {
        this.testsFailuresPerTest = testsFailuresPerTest;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), TEST_FAILURES_PERTEST, testsFailuresPerTest);
    }

    public boolean isCollapseProperties() {
        return collapseProperties;
    }

    public void setCollapseProperties(boolean collapseProperties) {
        this.collapseProperties = collapseProperties;
    }

    public boolean isShowComplexResult() {
        return showComplexResult;
    }

    public void setShowComplexResult(boolean showComplexResult) {
        this.showComplexResult = showComplexResult;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), TEST_RESULT_COMPLEX_SHOW, showComplexResult);
    }

    public void setNeedRestart(boolean needRestart) {
        this.needRestart = needRestart;
    }

    public boolean isNeedRestart() {
        return needRestart;
    }

    public void uploadListener(FileUploadEvent event) {
        ProjectFile file = null;
        try {
            file = new ProjectFile(event.getUploadedFile());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        uploadedFiles.add(file);
    }

    public void destroy() {
        if (model != null) {
            model.destroy();
        }

        if (rulesUserSession != null) {
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
            userWorkspace.getDesignTimeRepository().removeListener(this);
        }

        clearUploadedFiles();
    }

    public void clearUploadedFiles() {
        for (ProjectFile uploadedFile : uploadedFiles) {
            uploadedFile.destroy();
        }
        uploadedFiles.clear();
    }

    /**
     * Normalizes page urls: inserts project and module names.
     * <p/>
     * Example: Page Url = create/ Normalized Url = #tutorial1/rules/create/
     */
    public String url() {
        return url(null, null);
    }

    public String url(String pageUrl) {
        return url(pageUrl, null);
    }

    public String url(String pageUrl, final String tableURI) {
        String projectName;
        String moduleName;
        String repositoryId = currentRepositoryId;
        if (tableURI == null) {
            moduleName = getCurrentModule() == null ? null : getCurrentModule().getName();
            projectName = getCurrentProjectDescriptor() == null ? null : getCurrentProjectDescriptor().getName();
        } else {
            // Get a project
            List<ProjectDescriptor> allProjects = getAllProjects();
            ProjectDescriptor project = CollectionUtils.findFirst(allProjects,
                    projectDescriptor -> tableURI.startsWith(projectDescriptor.getRelativeUri()));
            if (project == null) {
                return null;
            }

            // Get a module
            Module module = CollectionUtils.findFirst(project.getModules(), module1 -> module1.containsTable(tableURI));
            repositoryId = projects.entrySet()
                    .stream()
                    .filter(
                            entry -> entry.getValue().stream().anyMatch(projectDescriptor -> projectDescriptor.equals(project)))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(currentRepositoryId);

            if (module != null) {
                projectName = project.getName();
                moduleName = module.getName();
            } else {
                // Eclipse project
                moduleName = getCurrentModule().getName();
                projectName = getCurrentProjectDescriptor().getName();
            }
        }
        if (StringUtils.isBlank(pageUrl)) {
            pageUrl = StringUtils.EMPTY;
        }

        if ((StringUtils.isBlank(projectName) || StringUtils.isBlank(moduleName)) && StringUtils.isNotBlank(pageUrl)) {
            return "#" + pageUrl;
        }
        if (StringUtils.isBlank(projectName)) {
            return "#"; // Current project is not selected. Show all projects
            // list.
        }
        if (StringUtils.isBlank(moduleName)) {
            return "#" + StringTool.encodeURL(repositoryId) + "/" + StringTool.encodeURL(projectName);
        }
        String moduleUrl = "#" + StringTool.encodeURL(repositoryId) + "/" + StringTool
                .encodeURL(projectName) + "/" + StringTool.encodeURL(moduleName);
        if (StringUtils.isBlank(pageUrl)) {
            return moduleUrl;
        }

        return moduleUrl + "/" + pageUrl;
    }

    public WebStudioLinkBuilder getLinkBuilder() {
        return linkBuilder;
    }

    public boolean isSupportsBranches() {
        RulesProject project = getCurrentProject();
        return project != null && project.isSupportsBranches();
    }

    public String getProjectBranch() {
        try {
            RulesProject project = getCurrentProject();
            return project == null ? null : project.getBranch();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean isBranchProtected() {
        return Optional.ofNullable(getCurrentProject())
                .map(UserWorkspaceProject::isBranchProtected)
                .orElse(Boolean.FALSE);
    }

    public Map<String, Object> getExternalProperties() {
        return externalProperties;
    }

    private String setProjectBranch(ProjectDescriptor descriptor, String branch) {
        try {
            String projectFolder = descriptor.getProjectFolder().getFileName().toString();
            RulesProject project = getProject(currentRepositoryId, projectFolder);
            if (isSupportsBranches() && project != null) {
                String previousBranch = project.getBranch();
                if (!branch.equals(previousBranch)) {
                    getModel().clearModuleInfo();
                    project.releaseMyLock();
                    project.setBranch(branch);

                    if (project.getLastHistoryVersion() == null) {
                        // move back to previous branch! Because the project is not present in new branch
                        project.setBranch(previousBranch);
                        log.warn(
                                "Current project does not exists in '{}' branch! Project branch was switched to the previous one",
                                branch);
                    }
                    if (project.isDeleted()) {
                        project.close();
                    } else {
                        // Update files
                        project.open();
                    }

                    String actualName = rulesUserSession.getUserWorkspace().getActualName(project);

                    doResetProjects();

                    return actualName.equals(descriptor.getName()) ? null : actualName;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public List<String> getProjectBranches() {
        try {
            if (!isSupportsBranches()) {
                return Collections.emptyList();
            }
            RulesProject project = getCurrentProject();
            return ((BranchRepository) getCurrentProject().getDesignRepository())
                    .getBranches(project.getDesignFolderName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public boolean getCanMerge() {
        RulesProject project = getCurrentProject();

        if (project == null || !isSupportsBranches() || project.isLocalOnly()) {
            return false;
        }

        try {
            if (project.isModified()) {
                return false;
            }
            List<String> branches = ((BranchRepository) project.getDesignRepository())
                    .getBranches(project.getDesignFolderName());
            if (branches.size() < 2) {
                return false;
            }
            for (AProjectArtefact artefact : project.getArtefacts()) {
                if (designRepositoryAclService.isGranted(artefact,
                        List.of(AclPermission.EDIT, AclPermission.DELETE, AclPermission.ADD))) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean getCanRedeploy() {
        UserWorkspaceProject selectedProject = getCurrentProject();
        return getCanRedeploy(selectedProject);
    }

    public boolean getCanRedeploy(UserWorkspaceProject selectedProject) {
        if (!rulesUserSession.getUserWorkspace().getDesignTimeRepository().hasDeployConfigRepo()) {
            return false;
        }

        if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject.isModified()) {
            return false;
        }

        return deploymentManager.getRepositoryConfigNames()
                .stream()
                .filter(e -> !DeploymentRepositoriesUtil
                        .isMainBranchProtected(deploymentManager.repositoryFactoryProxy.getRepositoryInstance(e)))
                .anyMatch(e -> productionRepositoryAclService.isGranted(e, null, List.of(AclPermission.EDIT)));
    }

    public boolean getCanOpenOtherVersion() {
        UserWorkspaceProject selectedProject = getCurrentProject();

        if (selectedProject == null) {
            return false;
        }

        if (!selectedProject.isLocalOnly()) {
            return designRepositoryAclService.isGranted(selectedProject, List.of(AclPermission.VIEW));
        }

        return false;
    }

    public void setProjectVersion(String version) {
        try {
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();

            RulesProject project = getCurrentProject();
            AProject historic = new AProject(project.getDesignRepository(), project.getDesignFolderName(), version);
            if (userWorkspace.isOpenedOtherProject(historic)) {
                throw new Message(
                        "OpenL Studio cannot open two projects with the same name. Close the currently opened project and try again.");
            }

            if (project.isOpened()) {
                getModel().clearModuleInfo();
                project.releaseMyLock();
            }

            project.openVersion(version);
            String repositoryId = project.getRepository().getId();
            String branch = project.getBranch();
            String actualName = userWorkspace.getActualName(project);
            resetProjects();
            init(repositoryId, branch, actualName, null);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Failed to open project version.";
            log.error(msg, e);
            throw new Message(msg);
        }
    }

    public Collection<ProjectVersion> getProjectVersions() {
        RulesProject project = getCurrentProject();
        if (project == null) {
            return Collections.emptyList();
        }

        List<ProjectVersion> versions = project.getVersions();
        Collections.reverse(versions);
        return versions;
    }

    public void freezeProject(String name) {
        frozenProjects.add(name);
    }

    public void releaseProject(String name) {
        frozenProjects.remove(name);
    }

    boolean isProjectFrozen(String name) {
        return frozenProjects.contains(name);
    }

    public String getCurrentRepositoryId() {
        return currentRepositoryId;
    }

    @Override
    public synchronized void onRepositoryModified() {
        projects = null;
        Authentication oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            if (currentProject != null) {
                RulesProject project = getCurrentProject();
                if (project == null || !project.isOpened()) {
                    currentProject = null;
                    currentModule = null;
                    model.clearModuleInfo();
                }
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(oldAuthentication);
        }
    }

    public void setShowRealNumbers(boolean showRealNumbers) {
        this.showRealNumbers = showRealNumbers;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), TRACE_REALNUMBERS_SHOW, showRealNumbers);
    }

    public boolean isShowRealNumbers() {
        return showRealNumbers;
    }
}
