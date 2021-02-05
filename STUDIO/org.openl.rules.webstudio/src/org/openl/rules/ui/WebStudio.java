package org.openl.rules.ui;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.DEPLOY_PROJECTS;
import static org.openl.rules.security.Privileges.EDIT_PROJECTS;
import static org.openl.rules.security.Privileges.VIEW_PROJECTS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
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

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.ValidationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.common.CommonException;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.MergeConflictException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.ProjectHistoryService;
import org.openl.rules.testmethod.TestSuiteExecutor;
import org.openl.rules.ui.tree.view.CategoryDetailedView;
import org.openl.rules.ui.tree.view.CategoryInversedView;
import org.openl.rules.ui.tree.view.CategoryView;
import org.openl.rules.ui.tree.view.FileView;
import org.openl.rules.ui.tree.view.RulesTreeView;
import org.openl.rules.ui.tree.view.TypeView;
import org.openl.rules.webstudio.service.UserSettingManagementService;
import org.openl.rules.webstudio.util.ExportFile;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.repository.merge.ConflictUtils;
import org.openl.rules.webstudio.web.repository.merge.MergeConflictInfo;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.ProjectDescriptorUtils;
import org.openl.rules.webstudio.web.repository.upload.ZipProjectDescriptorExtractor;
import org.openl.rules.webstudio.web.repository.upload.zip.DefaultZipEntryCommand;
import org.openl.rules.webstudio.web.repository.upload.zip.FilePathsCollector;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipFromProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipWalker;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.impl.ProjectExportHelper;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.thoughtworks.xstream.XStreamException;

/**
 * TODO Remove JSF dependency TODO Separate user session from app session TODO Move settings to separate UserSettings
 * class
 *
 * @author snshor
 */
public class WebStudio implements DesignTimeRepositoryListener {

    private final Logger log = LoggerFactory.getLogger(WebStudio.class);

    private static final Comparator<Module> MODULES_COMPARATOR = Comparator.comparing(Module::getName);
    private static final Comparator<ProjectDescriptor> PROJECT_DESCRIPTOR_COMPARATOR = (o1, o2) ->
            o1.getName().compareToIgnoreCase(o2.getName());

    private final RulesTreeView typeView = new TypeView();
    private final RulesTreeView fileView = new FileView();
    private final RulesTreeView categoryView = new CategoryView();
    private final RulesTreeView categoryDetailedView = new CategoryDetailedView();
    private final RulesTreeView categoryInversedView = new CategoryInversedView();

    private final RulesTreeView[] treeViews = { typeView,
            fileView,
            categoryView,
            categoryDetailedView,
            categoryInversedView };

    private final WebStudioLinkBuilder linkBuilder = new WebStudioLinkBuilder(this);

    private String workspacePath;
    private String tableUri;
    private final ProjectModel model;
    private final ProjectResolver projectResolver;
    private Map<String, List<ProjectDescriptor>> projects = null;

    private RulesTreeView treeView;
    private String tableView;
    private boolean showFormulas;
    private int testsPerPage;
    private boolean testsFailuresOnly;
    private int testsFailuresPerTest;
    private boolean showComplexResult;
    private ModuleMode defaultModuleMode = ModuleMode.MULTI;

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

    /**
     * Projects that are currently processed, for example saved. Projects's state can be in intermediate state, and it
     * can affect their modified status.
     */
    private final Set<String> frozenProjects = new HashSet<>();
    private boolean needRedirect;

    public WebStudio(HttpSession session) {
        model = new ProjectModel(this, WebStudioUtils.getBean(TestSuiteExecutor.class));
        userSettingsManager = WebStudioUtils.getBean(UserSettingManagementService.class);
        rulesUserSession = WebStudioUtils.getRulesUserSession(session, true);

        initWorkspace(session);
        initUserSettings();
        projectResolver = ProjectResolver.getInstance();
        externalProperties = new HashMap<>();
        copyExternalProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY);
        copyExternalProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY);
        copyExternalProperty(OpenLSystemProperties.DISPATCHING_VALIDATION);
    }

    private void copyExternalProperty(String key) {
        String value = Props.text(key);
        externalProperties.put(key, value);
    }

    private void initWorkspace(HttpSession session) {
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);

        if (userWorkspace == null) {
            return;
        }

        workspacePath = userWorkspace.getLocalWorkspace().getLocation().getAbsolutePath();
        userWorkspace.getDesignTimeRepository().addListener(this);
    }

    private void initUserSettings() {
        String userName = rulesUserSession.getUserName();

        treeView = getTreeView(userSettingsManager.getStringProperty(userName, "rules.tree.view"));
        tableView = userSettingsManager.getStringProperty(userName, "table.view");
        showFormulas = userSettingsManager.getBooleanProperty(userName, "table.formulas.show");
        testsPerPage = userSettingsManager.getIntegerProperty(userName, "test.tests.perpage");
        testsFailuresOnly = userSettingsManager.getBooleanProperty(userName, "test.failures.only");
        testsFailuresPerTest = userSettingsManager.getIntegerProperty(userName, "test.failures.pertest");
        showComplexResult = userSettingsManager.getBooleanProperty(userName, "test.result.complex.show");

        String defaultModuleMode = userSettingsManager.getStringProperty(userName, "project.module.default.mode");
        if (StringUtils.isNotEmpty(defaultModuleMode)) {
            try {
                this.defaultModuleMode = ModuleMode.valueOf(defaultModuleMode.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    public RulesTreeView[] getTreeViews() {
        return treeViews;
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
                    msg = "Failed to save the project. Please close module Excel file and try again.";
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

            throw new ValidationException(msg);
        }
    }

    public boolean isMergeConflict() {
        return ConflictUtils.getMergeConflict() != null;
    }

    public boolean isRenamed(RulesProject project) {
        return project != null && !getLogicalName(project).equals(project.getName());
    }

    public String getLogicalName(RulesProject project) {
        return project == null ? null : getProjectDescriptorResolver().getLogicalName(project);
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
                    IProjectDescriptorSerializer serializer = WebStudioUtils
                        .getBean(ProjectDescriptorSerializerFactory.class)
                        .getSerializer(project);
                    AProjectResource artefact = (AProjectResource) project
                        .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
                    content = artefact.getContent();
                    ProjectDescriptor projectDescriptor = serializer.deserialize(content);
                    projectDescriptor.setName(project.getName());
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
                        log.warn("Can't rename folder from " + prevPath + " to " + newPath);
                    }
                }
            }
            userWorkspace.refresh();
            model.resetSourceModified();
        } catch (WorkspaceException | IOException e) {
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

    public RulesProject getProject(String repositoryId, String name) {
        UserWorkspace userWorkspace;
        try {
            userWorkspace = rulesUserSession.getUserWorkspace();
        } catch (WorkspaceException e) {
            log.error(e.getMessage(), e);
            return null;
        }

        if (userWorkspace.hasProject(repositoryId, name)) {
            try {
                return userWorkspace.getProject(repositoryId, name, false);
            } catch (ProjectException e) {
                // Should not occur
                log.error(e.getMessage(), e);
                return null;
            }
        }
        return null;
    }

    public String exportModule() {
        try {
            File file = currentModule.getRulesPath().toFile();
            if (file.isDirectory() || !file.exists()) {
                throw new ProjectException("Exporting module was failed");
            }

            final FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) WebStudioUtils.getExternalContext().getResponse();
            ExportFile.writeOutContent(response, file);
            facesContext.responseComplete();
        } catch (ProjectException e) {
            log.error("Failed to export module", e);
        }
        return null;
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
        userSettingsManager.setProperty(rulesUserSession.getUserName(), "table.view", tableView);
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
        try {
            rulesUserSession.getUserWorkspace().syncProjects();
            rulesUserSession.getUserWorkspace().refresh();
        } catch (CommonException e) {
            log.error("Error on reloading user's workspace", e);
        }
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

    boolean isAutoCompile() {
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
                WebStudioUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
                FacesContext.getCurrentInstance().responseComplete();
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
                    WebStudioUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
                    FacesContext.getCurrentInstance().responseComplete();
                    return;
                }
            }
            Module module = getModule(project, moduleName);
            if (StringUtils.isNotBlank(moduleName) && module == null) {
                // Not empty module name is requested but it's not found
                WebStudioUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
                FacesContext.getCurrentInstance().responseComplete();
                return;
            }
            boolean moduleChanged = currentProject != project || currentModule != module;
            currentModule = module;
            currentProject = project;
            if (module != null && (needCompile && (isAutoCompile() || manualCompile) || forcedCompile || moduleChanged)) {
                if (forcedCompile) {
                    reset(ReloadType.FORCED);
                } else if (needCompile || moduleChanged) {
                    //if moduleChanged is true - we need to reset the project because we change tableSyntaxNode directly
                    //must be rewritten - tableSyntaxNode must be changed only on project saving
                    reset(ReloadType.SINGLE);
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
            WebStudioUtils.getExternalContext().setResponseStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            FacesContext.getCurrentInstance().responseComplete();
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
            throw new IllegalArgumentException("No file has been uploaded. Please upload .zip file to update project");
        }
        if (!FileTypeHelper.isZipFile(FilenameUtils.getName(lastUploadedFile.getName()))) {
            // TODO Replace exceptions with FacesUtils.addErrorMessage()
            throw new IllegalArgumentException("Wrong filename extension. Please upload .zip file");
        }
        ProjectDescriptor projectDescriptor;
        try {
            for (Module module : currentProject.getModules()) {
                File moduleFile = module.getRulesPath().toFile();
                String moduleHistoryPath = currentProject.getProjectFolder()
                        .resolve(FolderHelper.HISTORY_FOLDER)
                        .resolve(module.getName())
                        .toString();
                ProjectHistoryService.init(moduleHistoryPath, moduleFile);
            }
            tryLockProject();

            projectDescriptor = getCurrentProjectDescriptor();
            PathFilter filter = getZipFilter();

            List<String> filesInProject = getFilesInProject(filter);
            Charset charset = getZipCharsetDetector().detectCharset(new ZipFromProjectFile(lastUploadedFile),
                filesInProject);
            if (charset == null) {
                throw new ValidationException("Cannot detect a charset for the zip file");
            }

            String errorMessage = validateUploadedFiles(lastUploadedFile, filter, projectDescriptor, charset);
            if (errorMessage != null) {
                // TODO Replace exceptions with FacesUtils.addErrorMessage()
                throw new ValidationException(errorMessage);
            }

            final String userName = rulesUserSession.getUserName();
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
            final LocalRepository repository = userWorkspace.getLocalWorkspace().getRepository(currentRepositoryId);
            // project folder is not the same as project name
            final String projectPath = projectDescriptor.getProjectFolder().getFileName().toString();

            // Release resources that can be deleted or replaced
            getModel().clearModuleInfo();

            ZipWalker zipWalker = new ZipWalker(lastUploadedFile, filter, charset);

            FilePathsCollector filesCollector = new FilePathsCollector();
            zipWalker.iterateEntries(filesCollector);
            List<String> filesInZip = filesCollector.getFilePaths();

            final File projectFolder = projectDescriptor.getProjectFolder().toFile();
            Collection<File> files = getProjectFiles(projectFolder, filter);

            // Delete absent files in project
            for (File file : files) {
                String relative = getRelativePath(projectFolder, file);
                if (!filesInZip.contains(relative)) {
                    FileUtils.deleteQuietly(file);
                }
            }

            // Update/create other files in project
            zipWalker.iterateEntries(new DefaultZipEntryCommand() {
                @Override
                public boolean execute(String filePath, InputStream inputStream) throws IOException {
                    File outputFile = new File(projectFolder, filePath);
                    FileData data = new FileData();
                    data.setAuthor(userName);
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
        } catch (Exception e) {
            log.error("Error while updating project in user workspace.", e);
            // TODO Replace exceptions with FacesUtils.addErrorMessage()
            throw new IllegalStateException("Error while updating project in user workspace.", e);
        }

        currentProject = resolveProject(projectDescriptor);
        for (Module module : currentProject.getModules()) {
            File moduleFile = module.getRulesPath().toFile();
            String moduleHistoryPath = currentProject.getProjectFolder()
                    .resolve(FolderHelper.HISTORY_FOLDER)
                    .resolve(module.getName())
                    .toString();
            ProjectHistoryService.save(moduleHistoryPath, moduleFile);
        }
        if (currentProject == null) {
            log.warn("The project has not been resolved after update.");
        }

        clearUploadedFiles();

        return null;
    }

    private void tryLockProject() throws ProjectException {
        RulesProject currentProject = getCurrentProject();
        if (!currentProject.tryLock()) {
            throw new ValidationException("Project is locked by other user");
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

    public boolean isUploadedProjectStructureChanged() {
        ProjectFile lastUploadedFile = getLastUploadedFile();
        if (lastUploadedFile == null) {
            return false;
        }
        try {
            PathFilter filter = getZipFilter();
            List<String> filesInProject = getFilesInProject(filter);

            Charset charset = getZipCharsetDetector().detectCharset(new ZipFromProjectFile(lastUploadedFile),
                filesInProject);
            if (charset == null) {
                return true;
            }
            ZipWalker zipWalker = new ZipWalker(lastUploadedFile, filter, charset);

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
        } catch (XStreamException e) {
            return ProjectDescriptorUtils.getErrorMessage(e);
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

    private ProjectDescriptorArtefactResolver getProjectDescriptorResolver() {
        return (ProjectDescriptorArtefactResolver) WebApplicationContextUtils
            .getRequiredWebApplicationContext((ServletContext) WebStudioUtils.getExternalContext().getContext())
            .getBean("projectDescriptorArtefactResolver");
    }

    private PathFilter getZipFilter() {
        return (PathFilter) WebApplicationContextUtils
            .getRequiredWebApplicationContext((ServletContext) WebStudioUtils.getExternalContext().getContext())
            .getBean("zipFilter");
    }

    private ZipCharsetDetector getZipCharsetDetector() {
        return WebStudioUtils.getBean(ZipCharsetDetector.class);
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
                log.warn("Projects descriptor is found but the project isn't found.");
            }
            return project;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private AProject getProjectFromWorkspace(String name) throws WorkspaceException {
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
        userSettingsManager.setProperty(rulesUserSession.getUserName(), "rules.tree.view", treeView.getName());
    }

    public void setTreeView(String name) {
        RulesTreeView mode = getTreeView(name);
        if (mode != null) {
            setTreeView(mode);
        } else {
            log.error("Can't find RulesTreeView for name {}", name);
        }
    }

    private RulesTreeView getTreeView(String name) {
        for (RulesTreeView mode : treeViews) {
            if (name.equals(mode.getName())) {
                return mode;
            }
        }
        return null;
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
        userSettingsManager.setProperty(rulesUserSession.getUserName(), "table.formulas.show", showFormulas);
    }

    public int getTestsPerPage() {
        return testsPerPage;
    }

    public void setTestsPerPage(int testsPerPage) {
        this.testsPerPage = testsPerPage;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), "test.tests.perpage", testsPerPage);
    }

    public boolean isTestsFailuresOnly() {
        return testsFailuresOnly;
    }

    public void setTestsFailuresOnly(boolean testsFailuresOnly) {
        this.testsFailuresOnly = testsFailuresOnly;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), "test.failures.only", testsFailuresOnly);
    }

    public int getTestsFailuresPerTest() {
        return testsFailuresPerTest;
    }

    public void setTestsFailuresPerTest(int testsFailuresPerTest) {
        this.testsFailuresPerTest = testsFailuresPerTest;
        userSettingsManager.setProperty(rulesUserSession.getUserName(), "test.failures.pertest", testsFailuresPerTest);
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
        userSettingsManager.setProperty(rulesUserSession.getUserName(), "test.result.complex.show", showComplexResult);
    }

    public boolean isSingleModuleModeByDefault() {
        return defaultModuleMode == ModuleMode.SINGLE || defaultModuleMode == ModuleMode.SINGLE_ONLY;
    }

    public boolean isChangeableModuleMode() {
        return defaultModuleMode == ModuleMode.MULTI || defaultModuleMode == ModuleMode.SINGLE;
    }

    public void setSingleModuleModeByDefault(boolean singleMode) {
        this.defaultModuleMode = singleMode ? ModuleMode.SINGLE : ModuleMode.MULTI;
        userSettingsManager
            .setProperty(rulesUserSession.getUserName(), "project.module.default.mode", defaultModuleMode.name());
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
            try {
                UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
                userWorkspace.getDesignTimeRepository().removeListener(this);
            } catch (WorkspaceException e) {
                log.warn(e.getMessage(), e);
            }
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
        if (tableURI == null) {
            moduleName = getCurrentModule() == null ? null : getCurrentModule().getName();
            projectName = getCurrentProjectDescriptor() == null ? null : getCurrentProjectDescriptor().getName();
        } else {
            // Get a project
            List<ProjectDescriptor> allProjects = getAllProjects();
            ProjectDescriptor project = CollectionUtils.findFirst(allProjects, projectDescriptor -> {
                String projectURI = projectDescriptor.getProjectFolder().toUri().toString();
                return tableURI.startsWith(projectURI);
            });
            if (project == null) {
                return null;
            }
            // Get a module
            Module module = CollectionUtils.findFirst(project.getModules(), new CollectionUtils.Predicate<Module>() {
                @Override
                public boolean evaluate(Module module) {
                    if (module.getRulesRootPath() == null) {
                        // Eclipse project
                        return false;
                    }
                    String moduleURI = module.getRulesPath().toUri().toString();
                    return tableURI.startsWith(moduleURI);
                }
            });

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
            return "#" + StringTool.encodeURL(currentRepositoryId) + "/" + StringTool.encodeURL(projectName);
        }
        String moduleUrl = "#" + StringTool.encodeURL(currentRepositoryId) + "/" + StringTool
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
            return ((BranchRepository) getCurrentProject().getDesignRepository()).getBranches(project.getDesignFolderName());
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
            List<String> branches = ((BranchRepository) project.getDesignRepository()).getBranches(project.getDesignFolderName());
            if (branches.size() < 2) {
                return false;
            }

            return isGranted(EDIT_PROJECTS);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean getCanRedeploy() {
        UserWorkspaceProject selectedProject = getCurrentProject();

        if (selectedProject == null || selectedProject.isLocalOnly() || selectedProject.isModified()) {
            return false;
        }

        return isGranted(DEPLOY_PROJECTS);
    }

    public boolean getCanOpenOtherVersion() {
        UserWorkspaceProject selectedProject = getCurrentProject();

        if (selectedProject == null) {
            return false;
        }

        if (!selectedProject.isLocalOnly()) {
            return isGranted(VIEW_PROJECTS);
        }

        return false;
    }

    public void setProjectVersion(String version) {
        try {
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();

            RulesProject project = getCurrentProject();
            AProject historic = new AProject(project.getDesignRepository(), project.getDesignFolderName(), version);
            if (userWorkspace.isOpenedOtherProject(historic)) {
                throw new ValidationException(
                    "WebStudio can't open two projects with the same name. Please close another project and open it again.");
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
            throw new ValidationException(msg);
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

        if (currentProject != null) {
            RulesProject project = getCurrentProject();
            if (project == null || !project.isOpened()) {
                currentProject = null;
                currentModule = null;
                model.clearModuleInfo();
            }
        }
    }
}
