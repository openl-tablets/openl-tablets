package org.openl.studio.projects.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.springframework.core.env.Environment;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.WebstudioConfiguration;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.rules.webstudio.web.TablePropertiesSelector;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.utils.DateTimes;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.ExposedMethodsViewModel;
import org.openl.studio.projects.model.ModuleViewModel;
import org.openl.studio.projects.model.ProjectBranchInfo;
import org.openl.studio.projects.model.ProjectDependencyViewModel;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
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
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
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
import org.openl.studio.tags.service.TagAssignmentValidator;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
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
    private static final Comparator<Module> MODULES_COMPARATOR = Comparator.comparing(Module::getName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

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
    private final LocalWorkspaceManager localWorkspaceManager;
    private final MultiUserWorkspaceManager workspaceManager;
    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectStatusMapper projectStatusMapper;
    private final Environment environment;
    private final TagAssignmentValidator tagAssignmentValidator;

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
            DetailedMessageDescriptionMapper detailedMessageDescriptionMapper,
            LocalWorkspaceManager localWorkspaceManager,
            MultiUserWorkspaceManager workspaceManager,
            AclProjectsHelper aclProjectsHelper,
            ProjectAccessService projectAccessService,
            ProjectStatusMapper projectStatusMapper,
            Environment environment,
            TagAssignmentValidator tagAssignmentValidator) {
        super(designRepositoryAclService, projectIdentifierMapper, projectAccessService);
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
        this.localWorkspaceManager = localWorkspaceManager;
        this.workspaceManager = workspaceManager;
        this.aclProjectsHelper = aclProjectsHelper;
        this.projectStatusMapper = projectStatusMapper;
        this.environment = environment;
        this.tagAssignmentValidator = tagAssignmentValidator;
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
        return getProject(project, List.of());
    }

    public ProjectViewModel getProject(RulesProject project, Collection<ProjectInclude> includes) {
        var normalizedIncludes = ProjectInclude.normalize(includes);
        var builder = mapWorkspaceProjectResponse(project, Map.of(), normalizedIncludes.contains(ProjectInclude.MODULES));
        try {
            projectDependencyResolver.getDependsOnProject(project).stream()
                    .sorted(PROJECT_BUSINESS_NAME_ORDER)
                    .map(this::mapProjectDependency)
                    .map(ProjectDependencyViewModel.Builder::build)
                    .forEach(builder::addUsedBy);
        } catch (Exception e) {
            // used-by is best-effort: resolving it reads every other project's rules.xml, any of which may
            // be malformed and throw unchecked. A broken neighbour must not fail this project's detail view.
            log.warn("Failed to resolve dependent projects for '{}'", project.getName(), e);
        }
        if (normalizedIncludes.contains(ProjectInclude.STATUS)) {
            builder.compileStatus(projectStatusMapper.map(project));
        }
        return builder.build();
    }

    private Optional<ProjectDescriptor> resolveProjectDescriptor(RulesProject project) {
        var localDescriptor = resolveLocalProjectDescriptor(project);
        var repositoryDescriptor = resolveRepositoryProjectDescriptor(project);

        if (localDescriptor.isEmpty()) {
            return repositoryDescriptor;
        }
        if (repositoryDescriptor.isPresent()) {
            fillMissingDescriptorData(localDescriptor.get(), repositoryDescriptor.get());
        }
        return localDescriptor;
    }

    /**
     * Resolves the project descriptor from the local workspace without opening the project.
     *
     * <p>The resolver matches the legacy Editor behavior: a project may have modules even when they are
     * generated from Excel files instead of being declared directly in {@code rules.xml}.
     */
    private Optional<ProjectDescriptor> resolveLocalProjectDescriptor(RulesProject project) {
        try {
            var localWorkspace = getUserWorkspace().getLocalWorkspace();
            var repoRoot = localWorkspace.getRepository(project.getRepository().getId()).getRoot();
            var folder = repoRoot.resolve(project.getFolderPath());
            var descriptor = ProjectResolver.getInstance().resolve(folder);
            if (descriptor != null) {
                descriptor.getModules().sort(MODULES_COMPARATOR);
            }
            return Optional.ofNullable(descriptor);
        } catch (Exception e) {
            log.debug("Failed to resolve local project descriptor for '{}'", project.getName(), e);
            return Optional.empty();
        }
    }

    private Optional<ProjectDescriptor> resolveRepositoryProjectDescriptor(RulesProject project) {
        try {
            var repositoryDescriptor = readRepositoryProjectDescriptor(project);
            var descriptor = repositoryDescriptor.orElseGet(() -> createSimpleRepositoryDescriptor(project));
            var files = listRepositoryProjectFiles(project);
            descriptor.setModules(expandRepositoryModules(descriptor,
                    project.getFolderPath(),
                    files,
                    repositoryDescriptor.isPresent()));
            descriptor.getModules().sort(MODULES_COMPARATOR);
            return Optional.ofNullable(descriptor);
        } catch (IOException e) {
            log.warn("Failed to resolve repository project descriptor for '{}'", project.getName(), e);
        }
        return Optional.empty();
    }

    private Optional<ProjectDescriptor> readRepositoryProjectDescriptor(RulesProject project) throws IOException {
        var descriptorPath = projectRepositoryPath(project.getFolderPath(), ProjectDescriptor.FILE_NAME);
        var repository = project.getRepository();
        var fileData = project.isHistoric()
                ? repository.checkHistory(descriptorPath, project.getHistoryVersion())
                : repository.check(descriptorPath);
        if (fileData == null) {
            return Optional.empty();
        }
        var item = project.isHistoric()
                ? repository.readHistory(descriptorPath, project.getHistoryVersion())
                : repository.read(descriptorPath);
        if (item == null) {
            return Optional.empty();
        }
        try (var content = item.getStream()) {
            var descriptor = ProjectDescriptor.read(content);
            if (descriptor != null && StringUtils.isBlank(descriptor.getName())) {
                descriptor.setName(project.getBusinessName());
            }
            return Optional.ofNullable(descriptor);
        }
    }

    private static ProjectDescriptor createSimpleRepositoryDescriptor(RulesProject project) {
        var descriptor = new ProjectDescriptor();
        descriptor.setName(project.getBusinessName());
        return descriptor;
    }

    private static List<FileData> listRepositoryProjectFiles(RulesProject project) throws IOException {
        var path = projectRepositoryPath(project.getFolderPath(), "");
        var repository = project.getRepository();
        return project.isHistoric()
                ? repository.listFiles(path, project.getHistoryVersion())
                : repository.list(path);
    }

    private static List<Module> expandRepositoryModules(ProjectDescriptor descriptor,
                                                        String projectPath,
                                                        List<FileData> files,
                                                        boolean hasProjectDescriptor) {
        var readModules = descriptor.getModules();
        if (hasProjectDescriptor && readModules.isEmpty()) {
            readModules = defaultRepositoryModules();
        }

        var modules = new ArrayList<Module>(readModules.size());
        for (var module : readModules) {
            if (!module.isModuleWithWildcard()) {
                normalizeRepositoryModule(descriptor, module);
                modules.add(module);
            }
        }

        for (var module : readModules) {
            if (module.isModuleWithWildcard()) {
                for (var file : files) {
                    var relativePath = relativeProjectFilePath(projectPath, file);
                    if (isNotTemporaryFile(relativePath)
                            && FileUtils.pathMatches(module.getRulesRootPath(), relativePath)
                            && modules.stream()
                                    .noneMatch(existing -> Objects.equals(existing.getRulesRootPath(), relativePath))) {
                        modules.add(createRepositoryModule(descriptor, module, relativePath));
                    }
                }
            }
        }

        if (!hasProjectDescriptor) {
            addSimpleRepositoryModules(descriptor, projectPath, files, modules);
        }
        return modules;
    }

    private static List<Module> defaultRepositoryModules() {
        var rules = new Module();
        rules.setRulesRootPath("rules/**/*.xlsx");
        var tests = new Module();
        tests.setRulesRootPath("tests/**/*.xlsx");
        return List.of(rules, tests);
    }

    private static void normalizeRepositoryModule(ProjectDescriptor descriptor, Module module) {
        module.setProject(descriptor);
        if (StringUtils.isBlank(module.getName()) && StringUtils.isNotBlank(module.getRulesRootPath())) {
            module.setName(FileUtils.getBaseName(module.getRulesRootPath()));
        }
    }

    private static Module createRepositoryModule(ProjectDescriptor descriptor, Module source, String relativePath) {
        var module = new Module();
        module.setProject(descriptor);
        module.setRulesRootPath(relativePath);
        module.setName(FileUtils.getBaseName(relativePath));
        module.setMethodFilter(source.getMethodFilter());
        module.setWildcardName(source.getName());
        module.setWildcardRulesRootPath(source.getRulesRootPath());
        if (source.getWebstudioConfiguration() != null) {
            var webstudioConfiguration = new WebstudioConfiguration();
            webstudioConfiguration
                    .setCompileThisModuleOnly(source.getWebstudioConfiguration().isCompileThisModuleOnly());
            module.setWebstudioConfiguration(webstudioConfiguration);
        }
        return module;
    }

    private static void addSimpleRepositoryModules(ProjectDescriptor descriptor,
                                                   String projectPath,
                                                   List<FileData> files,
                                                   List<Module> modules) {
        for (var file : files) {
            var relativePath = relativeProjectFilePath(projectPath, file);
            if (isDirectProjectExcelFile(relativePath)) {
                var module = new Module();
                module.setProject(descriptor);
                module.setRulesRootPath(relativePath);
                module.setName(FileUtils.getBaseName(relativePath));
                modules.add(module);
            }
        }
    }

    private static boolean isDirectProjectExcelFile(String relativePath) {
        return !relativePath.contains("/")
                && isNotTemporaryFile(relativePath)
                && FileTypeHelper.isExcelFile(relativePath);
    }

    private static boolean isNotTemporaryFile(String path) {
        return !FileUtils.getName(path).startsWith("~$");
    }

    private static String relativeProjectFilePath(String projectPath, FileData file) {
        var prefix = projectRepositoryPath(projectPath, "");
        var filePath = file.getName().replace('\\', '/');
        return filePath.startsWith(prefix) ? filePath.substring(prefix.length()) : filePath;
    }

    private static String projectRepositoryPath(@Nullable String projectPath, String relativePath) {
        var normalizedProjectPath = projectPath == null ? "" : projectPath.replace('\\', '/');
        if (relativePath.isBlank()) {
            return normalizedProjectPath.isEmpty() || normalizedProjectPath.endsWith("/")
                    ? normalizedProjectPath
                    : normalizedProjectPath + "/";
        }
        if (normalizedProjectPath.isEmpty()) {
            return relativePath;
        }
        return normalizedProjectPath.endsWith("/")
                ? normalizedProjectPath + relativePath
                : normalizedProjectPath + "/" + relativePath;
    }

    private void fillMissingDescriptorData(ProjectDescriptor target, ProjectDescriptor fallback) {
        if (target.getComment() == null) {
            target.setComment(fallback.getComment());
        }
        if (target.getModules().isEmpty()) {
            target.getModules().addAll(fallback.getModules());
        }
        if (target.getPropertiesFileNamePatterns() == null || target.getPropertiesFileNamePatterns().length == 0) {
            target.setPropertiesFileNamePatterns(fallback.getPropertiesFileNamePatterns());
        }
        if (target.getExposedMethods() == null) {
            target.setExposedMethods(fallback.getExposedMethods());
        }
    }

    private void applyDescriptor(ProjectViewModel.Builder builder, ProjectDescriptor descriptor) {
        builder.description(descriptor.getComment());
        for (Module module : descriptor.getModules()) {
            builder.addModule(new ModuleViewModel(module.getName(), module.getRulesRootPath()));
        }
        var patterns = descriptor.getPropertiesFileNamePatterns();
        if (patterns != null && patterns.length > 0) {
            builder.versionPatterns(List.of(patterns));
        }
        var exposed = descriptor.getExposedMethods();
        if (exposed != null) {
            var includes = exposed.getIncludes();
            var excludes = exposed.getExcludes();
            var hasIncludes = includes != null && !includes.isEmpty();
            var hasExcludes = excludes != null && !excludes.isEmpty();
            if (hasIncludes || hasExcludes) {
                builder.exposedMethods(new ExposedMethodsViewModel(
                        hasIncludes ? List.copyOf(includes) : List.of(),
                        hasExcludes ? List.copyOf(excludes) : List.of()));
            }
        }
    }

    @Override
    protected ProjectViewModel.Builder mapProjectResponse(RulesProject src,
                                                          ProjectCriteriaQuery query,
                                                          Map<AProject, ProjectStatus> statuses) {
        return mapWorkspaceProjectResponse(src, statuses, query.includeModules());
    }

    private ProjectViewModel.Builder mapWorkspaceProjectResponse(RulesProject src,
                                                                 Map<AProject, ProjectStatus> statuses,
                                                                 boolean includeModules) {
        var builder = super.mapProjectResponse(src, statuses);
        builder.branchProtected(src.isBranchProtected());
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
        if (includeModules) {
            resolveProjectDescriptor(src).ifPresent(descriptor -> applyDescriptor(builder, descriptor));
        }
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
        if (query.hasRepositoryFilter() && !query.localRepositorySelected()) {
            return query.designRepositoryIds().stream()
                    .map(workspace::getProjects)
                    .flatMap(Collection::stream);
        } else {
            projects = workspace.getProjects();
        }
        return projects.stream();
    }

    @Override
    protected Optional<ProjectStatus> statusOf(AProject project) {
        return project instanceof UserWorkspaceProject workspaceProject
                ? Optional.ofNullable(workspaceProject.getStatus())
                : Optional.empty();
    }

    @Override
    protected List<ProjectStatusViewModel> projectStatuses(List<? extends AProject> pageProjects) {
        return pageProjects.stream()
                .map(RulesProject.class::cast)
                .map(projectStatusMapper::map)
                .toList();
    }

    @Override
    @Nonnull
    protected Predicate<AProject> buildStatusFilterCriteria(ProjectCriteriaQuery query,
                                                            Map<AProject, ProjectStatus> statuses) {
        if (query.hasStatusFilter()) {
            // Reuse the status already resolved into the map rather than calling uncached getStatus() again.
            return project -> query.statuses().contains(statuses.get(project));
        }
        return super.buildStatusFilterCriteria(query, statuses);
    }

    @Override
    @Nonnull
    protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
        Predicate<AProject> filter = super.buildFilterCriteria(query);

        if (query.hasRepositoryFilter()) {
            var repositoryIds = Set.copyOf(query.designRepositoryIds());
            filter = filter.and(project -> repositoryIds.contains(project.getRepository().getId())
                    || query.localRepositorySelected() && statusOf(project).orElse(null) == ProjectStatus.LOCAL);
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
        if (project.isModified() && shouldSave(model)) {
            save(project, model);
        }
        if (model.status() == ProjectStatus.VIEWING) {
            if (!project.isOpened() || StringUtils.isNotBlank(model.branch()) || StringUtils.isNotBlank(model.revision())) {
                open(project, Boolean.TRUE.equals(model.openDependencies()), model);
            }
        } else {
            var closeRequested = model.status() == ProjectStatus.CLOSED && project.getStatus() != ProjectStatus.CLOSED;
            if (closeRequested && StringUtils.isNotBlank(model.branch())) {
                switchToBranch(project, model.branch(), Boolean.TRUE.equals(model.discardChanges()));
            }
            if (closeRequested) {
                close(project, Boolean.TRUE.equals(model.discardChanges()));
            }
            if (!closeRequested && StringUtils.isNotBlank(model.branch())) {
                switchToBranch(project, model.branch(), Boolean.TRUE.equals(model.discardChanges()));
            }
        }
        if (CollectionUtils.isNotEmpty(model.selectedBranches())) {
            project.setSelectedBranches(model.selectedBranches());
        }
    }

    private static boolean shouldSave(ProjectStatusUpdateModel model) {
        return Boolean.TRUE.equals(model.save()) || model.comment() != null;
    }

    /**
     * Forcibly release the lock held on a project.
     *
     * @param project locked project to unlock
     */
    public void unlockProject(RulesProject project) {
        requireGranted(project, BasePermission.ADMINISTRATION);
        project.forceUnlock();
    }

    /**
     * Update the tags assigned to a project.
     *
     * @param project project to tag
     * @param tags    tag type to value assignments
     * @throws ProjectException if the tags cannot be saved
     */
    public void updateTags(RulesProject project, @Nullable Map<String, String> tags) throws ProjectException {
        requireGranted(project, BasePermission.WRITE);
        // Tags are editable only on an opened project (legacy getCanModifyTags = isOpened && WRITE).
        if (!project.isOpened()) {
            throw new ConflictException("project.not.opened.message");
        }
        project.saveTags(tagAssignmentValidator.sanitize(tags != null ? tags : Map.of()));
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
        requireGranted(project, BasePermission.WRITE);
        var comment = resolveSaveComment(project, StringUtils.trimToNull(model.comment()));
        validateComment(project, comment);
        project.getFileData().setComment(comment);
        commit(project);
    }

    private String resolveSaveComment(RulesProject project, @Nullable String comment) {
        if (comment != null) {
            return comment;
        }
        var comments = new Comments(environment, project.getRepository().getId());
        if (project.isOpenedOtherVersion()) {
            var fileData = project.getFileData();
            var authorName = Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).orElse(null);
            return comments.restoredFrom(fileData.getVersion(), authorName, fileData.getModifiedAt());
        }
        return comments.saveProject(project.getBusinessName());
    }

    private void requireGranted(AProjectArtefact project, Permission permission) {
        if (!designRepositoryAclService.isGranted(project, List.of(permission))) {
            throw new ForbiddenException("default.message");
        }
    }

    private void validateComment(RulesProject project, @Nullable String comment) {
        try {
            CommentValidator.forRepo(project.getRepository().getId()).validate(comment);
        } catch (Exception e) {
            throw new BadRequestException("repo.invalid.comment.message", new Object[]{e.getMessage()});
        }
    }

    private void commit(RulesProject project) throws ProjectException {
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
        close(project, false);
    }

    private void close(RulesProject project, boolean discardChanges) throws ProjectException {
        var webStudio = getWebStudio();
        requireGranted(project, BasePermission.READ);
        if (project.isDeleted()) {
            throw new ConflictException("project.close.deleted.message", project.getBusinessName());
        } else if (!projectStateValidator.canClose(project)) {
            throw new ConflictException("project.close.conflict.message");
        }
        requireDiscardForModifiedProject(project, discardChanges);
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
        var branch = project.getBranch();
        var supportsBranches = project.isSupportsBranches();
        project.close();
        if (supportsBranches && branch != null && !Objects.equals(branch, project.getBranch())) {
            project.setBranch(branch);
        }
    }

    public void delete(RulesProject project, @Nullable String comment) {
        if (!aclProjectsHelper.hasPermission(project, BasePermission.DELETE)) {
            throw new ForbiddenException("default.message");
        }
        if (!projectStateValidator.canDelete(project)) {
            if (!project.isLocalOnly()
                    && project.getDesignRepository().supports().branches()
                    && project.getVersion() == null) {
                throw new ConflictException("project.delete.branch.message");
            }
            if (project.isLocked()) {
                throw new ConflictException("project.delete.locked.message");
            }
            throw new ConflictException("project.delete.message");
        }

        var normalizedComment = StringUtils.trimToNull(comment);
        validateComment(project, normalizedComment);

        getWebStudio().getModel().clearModuleInfo();
        closeProjectForAllUsers(project);
        try {
            project.delete(normalizedComment);
        } catch (ProjectException e) {
            log.warn("Failed to delete project '{}'", project.getBusinessName(), e);
            throw new ConflictException("project.delete.message");
        }
        if (!project.isLocalOnly()) {
            designRepositoryAclService.deleteAcl(project);
        }
        workspaceManager.refreshWorkspaces();
        getWebStudio().reset();
    }

    /**
     * Closes a project in every user workspace before it is removed from the design repository.
     */
    private void closeProjectForAllUsers(RulesProject project) {
        var businessName = project.getBusinessName();
        var branch = project.getBranch();
        var repoId = project.getRepository().getId();

        try {
            ProjectHistoryService.deleteHistory(businessName);
            if (project.isOpened()) {
                project.close();
            }
        } catch (Exception e) {
            log.warn("Failed to close project '{}' before deletion", businessName, e);
        }

        File workspacesRoot = getUserWorkspace().getLocalWorkspace().getLocation().getParentFile();
        File[] files = workspacesRoot.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            closeProjectInWorkspace(file, repoId, businessName, branch);
        }
    }

    private void closeProjectInWorkspace(File file, String repoId, String businessName, @Nullable String branch) {
        if (!file.isDirectory() || LockEngineImpl.LOCKS_FOLDER_NAME.equals(file.getName())) {
            return;
        }
        try {
            LocalRepository repository = localWorkspaceManager.getWorkspace(file.getName()).getRepository(repoId);
            repository.initialize();

            ProjectState projectState = repository.getProjectState(businessName);
            var savedRepoId = projectState.getRepositoryId();
            FileData savedData = projectState.getFileData();
            var savedBranch = savedData == null ? null : savedData.getBranch();

            if (Objects.equals(savedRepoId, repoId) && Objects.equals(savedBranch, branch)) {
                FileData fileData = new FileData();
                fileData.setName(businessName);
                repository.delete(fileData);
            }
        } catch (Exception e) {
            log.warn("Failed to close project '{}' in workspace '{}'", businessName, file.getName(), e);
        }
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
        requireGranted(project, BasePermission.READ);
        var workspace = getUserWorkspace();
        var wasOpened = project.isOpened();
        var changeView = StringUtils.isNotBlank(model.branch()) || StringUtils.isNotBlank(model.revision());
        if (project.isDeleted()) {
            throw new ConflictException("project.open.deleted.message", project.getBusinessName());
        } else if (project.isLocalOnly() || (!wasOpened || !changeView) && !projectStateValidator.canOpen(project)) {
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
        // Do we really need to check this if we have a version?
        if (workspace.isOpenedOtherProject(project)) {
            throw new ConflictException("open.duplicated.project");
        }

        var webStudio = getWebStudio();
        if (wasOpened && changeView) {
            requireDiscardForModifiedProject(project, Boolean.TRUE.equals(model.discardChanges()));
            // We must clear module info and release project lock
            // because project was already opened and we are going to open it in another branch or revision
            webStudio.getModel().clearModuleInfo();
            project.releaseMyLock();
        }

        if (StringUtils.isNotBlank(model.branch())) {
            switchToBranch(project, model.branch(), Boolean.TRUE.equals(model.discardChanges()));
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

    private void switchToBranch(RulesProject project, String branchName, boolean discardChanges) throws ProjectException {
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
            requireDiscardForModifiedProject(project, discardChanges);
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

    private static void requireDiscardForModifiedProject(RulesProject project, boolean discardChanges) {
        if (project.isModified() && !discardChanges) {
            throw new ConflictException("project.close.modified.message");
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
        requireGranted(project, BasePermission.READ);
        var repository = (BranchRepository) project.getDesignRepository();
        boolean bypassEligible = bypassService.isBypassEligible(project);
        var baseBranch = repository.getBaseBranch();
        try {
            // projectPath parameter is not required because we need all branches for repository, not only selected project branches
            var branches = repository.getBranches(null);
            var statuses = repository.getBranchStatuses(branches);
            return branches.stream()
                    .map(branch -> toBranchInfo(repository, branch, baseBranch, bypassEligible, statuses))
                    .sorted(Comparator.comparing(ProjectBranchInfo::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            throw new ProjectException("Failed to retrieve branches", e);
        }
    }

    private ProjectBranchInfo toBranchInfo(BranchRepository repository, String branch, String baseBranch,
                                           boolean bypassEligible, Map<String, BranchStatus> statuses) {
        boolean isProtected = repository.isBranchProtected(branch);
        var builder = ProjectBranchInfo.builder()
                .name(branch)
                .protectedFlag(isProtected)
                .base(branch.equals(baseBranch))
                .bypassEligible(isProtected && bypassEligible);
        try {
            var status = statuses.get(branch);
            if (status != null) {
                builder.lastCommit(ProjectBranchInfo.LastCommit.builder()
                        .author(status.lastCommitAuthor() == null ? null : status.lastCommitAuthor().getName())
                        .modifiedAt(DateTimes.atSystemZone(status.lastCommitAt()))
                        .message(status.lastCommitMessage())
                        .revision(status.lastCommitRevision())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to read status for branch '{}'", branch, e);
        }
        return builder.build();
    }

    /**
     * Delete a branch from the repository that hosts the project.
     *
     * <p>The repository main branch cannot be deleted, and a missing branch is reported as not found. When the project
     * is currently opened on the branch being deleted, it is released first. Deleting a protected branch requires an
     * eligible user and the {@code force} flag.
     *
     * <p>A branch on which the project is locked by another user cannot be deleted: the lock means the user
     * is editing the project there. The lock owner can delete the branch.
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
        var repositoryId = repository.getId();
        var projectName = project.getBusinessName();
        try {
            if (!repository.branchExists(branchName)) {
                throw new NotFoundException("repository.branch.message");
            }
            requireNotLockedByAnotherUser(project, repository, branchName);
            bypassService.requireBypassOrThrow(repository, branchName, project, force);
            var restoreOpenedState = releaseProjectOnBranch(project, branchName);
            repository.deleteBranch(null, branchName);
            var workspace = getUserWorkspace();
            workspace.refresh();
            if (restoreOpenedState) {
                open(findWorkspaceProject(workspace, repositoryId, projectName), false);
            }
        } catch (IOException | ProjectException e) {
            log.warn("Failed to delete branch '{}' from project '{}'", branchName, project.getBusinessName(), e);
            throw new ConflictException("project.branch.delete.failed.message");
        }
    }

    private RulesProject findWorkspaceProject(UserWorkspace workspace,
                                              String repositoryId,
                                              String projectName) throws ProjectException {
        return workspace.getProjectsByName(projectName, false)
                .stream()
                .filter(project -> repositoryId.equals(project.getDesignRepository().getId()))
                .findFirst()
                .orElseThrow(() -> new ProjectException(
                        "Cannot find project ''{0}'' or access to the project is not permitted.",
                        null,
                        projectName));
    }

    /**
     * Rejects the branch deletion when the project on that branch is locked by another user.
     *
     * <p>The lock key includes the branch, so the check targets the branch being deleted, not the branch
     * the caller is currently on. The caller's own lock does not block the deletion.
     *
     * <p>The lock is looked up by the caller-visible project path. A project moved or renamed inside the
     * target branch of a mapped repository is not matched: the lock model identifies a project by its
     * path, and every lock check shares this limitation.
     *
     * @param project    project that identifies the lock
     * @param repository repository that hosts the branch
     * @param branchName branch to delete
     */
    private void requireNotLockedByAnotherUser(RulesProject project, BranchRepository repository, String branchName) {
        var lockInfo = getUserWorkspace().getProjectsLockEngine()
                .getLockInfo(repository.getId(), branchName, project.getRealPath());
        if (lockInfo.isLocked() && !lockInfo.getLockedBy().equals(getUserWorkspace().getUser().getUserName())) {
            throw new ConflictException("project.branch.delete.locked.message", lockInfo.getLockedBy());
        }
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
    private boolean releaseProjectOnBranch(RulesProject project, String branchName) throws IOException, ProjectException {
        if (!Objects.equals(project.getBranch(), branchName)) {
            return false;
        }
        ProjectHistoryService.deleteHistory(project.getBusinessName());
        getWebStudio().getModel().clearModuleInfo();
        var wasOpened = project.isOpened();
        if (wasOpened) {
            project.close();
        }
        return wasOpened;
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

        return PageResponse.of(content, page, total);
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
        return getTableRaw(project, tableId, null, null, false);
    }

    /**
     * Get a window of a table in raw format: the {@code maxRows} rows starting at {@code startRow} and,
     * optionally, each cell's Excel style.
     * <p>
     * The window lets a caller page through a large table in slices. Cell addresses stay absolute, and
     * {@code totalRows} is set whenever the window omits rows.
     *
     * @param project    project
     * @param tableId    table id
     * @param startRow   zero-based index of the first row to return, or {@code null} for the top
     * @param maxRows    maximum number of rows to return from {@code startRow}, or {@code null} for every
     *                   remaining row
     * @param withStyles whether to attach each cell's Excel style (background, font, alignment)
     * @return raw table data, with {@code totalRows} set when the window omits rows
     */
    public RawTableView getTableRaw(RulesProject project, String tableId, @Nullable Integer startRow,
            @Nullable Integer maxRows, boolean withStyles) {
        var context = getOpenLTable(project, tableId);
        var tableView = rawTableReader.read(context.table(), startRow, maxRows, withStyles);
        tableView.messages = mapMessages(context);
        return tableView;
    }

    private OpenLTableContext getOpenLTable(RulesProject project, String tableId) {
        return getOpenLTable(project, tableId, false);
    }

    /**
     * Resolve a table by id. When {@code editable} is set, a table that belongs to a dependency project is
     * rejected: it can be rendered read-only, but writing it here would mutate another project's source while
     * only the current project is locked and ACL-checked.
     */
    private OpenLTableContext getOpenLTable(RulesProject project, String tableId, boolean editable) {
        var moduleModel = openProject(project).awaitCompiled();
        var table = moduleModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }
        var tableUri = table.getUri();
        var module = moduleModel.getModuleInfo();
        if (!module.containsTable(tableUri)) {
            var pd = getProjectDescriptor(project);
            var owningModule = CollectionUtils.findFirst(pd.getModules(), module1 -> module1.containsTable(tableUri));
            // A table in another module of this project needs that module opened so its listeners and hooks
            // function. A table from a dependency project belongs to no module of this project; it was already
            // resolved across dependencies and carries its grid, so it is rendered as-is (read-only view) — but it
            // cannot be edited here, where the lock and the ACL check apply to the current project, not its owner.
            if (owningModule != null) {
                moduleModel = openProject(pd, project, owningModule).awaitCompiled();
                table = moduleModel.getTableById(tableId);
                if (table == null) {
                    throw new NotFoundException("table.message");
                }
            } else if (editable) {
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
        requireGranted(project, BasePermission.WRITE);
        var context = getOpenLTable(project, tableId, true);
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
        requireGranted(project, BasePermission.WRITE);
        var context = getOpenLTable(project, tableId, true);
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
        requireGranted(project, BasePermission.WRITE);
        var context = getOpenLTable(project, tableId, true);
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
        requireGranted(project, BasePermission.WRITE);
        var context = getOpenLTable(project, tableId, true);
        var writer = tableWritersFactory.getTableWriter(context.table(), RawTableView.TABLE_TYPE);
        getWebStudio().getCurrentProject().tryLockOrThrow();
        writer.delete();
    }

    public void createNewTable(RulesProject project, CreateNewTableRequest createTableRequest) throws ProjectException {
        requireGranted(project, BasePermission.WRITE);
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
