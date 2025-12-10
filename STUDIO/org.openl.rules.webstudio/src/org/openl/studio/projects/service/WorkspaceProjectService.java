package org.openl.studio.projects.service;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.rules.webstudio.web.TablePropertiesSelector;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.webstudio.web.repository.merge.ConflictUtils;
import org.openl.rules.webstudio.web.repository.merge.MergeConflictInfo;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.ProjectDependencyViewModel;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.TableCreatorService;
import org.openl.studio.projects.service.tables.read.EditableTableReader;
import org.openl.studio.projects.service.tables.read.RawTableReader;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.studio.projects.validator.NewBranchValidator;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;

/**
 * Implementation of project service for workspace projects.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
public class WorkspaceProjectService extends AbstractProjectService<RulesProject> {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceProjectService.class);

    private static final Set<ProjectStatus> ALLOWED_STATUSES = EnumSet.of(ProjectStatus.CLOSED, ProjectStatus.VIEWING);

    private final ProjectStateValidator projectStateValidator;
    private final ProjectDependencyResolver projectDependencyResolver;
    private final SummaryTableReader summaryTableReader;
    private final RawTableReader rawTableReader;
    private final List<EditableTableReader<? extends TableView, ? extends TableView.Builder<?>>> readers;
    private final Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory;
    private final BeanValidationProvider validationProvider;
    private final TableWriterExecutor tableWriterExecutor;
    private final TableCreatorService tableCreatorService;
    private final TableWritersFactory tableWritersFactory;

    public WorkspaceProjectService(
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            ProjectStateValidator projectStateValidator,
            ProjectDependencyResolver projectDependencyResolver,
            SummaryTableReader summaryTableReader,
            RawTableReader rawTableReader,
            List<EditableTableReader<? extends TableView, ? extends TableView.Builder<?>>> readers,
            Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory,
            BeanValidationProvider validationProvider,
            TableCreatorService tableCreatorService,
            TableWriterExecutor tableWriterExecutor,
            TableWritersFactory tableWritersFactory) {
        super(designRepositoryAclService);
        this.projectStateValidator = projectStateValidator;
        this.projectDependencyResolver = projectDependencyResolver;
        this.summaryTableReader = summaryTableReader;
        this.rawTableReader = rawTableReader;
        this.readers = readers;
        this.newBranchValidatorFactory = newBranchValidatorFactory;
        this.validationProvider = validationProvider;
        this.tableCreatorService = tableCreatorService;
        this.tableWriterExecutor = tableWriterExecutor;
        this.tableWritersFactory = tableWritersFactory;
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    public ProjectViewModel getProject(RulesProject project) {
        return mapProjectResponse(project).build();
    }

    @Override
    protected ProjectViewModel.Builder mapProjectResponse(RulesProject src) {
        var builder = super.mapProjectResponse(src);
        if (src.isSupportsBranches()) {
            try {
                var selectedBranches = src.getSelectedBranches();
                selectedBranches.sort(String.CASE_INSENSITIVE_ORDER);
                builder.selectedBranches(selectedBranches);
            } catch (ProjectException e) {
                LOG.warn("Failed to retrieve project branches", e);
            }
        }
        projectDependencyResolver.getProjectDependencies(src).stream()
                .sorted(PROJECT_BUSINESS_NAME_ORDER)
                .map(this::mapProjectDependency)
                .map(ProjectDependencyViewModel.Builder::build)
                .forEach(builder::addDependency);
        return builder;
    }

    protected ProjectDependencyViewModel.Builder mapProjectDependency(RulesProject src) {
        var repository = src.getRepository();
        var builder = ProjectDependencyViewModel.builder().name(src.getBusinessName())
                .id(resolveProjectId(src))
                .repository(repository.getId());
        builder.status(src.getStatus()).branch(src.getBranch());
        return builder;
    }

    @Override
    protected Stream<RulesProject> getProjects0(ProjectCriteriaQuery query) {
        var workspace = getUserWorkspace();
        Collection<RulesProject> projects;
        if (query.repositoryId() != null) {
            var repositoryId = query.repositoryId();
            projects = workspace.getProjects(repositoryId);
        } else {
            projects = workspace.getProjects();
        }
        return projects.stream();
    }

    @Override
    @Nonnull
    protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
        Predicate<AProject> filter = super.buildFilterCriteria(query);

        if (query.status() != null) {
            filter = filter.and(project -> {
                var workspaceProject = (UserWorkspaceProject) project;
                return workspaceProject.getStatus() == query.status();
            });
        }

        if (query.dependsOn() != null) {
            filter = filter.and(project -> {
                var rulesProject = (RulesProject) project;
                var dependencies = projectDependencyResolver.getProjectDependencies(rulesProject);
                return dependencies.stream().anyMatch(dependency -> {
                    var dependencyId = resolveProjectId(dependency);
                    return dependencyId.equals(query.dependsOn());
                });
            });
        }

        return filter;
    }

    public void updateProjectStatus(RulesProject project, ProjectStatusUpdateModel model) throws ProjectException {
        if (model.getStatus() != null && !ALLOWED_STATUSES.contains(model.getStatus())) {
            throw new BadRequestException("invalid.project.status.message");
        }
        if (project.isModified() && model.getComment().isPresent()) {
            save(project, model);
        }
        if (model.getStatus() == ProjectStatus.VIEWING) {
            if (!project.isOpened() || model.getBranch().isPresent() || model.getRevision().isPresent()) {
                open(project, false, model);
            }
        } else {
            if (model.getStatus() == ProjectStatus.CLOSED && project.getStatus() != ProjectStatus.CLOSED) {
                close(project);
            }
            if (model.getBranch().isPresent()) {
                switchToBranch(project, model.getBranch().get());
            }
        }
        if (CollectionUtils.isNotEmpty(model.getSelectedBranches())) {
            project.setSelectedBranches(model.getSelectedBranches());
        }
    }

    /**
     * Save project
     *
     * @param project project
     * @param model   project status update model
     * @throws ProjectException if failed to save project
     */
    public void save(RulesProject project, ProjectStatusUpdateModel model) throws ProjectException {
        if (!project.isModified()) {
            return;
        }
        var comment = model.getComment().map(String::trim).orElse(null);
        try {
            CommentValidator.forRepo(project.getRepository().getId()).validate(comment);
        } catch (Exception e) {
            throw new BadRequestException("repo.invalid.comment.message", new Object[]{e.getMessage()});
        }
        project.getFileData().setComment(comment);
        try {
            ConflictUtils.removeMergeConflict();
            getWebStudio().saveProject(project);
        } catch (ProjectException e) {
            var cause = e.getCause();
            if (cause instanceof MergeConflictException) {
                var info = new MergeConflictInfo((MergeConflictException) cause, project);
                ConflictUtils.saveMergeConflict(info);
            }
            throw e;
        }
    }

    /**
     * Close project
     *
     * @param project project
     * @throws ProjectException if failed to close project
     */
    public void close(RulesProject project) throws ProjectException {
        var webStudio = getWebStudio();
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            throw new ForbiddenException("default.message");
        }
        if (project.isDeleted()) {
            throw new ConflictException("project.close.deleted.message", project.getBusinessName());
        } else if (!projectStateValidator.canClose(project)) {
            throw new ConflictException("project.close.conflict.message");
        }
        try {
            ProjectHistoryService.deleteHistory(project.getBusinessName());
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            }
            throw new ProjectException("Failed to delete project history", e);
        }
        // We must release module info because it can hold jars.
        // We cannot rely on studio.getProject() to determine if closing project is compiled inside
        // studio.getModel()
        // because project could be changed or cleared before (See studio.reset() usages). Also that project can be
        // a dependency of other. That's why we must always clear moduleInfo when closing a project.
        webStudio.getModel().clearModuleInfo();
        project.close();
    }

    /**
     * Open project
     *
     * @param project          project
     * @param openDependencies open project dependencies
     * @throws ProjectException if failed to open project
     */
    public void open(RulesProject project, boolean openDependencies) throws ProjectException {
        open(project, openDependencies, new ProjectStatusUpdateModel());
    }

    private void open(RulesProject project,
                      boolean openDependencies,
                      ProjectStatusUpdateModel model) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            throw new ForbiddenException("default.message");
        }
        var workspace = getUserWorkspace();
        if (project.isDeleted()) {
            throw new ConflictException("project.open.deleted.message", project.getBusinessName());
        } else if (!projectStateValidator.canOpen(project)) {
            throw new ConflictException("project.open.conflict.message");
        }

        if (model.getRevision().isPresent()) {
            AProject historic = new AProject(project.getDesignRepository(),
                    project.getDesignFolderName(),
                    model.getRevision().get());
            if (workspace.isOpenedOtherProject(historic)) {
                throw new ConflictException("open.duplicated.project");
            }
        }
        // Do we really need to check this if we have a version? Copy-paste from
        // RepositoryTreeController#openProjectVersion
        if (workspace.isOpenedOtherProject(project)) {
            throw new ConflictException("open.duplicated.project");
        }

        var wasOpened = project.isOpened();
        var webStudio = getWebStudio();
        if (wasOpened && (model.getBranch().isPresent() || model.getRevision().isPresent())) {
            // We must clear module info and release project lock
            // because project was already opened and we are going to open it in another branch or revision
            webStudio.getModel().clearModuleInfo();
            project.releaseMyLock();
        }

        if (model.getBranch().isPresent()) {
            switchToBranch(project, model.getBranch().get());
        }

        if (model.getRevision().isPresent()) {
            project.openVersion(model.getRevision().get());
        } else {
            if (model.getBranch().isPresent() || !wasOpened) {
                project.open();
            } else {
                throw new ConflictException("project.open.conflict.message");
            }
        }

        if (openDependencies) {
            openAllDependencies(project);
        }
    }

    private void switchToBranch(RulesProject project, String branchName) throws ProjectException {
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        var previousBranch = project.getBranch();
        if (Objects.equals(previousBranch, branchName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Project '{}' is already opened in branch '{}'", project.getBusinessName(), branchName);
            }
            return;
        }

        var wasOpened = project.isOpened();
        if (wasOpened) {
            var webStudio = getWebStudio();
            // We must clear module info and release project lock
            // because project was already opened and we are going to open it in another branch or revision
            webStudio.getModel().clearModuleInfo();
            project.releaseMyLock();
        }

        var previousBusinessName = project.getBusinessName();
        project.setBranch(branchName);
        if (project.getLastHistoryVersion() == null) {
            project.setBranch(previousBranch);
            throw new ConflictException("project.switch.branch.failed.message", branchName);
        }
        if (wasOpened) {
            if (project.isDeleted()) {
                project.close();
            } else {
                // Update files
                try {
                    ProjectHistoryService.deleteHistory(previousBusinessName);
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(e.getMessage(), e);
                    }
                    throw new ProjectException("Failed to delete project history", e);
                }
                var workspace = getUserWorkspace();
                if (workspace.isOpenedOtherProject(project)) {
                    throw new ConflictException("open.duplicated.project");
                } else {
                    project.open();
                }
            }
        }
    }

    private void openAllDependencies(RulesProject project) throws ProjectException {
        for (RulesProject rulesProject : projectDependencyResolver.getProjectDependencies(project)) {
            rulesProject.open();
        }
    }

    /**
     * Create a new branch
     *
     * @param project project
     * @param model   branch creation model
     * @throws ProjectException if failed to create a new branch
     */
    public void createBranch(RulesProject project, CreateBranchModel model) throws ProjectException {
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        if (!hasCreateBranchPermissions(project)) {
            throw new ForbiddenException("default.message");
        }
        var repository = (BranchRepository) project.getDesignRepository();
        var validator = newBranchValidatorFactory.apply(repository);
        validationProvider.validate(model.getBranch(), validator);
        try {
            repository.createBranch(project.getDesignFolderName(), model.getBranch(), model.getRevision());
        } catch (IOException e) {
            throw new ProjectException("Failed to create branch", e);
        }
    }

    private boolean hasCreateBranchPermissions(RulesProject project) {
        if (project.isSupportsBranches()) {
            // FIXME Potential performance spike: If the project contains a large number of artifacts, it may result in slower performance.
            for (AProjectArtefact artefact : project.getArtefacts()) {
                if (designRepositoryAclService.isGranted(artefact,
                        List.of(BasePermission.WRITE, BasePermission.DELETE, BasePermission.CREATE))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get project tables
     *
     * @param project project
     * @param query   filter query
     * @return project tables
     */
    public Collection<SummaryTableView> getTables(RulesProject project, ProjectTableCriteriaQuery query) {
        var moduleModel = getProjectModel(project);

        var selectors = buildTableSelector(query);
        return moduleModel.search(selectors, SearchScope.CURRENT_PROJECT)
                .stream()
                .map(summaryTableReader::read)
                .collect(Collectors.toList());
    }

    private Predicate<TableSyntaxNode> buildTableSelector(ProjectTableCriteriaQuery query) {
        Predicate<TableSyntaxNode> selectors = tsn -> !XlsNodeTypes.XLS_OTHER.toString().equals(tsn.getType());

        var tableTypes = query.getKinds()
                .stream()
                .map(OpenLTableUtils.getTableTypeItems().inverse()::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(tableTypes)) {
            selectors = selectors.and(tsn -> tableTypes.contains(tsn.getType()));
        }

        if (query.getName().isPresent()) {
            var name = query.getName().get();
            selectors = selectors.and(tsn -> {
                var type = XlsNodeTypes.getEnumByValue(tsn.getType());
                var header = tsn.getHeader();
                var displayName = TableSyntaxNodeUtils.str2name(header.getSourceString(), type);
                return displayName.equals(name);
            });
        }

        if (CollectionUtils.isNotEmpty(query.getProperties())) {
            selectors = selectors.and(new TablePropertiesSelector(query.getProperties()));
        }

        return selectors;
    }

    public ProjectModel getProjectModel(RulesProject project) {
        return getProjectModel(project, (String) null);
    }

    public ProjectModel getProjectModel(RulesProject project, @Nullable String moduleName) {
        if (!project.isOpened()) {
            throw new ConflictException("project.not.opened.message");
        }
        var webstudio = getWebStudio();

        var projectDescriptor = webstudio.getProjectByName(project.getRepository().getId(), project.getName());
        var moduleSelector = projectDescriptor.getModules().stream();
        if (moduleName != null) {
            moduleSelector = moduleSelector.filter(module -> module.getName() != null && module.getName().equals(moduleName));
        }
        var module = moduleSelector.findFirst().orElse(null);
        return getProjectModel(project, module);
    }

    private ProjectModel getProjectModel(RulesProject project, @Nullable Module module) {
        if (module == null) {
            throw new NotFoundException("project.identifier.message");
        }
        var webstudio = getWebStudio();
        webstudio.init(project.getRepository().getId(), project.getBranch(), project.getName(), module.getName());
        var moduleModel = webstudio.getModel();
        while (!moduleModel.isProjectCompilationCompleted()) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Project compilation interrupted", e);
            }
        }
        return moduleModel;
    }

    /**
     * Get table data
     *
     * @param project project
     * @param tableId table id
     * @return table data
     */
    public TableView getTable(RulesProject project, String tableId) {
        var table = getOpenLTable(project, tableId);
        var reader = readers.stream()
                .filter(r -> r.supports(table))
                .findFirst()
                .orElse(null);
        return reader != null ? reader.read(table) : rawTableReader.read(table);
    }

    /**
     * Get table in raw format as 2D matrix with merge information
     *
     * @param project project
     * @param tableId table id
     * @return raw table data
     */
    public RawTableView getTableRaw(RulesProject project, String tableId) {
        var table = getOpenLTable(project, tableId);
        return rawTableReader.read(table);
    }

    private IOpenLTable getOpenLTable(RulesProject project, String tableId) {
        var moduleModel = getProjectModel(project);
        var table = moduleModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }
        var tableUri = table.getUri();
        var module = moduleModel.getModuleInfo();
        if (!module.containsTable(tableUri)) {
            // if table is not in the current module, then need to find it and inititialize module.
            // otherwise, all required listeners and hooks will not function properly
            var pd = getWebStudio().getProjectByName(project.getRepository().getId(), project.getName());
            module = CollectionUtils.findFirst(pd.getModules(), module1 -> module1.containsTable(tableUri));
            // initialize module
            moduleModel = getProjectModel(project, module);
            table = moduleModel.getTableById(tableId);
            if (table == null) {
                throw new NotFoundException("table.message");
            }
        }
        return table;
    }

    /**
     * Update table
     *
     * @param project   project
     * @param tableId   table id
     * @param tableView new table data
     * @throws ProjectException if project is locked by another user
     */
    public void updateTable(RulesProject project, String tableId, EditableTableView tableView) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var table = getOpenLTable(project, tableId);
        var writer = tableWritersFactory.getTableWriter(table, tableView.getTableType());
        getWebStudio().getCurrentProject().tryLockOrThrow();
        tableWriterExecutor.executeWrite(writer, tableView);
    }

    /**
     * Append new lines to table
     *
     * @param project   project
     * @param tableId   table id
     * @param tableView lines to append
     * @throws ProjectException if project is locked by another user
     */
    public void appendTableLines(RulesProject project,
                                 String tableId,
                                 AppendTableView tableView) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var table = getOpenLTable(project, tableId);
        var writer = tableWritersFactory.getTableWriter(table, tableView.getTableType());
        getWebStudio().getCurrentProject().tryLockOrThrow();
        tableWriterExecutor.executeAppend(writer, tableView);
    }

    @Override
    protected String resolveProjectName(RulesProject src) {
        var designRepo = src.getDesignRepository();
        if (designRepo != null && designRepo.supports().mappedFolders()) {
            // if project repository supports mapped folders, then project id should be based on design folder name
            // it's required to align project id when current project is opened or closed
            // if project is opened its name is different from the name in design repository
            var mappingData = src.getFileData().getAdditionalData(FileMappingData.class);
            if (mappingData != null) {
                return FileUtils.getName(mappingData.getExternalPath());
            }
        }
        return super.resolveProjectName(src);
    }

    public void createNewTable(RulesProject project, CreateNewTableRequest createTableRequest) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var projectModel = getProjectModel(project, createTableRequest.moduleName());
        getWebStudio().getCurrentProject().tryLockOrThrow();
        tableCreatorService.createTable(createTableRequest, projectModel);
    }

}
