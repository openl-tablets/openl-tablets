package org.openl.studio.projects.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
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
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.rules.webstudio.web.TablePropertiesSelector;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.ProjectBranchInfo;
import org.openl.studio.projects.model.ProjectDependencyViewModel;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.studio.projects.service.merge.SaveMergeConflictEvent;
import org.openl.studio.projects.service.project.compile.CompilationJobRegistry;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
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
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;

/**
 * Implementation of project service for workspace projects.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
@Slf4j
public class WorkspaceProjectService extends AbstractProjectService<RulesProject> {

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
    private final ApplicationEventPublisher eventPublisher;
    private final ProtectedBranchBypassService bypassService;
    private final DetailedMessageDescriptionMapper detailedMessageDescriptionMapper;

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
            TableWritersFactory tableWritersFactory,
            ApplicationEventPublisher eventPublisher,
            ProtectedBranchBypassService bypassService,
            ProjectIdentifierMapper projectIdentifierMapper,
            DetailedMessageDescriptionMapper detailedMessageDescriptionMapper) {
        super(designRepositoryAclService, projectIdentifierMapper);
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
        this.eventPublisher = eventPublisher;
        this.bypassService = bypassService;
        this.detailedMessageDescriptionMapper = detailedMessageDescriptionMapper;
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @Lookup
    public CompilationJobRegistry getCompilationJobRegistry() {
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
                log.warn("Failed to retrieve project branches", e);
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
                .id(projectIdentifierMapper.map(src))
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
                    var dependencyId = projectIdentifierMapper.map(dependency);
                    return dependencyId.equals(query.dependsOn());
                });
            });
        }

        return filter;
    }

    public void updateProjectStatus(RulesProject project, ProjectStatusUpdateModel model) throws ProjectException {
        if (model.status() != null && !ALLOWED_STATUSES.contains(model.status())) {
            throw new BadRequestException("invalid.project.status.message");
        }
        if (project.isModified() && model.comment() != null) {
            save(project, model);
        }
        if (model.status() == ProjectStatus.VIEWING) {
            if (!project.isOpened() || StringUtils.isNotBlank(model.branch()) || StringUtils.isNotBlank(model.revision())) {
                open(project, false, model);
            }
        } else {
            if (model.status() == ProjectStatus.CLOSED && project.getStatus() != ProjectStatus.CLOSED) {
                close(project);
            }
            if (StringUtils.isNotBlank(model.branch())) {
                switchToBranch(project, model.branch());
            }
        }
        if (CollectionUtils.isNotEmpty(model.selectedBranches())) {
            project.setSelectedBranches(model.selectedBranches());
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
        if (!projectStateValidator.canSave(project) || project.isLocalOnly()) {
            throw new ConflictException("project.save.conflict.message");
        }
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var comment = StringUtils.trimToNull(model.comment());
        try {
            CommentValidator.forRepo(project.getRepository().getId()).validate(comment);
        } catch (Exception e) {
            throw new BadRequestException("repo.invalid.comment.message", new Object[]{e.getMessage()});
        }
        project.getFileData().setComment(comment);
        try {
            getWebStudio().saveProject(project);
        } catch (ProjectException e) {
            if (e.getCause() instanceof MergeConflictException mergeConflictEx) {
                var conflictInfo = MergeConflictInfo.builder()
                        .details(mergeConflictEx.getDetails())
                        .project(project)
                        .build();
                eventPublisher.publishEvent(new SaveMergeConflictEvent(project, conflictInfo));
                throw new ConflictException("project.save.merge.conflict.message");
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
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
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
        open(project, openDependencies, ProjectStatusUpdateModel.builder().build());
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

        if (StringUtils.isNotBlank(model.revision())) {
            AProject historic = new AProject(project.getDesignRepository(),
                    project.getDesignFolderName(),
                    model.revision());
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
        if (wasOpened && (StringUtils.isNotBlank(model.branch()) || StringUtils.isNotBlank(model.revision()))) {
            // We must clear module info and release project lock
            // because project was already opened and we are going to open it in another branch or revision
            webStudio.getModel().clearModuleInfo();
            project.releaseMyLock();
        }

        if (StringUtils.isNotBlank(model.branch())) {
            switchToBranch(project, model.branch());
        }

        if (StringUtils.isNotBlank(model.revision())) {
            project.openVersion(model.revision());
        } else {
            if (StringUtils.isNotBlank(model.branch()) || !wasOpened) {
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
            if (log.isDebugEnabled()) {
                log.debug("Project '{}' is already opened in branch '{}'", project.getBusinessName(), branchName);
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
                    if (log.isDebugEnabled()) {
                        log.debug(e.getMessage(), e);
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
        if (!hasManageBranchPermissions(project)) {
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

    public List<ProjectBranchInfo> getBranches(RulesProject project) throws ProjectException {
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            throw new ForbiddenException("default.message");
        }
        var repository = (BranchRepository) project.getDesignRepository();
        boolean bypassEligible = bypassService.isBypassEligible(project);
        try {
            // projectPath parameter is not required because we need all branches for repository, not only selected project branches
            return repository.getBranches(null).stream()
                    .map(branch -> {
                        boolean isProtected = repository.isBranchProtected(branch);
                        return ProjectBranchInfo.builder()
                                .name(branch)
                                .protectedFlag(isProtected)
                                .bypassEligible(isProtected && bypassEligible)
                                .build();
                    })
                    .sorted(Comparator.comparing(ProjectBranchInfo::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            throw new ProjectException("Failed to retrieve branches", e);
        }
    }

    /**
     * Delete a branch from the repository that hosts the project.
     *
     * <p>The repository main branch cannot be deleted, and a missing branch is reported as not found. When the project
     * is currently opened on the branch being deleted, it is released first. Deleting a protected branch requires an
     * eligible user and the {@code force} flag.
     *
     * @param project    project that identifies the target repository
     * @param branchName branch to delete
     * @param force      confirmation flag to bypass protected-branch restrictions
     */
    public void deleteBranch(RulesProject project, String branchName, boolean force) {
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        if (!hasManageBranchPermissions(project)) {
            throw new ForbiddenException("default.message");
        }
        var repository = (BranchRepository) project.getDesignRepository();
        if (Objects.equals(repository.getBaseBranch(), branchName)) {
            throw new ConflictException("project.branch.delete.base.message");
        }
        try {
            if (!repository.branchExists(branchName)) {
                throw new NotFoundException("repository.branch.message");
            }
            bypassService.requireBypassOrThrow(repository, branchName, project, force);
            releaseProjectOnBranch(project, branchName);
            repository.deleteBranch(null, branchName);
        } catch (IOException | ProjectException e) {
            log.warn("Failed to delete branch '{}' from project '{}'", branchName, project.getBusinessName(), e);
            throw new ConflictException("project.branch.delete.failed.message");
        }
        getUserWorkspace().refresh();
    }

    /**
     * Release the project when it is currently opened on the branch being deleted. Clears the cached history and module
     * info and closes the project so that no resources keep referencing the branch.
     *
     * @param project    project to release
     * @param branchName branch about to be deleted
     * @throws IOException      if the project history cannot be cleared
     * @throws ProjectException if the project cannot be closed
     */
    private void releaseProjectOnBranch(RulesProject project, String branchName) throws IOException, ProjectException {
        if (!Objects.equals(project.getBranch(), branchName)) {
            return;
        }
        ProjectHistoryService.deleteHistory(project.getBusinessName());
        getWebStudio().getModel().clearModuleInfo();
        if (project.isOpened()) {
            project.close();
        }
    }

    private boolean hasManageBranchPermissions(RulesProject project) {
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
     * @param page    pagination parameters
     * @return project tables
     */
    public PageResponse<SummaryTableView> getTables(RulesProject project,
                                                    ProjectTableCriteriaQuery query,
                                                    Pageable page) {
        var moduleModel = openProject(project).awaitCompiled();

        var selectors = buildTableSelector(query);
        var allTables = moduleModel.search(selectors, SearchScope.CURRENT_PROJECT)
                .stream()
                .map(summaryTableReader::read)
                .sorted(Comparator.comparing(view -> view.name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        long total = allTables.size();

        var content = allTables.stream()
                .skip(page.getOffset())
                .limit(page.getPageSize())
                .toList();

        if (page.isUnpaged()) {
            return new PageResponse<>(content, -1, content.size(), total);
        }
        return new PageResponse<>(content, page.getPageNumber(), page.getPageSize(), total);
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
            var nameFilter = query.getName().get().toLowerCase();
            selectors = selectors.and(tsn -> {
                var type = XlsNodeTypes.getEnumByValue(tsn.getType());
                var header = tsn.getHeader();
                var displayName = TableSyntaxNodeUtils.str2name(header.getSourceString(), type);
                return displayName.toLowerCase().contains(nameFilter);
            });
        }

        if (CollectionUtils.isNotEmpty(query.getProperties())) {
            selectors = selectors.and(new TablePropertiesSelector(query.getProperties()));
        }

        return selectors;
    }

    /**
     * Open the project's default module and return a non-blocking handle to the
     * resulting {@link ProjectModel} together with its asynchronous compilation
     * job. The project model becomes available immediately; callers that need a
     * compiled model should use {@link ProjectHandle#awaitCompiled()} or wait on
     * {@link org.openl.studio.projects.service.project.compile.CompilationJob#future()}.
     *
     * @param project workspace project to open
     * @return handle exposing the project model and its compilation job
     */
    public ProjectHandle openProject(RulesProject project) {
        return openProject(project, (String) null);
    }

    /**
     * Open a specific module of the given project and return a non-blocking
     * handle.
     *
     * @param project    workspace project to open
     * @param moduleName name of the module to open, or {@code null} to pick the
     *                   first module
     * @return handle exposing the project model and its compilation job
     */
    public ProjectHandle openProject(RulesProject project, @Nullable String moduleName) {
        var projectDescriptor = getProjectDescriptor(project);
        var moduleSelector = projectDescriptor.getModules().stream();
        if (moduleName != null) {
            moduleSelector = moduleSelector.filter(module -> module.getName() != null && module.getName().equals(moduleName));
        }
        var module = moduleSelector.findFirst().orElse(null);
        return openProject(projectDescriptor, project, module);
    }

    private ProjectDescriptor getProjectDescriptor(RulesProject project) {
        if (!project.isOpened()) {
            throw new ConflictException("project.not.opened.message");
        }
        var webstudio = getWebStudio();
        var projectName = project.getName();
        if (project.isLocalOnly()) {
            // The case when in local project is not linked to any design repository,
            // so project name is may not equal to it is business name.
            // In that case we should resolve project descriptor manually. and get real project name from it.
            var localWorkspace = getUserWorkspace().getLocalWorkspace();
            var repoRoot = localWorkspace.getRepository(project.getRepository().getId()).getRoot();
            var folder = repoRoot.resolve(project.getFolderPath());
            try {
                var pd = ProjectResolver.getInstance().resolve(folder);
                projectName = pd.getName();
            } catch (ProjectResolvingException e) {
                // If project descriptor cannot be resolved, then we cannot open project and get model.
                // Usually it means that project folder is corrupted or has invalid structure.
                // User can do nothing with such project until the problem is fixed, so we should not silently ignore that error and return null.
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

        var projectDescriptor = webstudio.getProjectByName(project.getRepository().getId(), projectName);
        if (projectDescriptor == null) {
            throw new NotFoundException("project.identifier.message");
        }
        return projectDescriptor;
    }

    private ProjectHandle openProject(ProjectDescriptor projectDescriptor, RulesProject project, @Nullable Module module) {
        if (module == null) {
            throw new NotFoundException("project.identifier.message");
        }
        var webstudio = getWebStudio();
        webstudio.init(project.getRepository().getId(), project.getBranch(), projectDescriptor.getName(), module.getName());
        var moduleModel = webstudio.getModel();
        var job = getCompilationJobRegistry().acquire(projectIdentifierMapper.map(project), moduleModel);
        return ProjectHandle.of(moduleModel, job);
    }

    /**
     * Get table data
     *
     * @param project project
     * @param tableId table id
     * @return table data
     */
    public TableView getTable(RulesProject project, String tableId) {
        var context = getOpenLTable(project, tableId);
        var table = context.table();
        var reader = readers.stream()
                .filter(r -> r.supports(table))
                .findFirst()
                .orElse(null);
        var tableView = reader != null ? reader.read(table) : rawTableReader.read(table);
        tableView.messages = mapMessages(context);
        return tableView;
    }

    private List<DetailedMessageDescription> mapMessages(OpenLTableContext context) {
        var messages = context.getMessages().values().stream()
                .flatMap(Collection::stream)
                .toList();
        return detailedMessageDescriptionMapper.mapSorted(messages, context.module());
    }

    /**
     * Get table in raw format as 2D matrix with merge information
     *
     * @param project project
     * @param tableId table id
     * @return raw table data
     */
    public RawTableView getTableRaw(RulesProject project, String tableId) {
        var context = getOpenLTable(project, tableId);
        var tableView = rawTableReader.read(context.table());
        tableView.messages = mapMessages(context);
        return tableView;
    }

    private OpenLTableContext getOpenLTable(RulesProject project, String tableId) {
        var moduleModel = openProject(project).awaitCompiled();
        var table = moduleModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }
        var tableUri = table.getUri();
        var module = moduleModel.getModuleInfo();
        if (!module.containsTable(tableUri)) {
            // if table is not in the current module, then need to find it and inititialize module.
            // otherwise, all required listeners and hooks will not function properly
            var pd = getProjectDescriptor(project);
            module = CollectionUtils.findFirst(pd.getModules(), module1 -> module1.containsTable(tableUri));
            // initialize module
            moduleModel = openProject(pd, project, module).awaitCompiled();
            table = moduleModel.getTableById(tableId);
            if (table == null) {
                throw new NotFoundException("table.message");
            }
        }
        return new OpenLTableContext(table, moduleModel);
    }

    /**
     * Update table
     *
     * @param project   project
     * @param tableId   table id
     * @param tableView new table data
     * @return table id after the write; differs from {@code tableId} when the table was relocated to grow
     * @throws ProjectException if project is locked by another user
     */
    public String updateTable(RulesProject project, String tableId, EditableTableView tableView) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var context = getOpenLTable(project, tableId);
        var writer = tableWritersFactory.getTableWriter(context.table(), tableView.getTableType());
        getWebStudio().getCurrentProject().tryLockOrThrow();
        return tableWriterExecutor.executeWrite(writer, tableView);
    }

    /**
     * Append new lines to table
     *
     * @param project   project
     * @param tableId   table id
     * @param tableView lines to append
     * @return table id after the append; differs from {@code tableId} when the table was relocated to grow
     * @throws ProjectException if project is locked by another user
     */
    public String appendTableLines(RulesProject project,
                                   String tableId,
                                   AppendTableView tableView) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var context = getOpenLTable(project, tableId);
        var writer = tableWritersFactory.getTableWriter(context.table(), tableView.getTableType());
        getWebStudio().getCurrentProject().tryLockOrThrow();
        return tableWriterExecutor.executeAppend(writer, tableView);
    }

    /**
     * Apply a single raw-source edit to a table.
     * <p>
     * The table is always handled in the raw format regardless of its type. The concrete edit (append, insert or delete
     * a row or a column, or update a cell) is carried by the action.
     *
     * @param project project
     * @param tableId table id
     * @param action  the edit to apply
     * @return table id after the edit; differs from {@code tableId} when the table was relocated to grow
     * @throws ProjectException if project is locked by another user
     */
    public String editTableSource(RulesProject project,
                                  String tableId,
                                  RawTableSourceAction action) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var context = getOpenLTable(project, tableId);
        var writer = tableWritersFactory.getTableWriter(context.table(), RawTableView.TABLE_TYPE);
        getWebStudio().getCurrentProject().tryLockOrThrow();
        return tableWriterExecutor.executeSourceAction(writer, action);
    }

    /**
     * Delete a table from the currently opened project.
     * <p>
     * The whole table area is cleared from the sheet regardless of the table type. The table no longer exists once the
     * project is recompiled.
     *
     * @param project project
     * @param tableId table id
     * @throws ProjectException if project is locked by another user
     */
    public void deleteTable(RulesProject project, String tableId) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var context = getOpenLTable(project, tableId);
        var writer = tableWritersFactory.getTableWriter(context.table(), RawTableView.TABLE_TYPE);
        getWebStudio().getCurrentProject().tryLockOrThrow();
        writer.delete();
    }

    public void createNewTable(RulesProject project, CreateNewTableRequest createTableRequest) throws ProjectException {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("default.message");
        }
        var projectModel = openProject(project, createTableRequest.moduleName()).awaitCompiled();
        getWebStudio().getCurrentProject().tryLockOrThrow();
        tableCreatorService.createTable(createTableRequest, projectModel);
    }

    private record OpenLTableContext(
            @NotNull
            IOpenLTable table,
            @NotNull
            ProjectModel module
    ) {

        public Map<Severity, List<OpenLMessage>> getMessages() {
            var tableUri = table.getUri();
            return Stream.of(Severity.values())
                    .flatMap(severity -> module.getMessagesByTsn(tableUri, severity).stream())
                    .collect(Collectors.groupingBy(OpenLMessage::getSeverity));
        }

    }

}
