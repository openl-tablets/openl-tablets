package org.openl.studio.projects.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
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
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.compile.OpenLTableLogic;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.rules.webstudio.web.TablePropertiesSelector;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService.IndexHealth;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService.IndexState;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.BranchScope;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.DescriptorViewModel;
import org.openl.studio.projects.model.ModuleViewModel;
import org.openl.studio.projects.model.ProjectBranchInfo;
import org.openl.studio.projects.model.ProjectDependencyViewModel;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectRepositoryModel;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.studio.projects.model.tables.AppendTableView;
import org.openl.studio.projects.model.tables.CopyTableRequest;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TablePropertiesView;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.studio.projects.service.merge.SaveMergeConflictEvent;
import org.openl.studio.projects.service.project.compile.CompilationJobRegistry;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.TableCopyService;
import org.openl.studio.projects.service.tables.TableCreatorService;
import org.openl.studio.projects.service.tables.TablePropertiesService;
import org.openl.studio.projects.service.tables.TableVersionService;
import org.openl.studio.projects.service.tables.read.EditableTableReader;
import org.openl.studio.projects.service.tables.read.RawTableReader;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.studio.projects.validator.NewBranchValidator;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.studio.repositories.model.RepositoryFeatures;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StreamUtils;
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
    private static final long PROJECT_INDEX_TIMEOUT_SECONDS = 30;
    /** The mark {@link TableSyntaxNodeUtils} appends to a display name it had to shorten. */
    private static final String SHORTENED_NAME_MARK = "...";
    private static final Comparator<ProjectDependency> DEPENDENCY_NAME_ORDER = Comparator
            .comparing(ProjectDependency::name, String.CASE_INSENSITIVE_ORDER);

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
    private final TableCopyService tableCopyService;
    private final TablePropertiesService tablePropertiesService;
    private final TableVersionService tableVersionService;
    private final ProjectMetadataService metadataService;
    private final TableWritersFactory tableWritersFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final ProtectedBranchBypassService bypassService;
    private final DetailedMessageDescriptionMapper detailedMessageDescriptionMapper;
    private final LocalWorkspaceManager localWorkspaceManager;
    private final MultiUserWorkspaceManager workspaceManager;
    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectStatusMapper projectStatusMapper;
    private final Environment environment;
    private final ProjectTagsCache projectTagsCache;
    private final ProjectListingContext listingContext;
    private final ObjectFactory<UserWorkspace> userWorkspaceFactory;

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
            TableCopyService tableCopyService,
            TablePropertiesService tablePropertiesService,
            TableVersionService tableVersionService,
            ProjectMetadataService metadataService,
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
            ProjectTagsCache projectTagsCache,
            ProjectListingContext listingContext,
            ObjectFactory<UserWorkspace> userWorkspaceFactory) {
        super(designRepositoryAclService, projectIdentifierMapper, projectAccessService);
        this.projectStateValidator = projectStateValidator;
        this.projectDependencyResolver = projectDependencyResolver;
        this.summaryTableReader = summaryTableReader;
        this.rawTableReader = rawTableReader;
        this.readers = readers;
        this.newBranchValidatorFactory = newBranchValidatorFactory;
        this.validationProvider = validationProvider;
        this.tableCreatorService = tableCreatorService;
        this.tableCopyService = tableCopyService;
        this.tablePropertiesService = tablePropertiesService;
        this.tableVersionService = tableVersionService;
        this.metadataService = metadataService;
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
        this.projectTagsCache = projectTagsCache;
        this.listingContext = listingContext;
        this.userWorkspaceFactory = userWorkspaceFactory;
    }

    /**
     * The workspace of the current user.
     *
     * <p>The workspace lives for the session while this service is a singleton, so it is resolved on each call.
     */
    public UserWorkspace getUserWorkspace() {
        return userWorkspaceFactory.getObject();
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @Lookup
    public CompilationJobRegistry getCompilationJobRegistry() {
        return null;
    }

    /**
     * The strategy naming the properties of a spreadsheet result bean in the currently open project.
     *
     * <p>A project chooses how its spreadsheet steps are named in JSON through its deployment configuration. The run
     * and tests APIs write spreadsheet results the way OpenL Rule Services publishes them, so they follow the same
     * strategy.
     *
     * @return the naming strategy of the project, or {@code null} when the project names steps as they are written
     */
    @Nullable
    public SpreadsheetResultBeanPropertyNamingStrategy getSpreadsheetResultNamingStrategy() {
        var studio = getWebStudio();
        var namingStrategy = ProjectJacksonObjectMapperFactoryBean.extractPropertyNamingStrategy(
                studio.getCurrentProjectRulesDeploy(),
                studio.getModel().getCompiledOpenClass().getClassLoader());
        return namingStrategy instanceof SpreadsheetResultBeanPropertyNamingStrategy spr ? spr : null;
    }

    public ProjectViewModel getProject(RulesProject project) {
        return getProject(project, List.of());
    }

    public ProjectViewModel getProject(RulesProject project, Collection<ProjectInclude> includes) {
        var normalizedIncludes = ProjectInclude.normalize(includes);
        var builder = mapWorkspaceProjectResponse(project, Map.of(), normalizedIncludes.contains(ProjectInclude.DESCRIPTOR));
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

    /**
     * A project as its descriptor describes it: the modules {@code rules.xml} declares, and the modules
     * those declarations resolve to once every pattern is replaced by the files it matches.
     *
     * @param modulesDefaulted whether the modules come from the engine's defaults because {@code rules.xml}
     *                         declares none — such modules are not in the file
     */
    private record ResolvedDescriptor(ProjectDescriptor descriptor, List<Module> declaredModules,
                                      boolean modulesDefaulted) {
    }

    /** The modules a project starts from: what it declares, or the defaults when it declares none. */
    private record DeclaredModules(List<Module> modules, boolean defaulted) {
    }

    private Optional<ResolvedDescriptor> resolveProjectDescriptor(RulesProject project) {
        var localDescriptor = resolveLocalProjectDescriptor(project);
        if (localDescriptor.isEmpty()) {
            // Nothing in the local workspace to read: the repository is the only source.
            return resolveRepositoryProjectDescriptor(project);
        }
        if (!hasMissingData(localDescriptor.get().descriptor())) {
            // The local workspace already answers the whole descriptor, so reading the repository
            // (its rules.xml and its full file listing) would only re-supply fields already present.
            return localDescriptor;
        }
        var repositoryDescriptor = resolveRepositoryProjectDescriptor(project);
        return repositoryDescriptor.isPresent()
                ? Optional.of(fillMissingDescriptorData(localDescriptor.get(), repositoryDescriptor.get()))
                : localDescriptor;
    }

    /**
     * Resolves the project descriptor from the local workspace without opening the project.
     *
     * <p>The resolver matches the legacy Editor behavior: a project may have modules even when they are
     * generated from Excel files instead of being declared directly in {@code rules.xml}.
     */
    private Optional<ResolvedDescriptor> resolveLocalProjectDescriptor(RulesProject project) {
        try {
            var localWorkspace = getUserWorkspace().getLocalWorkspace();
            var repoRoot = localWorkspace.getRepository(project.getRepository().getId()).getRoot();
            var folder = repoRoot.resolve(project.getFolderPath());
            var descriptor = ProjectResolver.getInstance().resolve(folder);
            if (descriptor == null) {
                return Optional.empty();
            }
            descriptor.getModules().sort(MODULES_COMPARATOR);
            var declared = readDeclaredModules(folder);
            return Optional.of(new ResolvedDescriptor(descriptor, declared.modules(), declared.defaulted()));
        } catch (Exception e) {
            log.debug("Failed to resolve local project descriptor for '{}'", project.getName(), e);
            return Optional.empty();
        }
    }

    /**
     * The modules {@code rules.xml} declares, read from the file itself.
     *
     * <p>The resolver answers with the modules the project resolved to, where a pattern is already
     * replaced by the files it matched — a pattern that matched nothing leaves no trace there at all.
     * The declarations are read separately so the screen can show them as they are written.
     */
    private static DeclaredModules readDeclaredModules(Path projectFolder) {
        var descriptorFile = projectFolder.resolve(ProjectDescriptor.FILE_NAME);
        if (!Files.isRegularFile(descriptorFile)) {
            return new DeclaredModules(List.of(), false);
        }
        try (var content = Files.newInputStream(descriptorFile)) {
            var descriptor = ProjectDescriptor.read(content);
            return descriptor == null ? new DeclaredModules(List.of(), false)
                    : declaredOrDefaultModules(descriptor.getModules());
        } catch (Exception e) {
            log.debug("Failed to read the declared modules of '{}'", descriptorFile, e);
            return new DeclaredModules(List.of(), false);
        }
    }

    private Optional<ResolvedDescriptor> resolveRepositoryProjectDescriptor(RulesProject project) {
        try {
            var repositoryDescriptor = readRepositoryProjectDescriptor(project);
            var descriptor = repositoryDescriptor.orElseGet(() -> createSimpleRepositoryDescriptor(project));
            var declared = repositoryDescriptor.isPresent()
                    ? declaredOrDefaultModules(descriptor.getModules())
                    : new DeclaredModules(List.of(), false);
            var files = listRepositoryProjectFiles(project);
            descriptor.setModules(expandRepositoryModules(descriptor,
                    project.getFolderPath(),
                    declared.modules(),
                    files,
                    repositoryDescriptor.isPresent()));
            descriptor.getModules().sort(MODULES_COMPARATOR);
            return Optional.of(new ResolvedDescriptor(descriptor, declared.modules(), declared.defaulted()));
        } catch (IOException e) {
            log.warn("Failed to resolve repository project descriptor for '{}'", project.getName(), e);
        }
        return Optional.empty();
    }

    /**
     * The modules a project starts from. A project that declares none of its own takes the modules every
     * project has by default; those defaults are not in the file, and are flagged as such.
     */
    private static DeclaredModules declaredOrDefaultModules(List<Module> declared) {
        return declared.isEmpty() ? new DeclaredModules(ProjectDescriptor.defaultModules(), true)
                : new DeclaredModules(List.copyOf(declared), false);
    }

    private Optional<ProjectDescriptor> readRepositoryProjectDescriptor(RulesProject project) throws IOException {
        try (var content = readProjectDescriptorFile(project)) {
            if (content == null) {
                return Optional.empty();
            }
            var descriptor = ProjectDescriptor.read(content);
            if (descriptor != null && StringUtils.isBlank(descriptor.getName())) {
                descriptor.setName(project.getBusinessName());
            }
            return Optional.ofNullable(descriptor);
        }
    }

    /**
     * The {@code rules.xml} of the project, or {@code null} when it has none.
     *
     * <p>A repository without folder support keeps a project as a single archive and answers no request
     * for a file inside it, so the descriptor is taken from the project artefacts, which unpack it.
     */
    private static @Nullable InputStream readProjectDescriptorFile(RulesProject project) throws IOException {
        if (!project.getRepository().supports().folders()) {
            return readArtefactContent(project, ProjectDescriptor.FILE_NAME);
        }
        var descriptorPath = projectRepositoryPath(project.getFolderPath(), ProjectDescriptor.FILE_NAME);
        var repository = project.getRepository();
        var fileData = project.isHistoric()
                ? repository.checkHistory(descriptorPath, project.getHistoryVersion())
                : repository.check(descriptorPath);
        if (fileData == null) {
            return null;
        }
        var item = project.isHistoric()
                ? repository.readHistory(descriptorPath, project.getHistoryVersion())
                : repository.read(descriptorPath);
        return item == null ? null : item.getStream();
    }

    /** The content of a file of the project, or {@code null} when the project holds no such file. */
    private static @Nullable InputStream readArtefactContent(RulesProject project, String name) throws IOException {
        if (!project.hasArtefact(name)) {
            return null;
        }
        try {
            return project.getArtefact(name) instanceof AProjectResource file ? file.getContent() : null;
        } catch (ProjectException e) {
            throw new IOException(e);
        }
    }

    private static ProjectDescriptor createSimpleRepositoryDescriptor(RulesProject project) {
        var descriptor = new ProjectDescriptor();
        descriptor.setName(project.getBusinessName());
        return descriptor;
    }

    /**
     * The files the project is made of, as the repository stores them.
     *
     * <p>A repository without folder support keeps a project as a single archive and answers no per-file
     * listing at all, so its files are read through the project artefacts, which unpack it.
     */
    private static List<FileData> listRepositoryProjectFiles(RulesProject project) throws IOException {
        var repository = project.getRepository();
        if (!repository.supports().folders()) {
            return listArtefactFiles(project);
        }
        var path = projectRepositoryPath(project.getFolderPath(), "");
        return project.isHistoric()
                ? repository.listFiles(path, project.getHistoryVersion())
                : repository.list(path);
    }

    /** Every file of the folder and of the folders inside it, at the revision the project is opened on. */
    private static List<FileData> listArtefactFiles(AProjectFolder folder) {
        var files = new ArrayList<FileData>();
        for (var artefact : folder.getArtefacts()) {
            if (artefact instanceof AProjectFolder nested) {
                files.addAll(listArtefactFiles(nested));
            } else {
                files.add(artefact.getFileData());
            }
        }
        return files;
    }

    private static List<Module> expandRepositoryModules(ProjectDescriptor descriptor,
                                                        String projectPath,
                                                        List<Module> readModules,
                                                        List<FileData> files,
                                                        boolean hasProjectDescriptor) {
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

    /**
     * Tells whether the repository descriptor could still add anything to a locally resolved one.
     *
     * <p>Checks the very fields {@link #fillMissingDescriptorData} copies over: the comment, the modules,
     * the properties-file-name patterns, and the exposed methods. When the local workspace supplies all of
     * them, reading the repository is pure overhead and is skipped.
     */
    // package-private static for testing
    static boolean hasMissingData(ProjectDescriptor descriptor) {
        return commentMissing(descriptor)
                || modulesMissing(descriptor)
                || propertiesPatternsMissing(descriptor)
                || exposedMethodsMissing(descriptor);
    }

    private static boolean commentMissing(ProjectDescriptor d) {
        return d.getComment() == null;
    }

    private static boolean modulesMissing(ProjectDescriptor d) {
        return d.getModules().isEmpty();
    }

    private static boolean propertiesPatternsMissing(ProjectDescriptor d) {
        return d.getPropertiesFileNamePatterns() == null || d.getPropertiesFileNamePatterns().length == 0;
    }

    private static boolean exposedMethodsMissing(ProjectDescriptor d) {
        return d.getExposedMethods() == null;
    }

    private ResolvedDescriptor fillMissingDescriptorData(ResolvedDescriptor target, ResolvedDescriptor fallback) {
        var descriptor = target.descriptor();
        var source = fallback.descriptor();
        if (commentMissing(descriptor)) {
            descriptor.setComment(source.getComment());
        }
        var declaredModules = target.declaredModules();
        var modulesDefaulted = target.modulesDefaulted();
        if (modulesMissing(descriptor)) {
            descriptor.getModules().addAll(source.getModules());
            declaredModules = fallback.declaredModules();
            modulesDefaulted = fallback.modulesDefaulted();
        }
        if (propertiesPatternsMissing(descriptor)) {
            descriptor.setPropertiesFileNamePatterns(source.getPropertiesFileNamePatterns());
        }
        if (exposedMethodsMissing(descriptor)) {
            descriptor.setExposedMethods(source.getExposedMethods());
        }
        return new ResolvedDescriptor(descriptor, declaredModules, modulesDefaulted);
    }

    /**
     * The parts of the descriptor the UI cannot work out from {@code rules.xml} itself: the modules a
     * wildcard resolves to, and the source path entries, each flagged when it is the engine's default.
     * Everything else in the descriptor the UI reads from the file directly.
     */
    private DescriptorViewModel mapDescriptor(ResolvedDescriptor resolved) {
        var descriptor = resolved.descriptor();
        var classpath = descriptor.getClasspath();
        var sourcesDefault = classpath == null || classpath.isEmpty();
        return DescriptorViewModel.builder()
                .modules(ProjectModules.map(resolved.declaredModules(), descriptor.getModules()))
                .modulesDefault(resolved.modulesDefaulted())
                .sources(sourcesDefault ? ProjectDescriptor.defaultClasspath() : List.copyOf(classpath))
                .sourcesDefault(sourcesDefault)
                .build();
    }

    @Override
    protected ProjectViewModel.Builder mapProjectResponse(RulesProject src,
                                                          ProjectCriteriaQuery query,
                                                          Map<AProject, ProjectStatus> statuses) {
        return mapWorkspaceProjectResponse(src, statuses, query.includeDescriptor());
    }

    private ProjectViewModel.Builder mapWorkspaceProjectResponse(RulesProject src,
                                                                 Map<AProject, ProjectStatus> statuses,
                                                                 boolean includeDescriptor) {
        var builder = super.mapProjectResponse(src, statuses);
        builder.branchProtected(src.isBranchProtected());
        builder.branchDefault(src.isBranchDefault());
        builder.repositoryInfo(mapRepositoryInfo(src));
        projectDependencyResolver.getDependencies(src).stream()
                .sorted(DEPENDENCY_NAME_ORDER)
                .map(this::mapProjectDependency)
                .map(ProjectDependencyViewModel.Builder::build)
                .forEach(builder::addDependency);
        if (includeDescriptor) {
            resolveProjectDescriptor(src).ifPresent(descriptor -> builder.descriptor(mapDescriptor(descriptor)));
        }
        return builder;
    }

    /**
     * Maps a declared dependency. A dependency the workspace has no project for carries its name and the
     * mark that it is missing, so the screen can show what {@code rules.xml} asks for and cannot find.
     */
    private ProjectDependencyViewModel.Builder mapProjectDependency(ProjectDependency dependency) {
        var project = dependency.project();
        var builder = project == null
                ? ProjectDependencyViewModel.builder().name(dependency.name()).missing(true)
                : mapProjectDependency(project).name(dependency.name());
        return builder.transitive(dependency.transitive());
    }

    protected ProjectDependencyViewModel.Builder mapProjectDependency(RulesProject src) {
        var repository = src.getRepository();
        var builder = ProjectDependencyViewModel.builder().name(src.getBusinessName())
                .id(projectIdentifierMapper.map(src))
                .repository(repository.getId());
        builder.status(src.getStatus()).branch(src.getBranch());
        builder.branchProtected(src.isBranchProtected()).branchDefault(src.isBranchDefault());
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
            // FIXME: the no-arg getProjects() force-refreshes the whole user workspace (rebuilds all ~160 RulesProject
            //  instances on every request — the dominant cost of the list and its facet-count scan). getProjects(false)
            //  is far cheaper but serves stale cross-session state (deleted repos still listed, fresh ACL grants stay
            //  invisible), so it is reverted until workspace-cache invalidation is fixed.
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
    protected Map<String, String> tagsOf(RulesProject project) {
        // The tags.properties file is the source of truth: whatever it names is what the project carries,
        // whether or not an administrator configured the type. The catalog only offers suggestions.
        return projectTagsCache.getTags(project);
    }

    @Override
    protected List<ProjectStatusViewModel> projectStatuses(List<? extends AProject> pageProjects) {
        // The list renders only the compile state and message counts, so build a summary that skips the
        // per-message table/module resolution — the dominant cost on large projects.
        return pageProjects.stream()
                .map(RulesProject.class::cast)
                .map(projectStatusMapper::mapSummary)
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

    @Override
    protected Stream<String> branchNamesOf(AProject project) {
        return project instanceof RulesProject rulesProject && rulesProject.isSupportsBranches()
                ? projectBranches(rulesProject).stream()
                : super.branchNamesOf(project);
    }

    @Override
    protected Map<String, IndexHealth> projectIndexHealth() {
        var workspace = getUserWorkspace();
        if (workspace == null) {
            return Map.of();
        }
        var designRepository = workspace.getDesignTimeRepository();
        return designRepository.getRepositories()
                .stream()
                .flatMap(repository -> designRepository.getProjectIndexHealth(repository.getId())
                        .stream()
                        .filter(health -> health.state() != IndexState.READY)
                        .map(health -> Map.entry(repository.getId(), health)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));
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
        if (!projectStateValidator.canSave(project)) {
            throw new ConflictException("project.save.conflict.message");
        }
        requireGranted(project, BasePermission.WRITE);
        var comment = resolveSaveComment(project, StringUtils.trimToNull(model.comment()));
        validateComment(project, comment);
        project.getFileData().setComment(comment);
        commit(project);
        publishStateChanged(project);
    }

    /**
     * Tells the WebSocket layer that a project of the current user's workspace changed state, so the
     * user's open screens can re-read what they show. Fired only after the action succeeded, and never
     * fails the action itself: a notification is advisory.
     */
    private void publishStateChanged(RulesProject project) {
        try {
            var workspace = getUserWorkspace();
            if (workspace == null) {
                return;
            }
            eventPublisher.publishEvent(new ProjectStateChangedEvent(project, workspace.getUser().getUserName()));
        } catch (RuntimeException e) {
            log.warn("Failed to publish a state change of project '{}'.", project.getBusinessName(), e);
        }
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
            getUserWorkspace().setProjectBranch(project, branch);
        }
        publishStateChanged(project);
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
        var localOnly = project.isLocalOnly();
        var repository = project.getDesignRepository();
        var projectName = project.getDesignProjectName();
        var branch = project.getBranch();
        var aclPath = localOnly ? null : designRepositoryAclService.getPath(project);

        getWebStudio().getModel().clearModuleInfo();
        closeProjectForAllUsers(project);
        try {
            deleteAuthorizedProject(project, normalizedComment);
        } catch (ProjectException e) {
            log.warn("Failed to delete project '{}'", project.getBusinessName(), e);
            throw new ConflictException("project.delete.message");
        }
        var indexPublished = localOnly || refreshProjectIndex(repository, branch);
        if (!localOnly && indexPublished && !isAclPathStillUsed(repository, projectName, aclPath)) {
            designRepositoryAclService.deleteAcl(repository.getId(), aclPath);
        }
        workspaceManager.refreshWorkspaces();
        getWebStudio().reset();
        publishStateChanged(project);
        if (!indexPublished) {
            throw new ConflictException("project.delete.indexing.incomplete.message");
        }
    }

    /**
     * Deletes a project after {@link #delete(RulesProject, String)} has verified its project-level permission.
     *
     * <p>Readable cross-branch snapshots use ACL-decorated repositories. Their generic delete check targets the
     * parent repository path, which is correct for an arbitrary child but would replace the project-level permission
     * already verified by this service. The original repository keeps the same branch and performs the authorized
     * project write.
     */
    private void deleteAuthorizedProject(RulesProject project, @Nullable String comment) throws ProjectException {
        var repository = project.getRepository();
        var originalRepository = unwrapRepository(repository);
        if (originalRepository == repository) {
            project.delete(comment);
            return;
        }

        var designProject = new AProject(originalRepository, project.getFileData());
        designProject.delete(getUserWorkspace().getUser(), comment);
    }

    private boolean refreshProjectIndex(Repository repository, @Nullable String branch) {
        var designTimeRepository = getUserWorkspace().getDesignTimeRepository();
        if (!(repository instanceof BranchRepository) || !repository.supports().branches()) {
            designTimeRepository.refresh();
            return true;
        }
        try {
            designTimeRepository.refreshBranch(repository.getId(), Objects.requireNonNull(branch))
                    .toCompletableFuture()
                    .get(PROJECT_INDEX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Project index did not publish branch '{}' in repository '{}'.",
                    branch, repository.getId(), e);
        }
        return false;
    }

    private boolean refreshRepositoryIndex(Repository repository) {
        try {
            getUserWorkspace().getDesignTimeRepository()
                    .refreshRepository(repository.getId())
                    .toCompletableFuture()
                    .get(PROJECT_INDEX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Project index did not publish repository '{}'.", repository.getId(), e);
        }
        return false;
    }

    private boolean isAclPathStillUsed(Repository repository, String projectName, String aclPath) {
        if (!repository.supports().branches()) {
            return false;
        }
        return getUserWorkspace().getDesignTimeRepository()
                .getBranchedProject(repository.getId(), projectName)
                .stream()
                .flatMap(project -> project.entries().values().stream())
                .map(entry -> designRepositoryAclService.getPath(entry.project()))
                .anyMatch(path -> path != null && (path.equals(aclPath) || path.startsWith(aclPath + '/')));
    }

    /**
     * Closes a project in every user workspace before it is removed from the design repository.
     *
     * <p>A workspace holds the project in a folder named after it. That name is usually the same everywhere, but a
     * rename in {@code rules.xml} that is not saved yet moves only the folder of the user who made it. Both names
     * are therefore closed, so no user is left with a copy of a project that no longer exists.
     */
    private void closeProjectForAllUsers(RulesProject project) {
        var businessName = project.getBusinessName();
        var mainBusinessName = project.getMainBusinessName();
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
            if (!Objects.equals(mainBusinessName, businessName)) {
                closeProjectInWorkspace(file, repoId, mainBusinessName, branch);
            }
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
        publishStateChanged(project);
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
        var workspace = getUserWorkspace();
        workspace.setProjectBranch(project, branchName);
        if (project.getLastHistoryVersion() == null) {
            workspace.setProjectBranch(project, previousBranch);
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
                if (workspace.isOpenedOtherProject(project)) {
                    throw new ConflictException("open.duplicated.project");
                } else {
                    project.open();
                }
            }
        }
        publishStateChanged(project);
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
        var securedRepository = (BranchRepository) project.getDesignRepository();
        var repository = unwrapBranchRepository(securedRepository);
        var validator = newBranchValidatorFactory.apply(repository);
        validationProvider.validate(model.getBranch(), validator);
        try {
            var startPoint = StringUtils.isNotBlank(model.getRevision()) ? model.getRevision() : project.getBranch();
            securedRepository.createRepositoryBranch(model.getBranch(), startPoint);
        } catch (AccessDeniedException e) {
            throw new ForbiddenException("default.message");
        } catch (IOException e) {
            throw new ProjectException("Failed to create branch", e);
        }
        if (!refreshProjectIndex(repository, model.getBranch())) {
            throw new ConflictException("project.indexing.incomplete.message");
        }
        publishStateChanged(project);
    }

    /**
     * The design repository of a project, read from the project itself.
     *
     * <p>A project that exists only in the local workspace has no design repository, and nothing is
     * reported for it.
     *
     * <p>The configured type comes from the repository settings, which are parsed once per repository per
     * request: a page of projects repeats the same few repositories.
     */
    @Nullable
    private ProjectRepositoryModel mapRepositoryInfo(RulesProject project) {
        if (project.isLocalOnly()) {
            return null;
        }
        var repository = project.getDesignRepository();
        var repositoryId = repository.getId();
        return new ProjectRepositoryModel(repositoryId,
                repository.getName(),
                listingContext.repositoryType(repositoryId,
                        id -> new RepositoryConfiguration(id, environment).getType()),
                new RepositoryFeatures(repository.supports()));
    }

    /**
     * Lists the branches of a project, in the given {@link BranchScope}, each marked as protected and as the
     * repository base branch.
     */
    public List<ProjectBranchInfo> getBranches(RulesProject project, BranchScope scope) {
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        requireGranted(project, BasePermission.READ);
        var repository = unwrapBranchRepository(project.getDesignRepository());
        var baseBranch = repository.getBaseBranch();
        return branchNames(repository, project, scope).stream()
                .map(branch -> ProjectBranchInfo.builder()
                        .name(branch)
                        .protectedFlag(repository.isBranchProtected(branch))
                        .base(branch.equals(baseBranch))
                        .build())
                .toList();
    }

    /**
     * Refuses the branch deletion unless the caller may delete every project that no other branch holds.
     *
     * <p>Deleting a branch removes those projects for good, which is what deleting a project is for and what the
     * delete permission guards. The branch is deleted repository-wide, so the check covers every project it holds
     * alone, not only the one the request addressed.
     *
     * <p>The holders come from the project index. An index that has no entry for the addressed project — a
     * repository still being indexed, or a branch it failed to read — answers "not known", not "no holders", so
     * the addressed project counts as held alone and the deletion still asks for the delete permission.
     */
    private void requireDeletionOfProjectsHeldOnlyBy(RulesProject project, String repositoryId, String branch) {
        var designTimeRepository = getUserWorkspace().getDesignTimeRepository();
        if (designTimeRepository.isLastProjectBranch(repositoryId, project.getDesignProjectName(), branch)) {
            requireDeletePermission(project);
        }
        for (AProject held : designTimeRepository.getProjectsHeldOnlyBy(repositoryId, branch)) {
            requireDeletePermission(held);
        }
    }

    private void requireDeletePermission(AProject project) {
        if (!aclProjectsHelper.hasPermission(project, BasePermission.DELETE)) {
            throw new ForbiddenException("project.branch.delete.last.message");
        }
    }

    private List<String> branchNames(BranchRepository repository, RulesProject project, BranchScope scope) {
        var projectBranches = projectBranches(project);
        return switch (scope) {
            case PROJECT -> projectBranches;
            case REPOSITORY -> repositoryBranches(repository, projectBranches);
        };
    }

    /**
     * Every branch of the repository, ordered case-insensitively.
     *
     * <p>A repository that cannot be read answers with the branches holding the project, so a project never
     * loses the branch it is on.
     */
    private List<String> repositoryBranches(BranchRepository repository, List<String> projectBranches) {
        try {
            return List.copyOf(Stream.concat(repository.listBranches().stream(), projectBranches.stream())
                    .collect(StreamUtils.toTreeSet(String.CASE_INSENSITIVE_ORDER)));
        } catch (IOException e) {
            log.warn("Cannot list the branches of repository '{}'.", repository.getId(), e);
            return projectBranches;
        }
    }

    private List<String> projectBranches(RulesProject project) {
        var workspace = getUserWorkspace();
        if (workspace != null) {
            var entries = workspace.getDesignTimeRepository()
                    .getBranchedProject(project.getDesignRepository().getId(),
                            project.getDesignProjectName())
                    .map(branchedProject -> branchedProject.entries().keySet())
                    .orElse(Set.of());
            if (!entries.isEmpty()) {
                return entries.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            }
        }
        return StringUtils.isBlank(project.getBranch()) ? List.of() : List.of(project.getBranch());
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
        var securedRepository = (BranchRepository) project.getDesignRepository();
        var repository = unwrapBranchRepository(securedRepository);
        var repositoryId = repository.getId();
        var projectName = project.getBusinessName();
        try {
            var branch = repository.listBranches()
                    .stream()
                    .filter(candidate -> candidate.equalsIgnoreCase(branchName))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("repository.branch.message"));
            if (repository.getBaseBranch().equalsIgnoreCase(branch)) {
                throw new ConflictException("project.branch.delete.base.message");
            }
            requireDeletionOfProjectsHeldOnlyBy(project, repositoryId, branch);
            requireNotLockedByAnotherUser(project, repository, branch);
            bypassService.requireBypassOrThrow(repository, branch, project, force);
            var restoreOpenedState = releaseProjectOnBranch(project, branch);
            securedRepository.deleteRepositoryBranch(branch);
            if (!refreshRepositoryIndex(repository)) {
                throw new ProjectException("The deleted branch was not published by the project index.");
            }
            var workspace = getUserWorkspace();
            workspace.refresh();
            if (restoreOpenedState) {
                findWorkspaceProject(workspace, repositoryId, projectName).ifPresent(remainingProject -> {
                    try {
                        open(remainingProject, false);
                    } catch (ProjectException e) {
                        throw new ConflictException("project.branch.delete.failed.message");
                    }
                });
            }
        } catch (AccessDeniedException e) {
            throw new ForbiddenException("default.message");
        } catch (IOException | ProjectException e) {
            log.warn("Failed to delete branch '{}' from project '{}'", branchName, project.getBusinessName(), e);
            throw new ConflictException("project.branch.delete.failed.message");
        }
        publishStateChanged(project);
    }

    private static BranchRepository unwrapBranchRepository(Repository repository) {
        return (BranchRepository) unwrapRepository(repository);
    }

    private static Repository unwrapRepository(Repository repository) {
        while (repository instanceof RepositoryDelegate delegate) {
            repository = delegate.getOriginal();
        }
        return repository;
    }

    private Optional<RulesProject> findWorkspaceProject(UserWorkspace workspace,
                                                        String repositoryId,
                                                        String projectName) {
        return workspace.getProjectsByName(projectName, false)
                .stream()
                .filter(project -> repositoryId.equals(project.getDesignRepository().getId()))
                .findFirst();
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
        // A project declaring no module has no table to list and nothing to open. It is a project the table creator
        // is expected to work on — it is where the first module comes from — so it answers empty rather than 404.
        var projectDescriptor = getProjectDescriptor(project);
        var modules = projectDescriptor.getModules();
        if (modules.isEmpty()) {
            return PageResponse.of(List.of(), page, 0L);
        }
        var moduleModel = openProject(projectDescriptor, project, modules.getFirst()).awaitCompiled();

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

    /**
     * Finds a table created by the Tables API, including custom OpenL table types that are hidden from the regular
     * tables list.
     */
    public @Nullable SummaryTableView getCreatedTable(RulesProject project,
                                                      String moduleName,
                                                      @Nullable String tableId,
                                                      String tableName) {
        var moduleModel = openProject(project, moduleName).awaitCompiled();
        if (tableId != null) {
            var createdTable = moduleModel.getTableById(tableId);
            return createdTable == null ? null : summaryTableReader.read(createdTable);
        }
        // A free-form table is shown under a shortened header, so the requested name is matched in the same form the
        // search and the reader produce; for every other kind the name is returned unchanged.
        var displayName = TableSyntaxNodeUtils.str2name(tableName, XlsNodeTypes.XLS_OTHER);
        // The search narrows by substring, and shortening appends a mark the full name does not carry. Only the part
        // both forms share can be searched for, or a name longer than the shortening allows matches neither and a
        // table just written is reported as not created. The exact match below is what tells the two apart.
        var searchedName = displayName.endsWith(SHORTENED_NAME_MARK)
                ? displayName.substring(0, displayName.length() - SHORTENED_NAME_MARK.length())
                : displayName;
        var query = ProjectTableCriteriaQuery.builder().name(searchedName).includeOther(true).build();
        return moduleModel.search(buildTableSelector(query), SearchScope.CURRENT_MODULE)
                .stream()
                .map(summaryTableReader::read)
                .filter(table -> table.name.equalsIgnoreCase(tableName) || table.name.equalsIgnoreCase(displayName))
                .findFirst()
                .orElse(null);
    }

    private Predicate<TableSyntaxNode> buildTableSelector(ProjectTableCriteriaQuery query) {
        Predicate<TableSyntaxNode> selectors = tsn -> query.isIncludeOther()
                || !XlsNodeTypes.XLS_OTHER.toString().equals(tsn.getType());

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

    /**
     * Get a table's name, kind and its own properties, without reading the body.
     * <p>
     * The copy dialog prefills the copy's name and properties from this, so a table of any size is read cheaply.
     *
     * @param project project
     * @param tableId table id
     * @return the table's name, kind and defined properties
     */
    public TablePropertiesView getTableProperties(RulesProject project, String tableId) {
        var context = getOpenLTable(project, tableId);
        var table = context.table();
        var summary = summaryTableReader.read(table);
        return new TablePropertiesView(summary.name, summary.kind, tablePropertiesService.read(table),
                tableVersionService.describe(table, context.module().getTableSyntaxNodes()));
    }

    /**
     * Get the modules the project declares.
     *
     * <p>One entry per module, patterns already resolved to the files they matched.
     *
     * @param project project
     * @return declared modules
     */
    public List<ModuleViewModel> getModules(RulesProject project) {
        return getProjectDescriptor(project).getModules()
                .stream()
                .filter(module -> module.getName() != null)
                .map(module -> ModuleViewModel.module(module.getName(), module.getRulesRootPath()))
                .toList();
    }

    /**
     * Get the worksheets of one module.
     *
     * @param project    project
     * @param moduleName module name
     * @return worksheets of the module's workbook
     */
    public List<String> getModuleSheets(RulesProject project, String moduleName) {
        var module = getProjectDescriptor(project).getModules()
                .stream()
                .filter(declared -> moduleName.equals(declared.getName()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("project.module.identifier.message"));
        return metadataService.getSheets(project, module.getRulesRootPath());
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

    /**
     * Creates a table and returns its exact identifier when the destination module already exists.
     *
     * <p>A new module has no compiled table identifier until the project is reopened. Its table is found after the
     * write by name instead.
     *
     * @return created table identifier, or {@code null} for a newly created module
     */
    public @Nullable String createNewTable(RulesProject project,
                                           CreateNewTableRequest createTableRequest) throws ProjectException {
        requireGranted(project, BasePermission.WRITE);
        if (StringUtils.isNotBlank(createTableRequest.modulePath())) {
            createTableInNewModule(project, createTableRequest);
            return null;
        }
        var projectModel = openProject(project, createTableRequest.moduleName()).awaitCompiled();
        getWebStudio().getCurrentProject().tryLockOrThrow();
        return tableCreatorService.createTable(createTableRequest, projectModel);
    }

    /**
     * Copy a table into a module of the currently opened project, preserving its formatting.
     * <p>
     * The table stays on the server, resolved by its id. It is rebuilt on the destination sheet the way the table
     * editor writes a new table: the body is copied with its styles, merged cells and comments, the header is renamed
     * after the request, and the properties are the requested ones or — when none are given — the source's own. The
     * copy is laid out in one pass, so a copy that keeps the source's name never exists as an indistinguishable
     * duplicate.
     *
     * <p>The returned identifier is the copy's own, even when it shares the source's name — a copy kept under the same
     * name is a new version told apart by its properties, and its identifier reflects its position, not its name.
     *
     * @param project       project
     * @param sourceTableId id of the table to copy
     * @param request       the copy request
     * @return the copy's identifier, or {@code null} for a newly created module
     * @throws ProjectException if project is locked by another user
     */
    public @Nullable String copyTable(RulesProject project,
                                      String sourceTableId,
                                      CopyTableRequest request) throws ProjectException {
        requireGranted(project, BasePermission.WRITE);
        // Resolve the source (with its live grid) before opening the destination module. The resolved POI grid stays
        // valid across the reopen — the copy only reads it — so a copy into another module still sees the source cells.
        var source = getOpenLTable(project, sourceTableId).table();
        var sheetName = Optional.ofNullable(request.sheetName()).filter(StringUtils::isNotBlank).orElseGet(request::name);
        if (StringUtils.isNotBlank(request.modulePath())) {
            return copyIntoNewModule(project, source, request, sheetName);
        }
        var projectModel = openProject(project, request.moduleName()).awaitCompiled();
        getWebStudio().getCurrentProject().tryLockOrThrow();
        return writeCopy(projectModel, source, request, sheetName);
    }

    /** Rebuild the copy on {@code sheetName} of the already-compiled destination module and persist it. */
    private String writeCopy(ProjectModel projectModel, IOpenLTable source, CopyTableRequest request,
                             String sheetName) {
        var destGrid = tableCreatorService.sheetGridModel(projectModel, sheetName);
        var copyId = tableCopyService.copyInto(source, request.name(), request.properties(), destGrid,
                projectModel.getTableSyntaxNodes());
        tableCreatorService.save(destGrid);
        return copyId;
    }

    /**
     * Copy a table into a module that does not exist yet.
     * <p>
     * The module is created empty and registered, then the copy is written into it. The copy's identifier is unknown
     * until the project is recompiled, so it is found after the write by name.
     *
     * @return {@code null}, so the caller resolves the copy by name
     */
    private @Nullable String copyIntoNewModule(RulesProject project,
                                               IOpenLTable source,
                                               CopyTableRequest request,
                                               String sheetName) throws ProjectException {
        var lockedBefore = project.isLockedByMe();
        project.tryLockOrThrow();
        boolean moduleCreated = false;
        try {
            var projectDescriptor = getProjectDescriptor(project);
            requireModuleAbsent(projectDescriptor.getModules(), request.moduleName(), request.modulePath());
            tableCreatorService.createEmptyModule(project, projectDescriptor, request.moduleName(),
                    request.modulePath(), sheetName);
            moduleCreated = true;
            // Recompile so the empty module carries a sheet to write into, then rebuild the copy there.
            getWebStudio().reset();
            var projectModel = openProject(project, request.moduleName()).awaitCompiled();
            writeCopy(projectModel, source, request, sheetName);
            return null;
        } catch (RuntimeException | ProjectException e) {
            // The write can fail after the empty module is registered — unlike the atomic create path. Remove the
            // module so no phantom lingers to block a retry, and release the lock this request took.
            if (moduleCreated) {
                tableCreatorService.deleteModule(project, request.moduleName(), request.modulePath());
            }
            releaseLockTaken(project, lockedBefore);
            throw e;
        }
    }

    private void createTableInNewModule(RulesProject project,
                                        CreateNewTableRequest createTableRequest) throws ProjectException {
        if (!(createTableRequest.table() instanceof RawTableView rawTable)) {
            throw new BadRequestException("table.new-module.raw-source.message");
        }
        // A project without modules never opens, so the session has no current project to lock; the project the
        // module is written to is locked instead.
        var lockedBefore = project.isLockedByMe();
        project.tryLockOrThrow();
        try {
            var projectDescriptor = getProjectDescriptor(project);
            requireModuleAbsent(projectDescriptor.getModules(), createTableRequest.moduleName(),
                    createTableRequest.modulePath());
            var newTableName = rawTable.name;
            // Required whether or not the project has a module to compile first: without a name the table is written
            // and then cannot be found again, which answers a successful create with an empty body.
            tableCreatorService.requireTableName(newTableName);
            tableCreatorService.createModuleWithTable(project, projectDescriptor, createTableRequest, rawTable);
        } catch (RuntimeException e) {
            // The checks answer ordinary input and the layout is refused the same way, so a rejected request leaves
            // no lock behind: the project would otherwise stay reserved for a write that never happened, and
            // clearing a lock its owner never meant to take takes an administrator.
            releaseLockTaken(project, lockedBefore);
            throw e;
        }
    }

    /** Releases the lock this request took, leaving one the session already held alone. */
    private static void releaseLockTaken(RulesProject project, boolean lockedBefore) {
        if (!lockedBefore && project.isLockedByMe()) {
            project.unlock();
        }
    }

    /** Rejects a new module whose name or path already resolves in the project. */
    private static void requireModuleAbsent(List<Module> modules, String moduleName, @Nullable String modulePath) {
        if (modules.stream().map(Module::getName).filter(Objects::nonNull).anyMatch(moduleName::equalsIgnoreCase)) {
            throw new ConflictException("table.new-module.exists.message", moduleName);
        }
        if (declaresPath(modules, modulePath)) {
            throw new ConflictException("table.new-module.exists.message", modulePath);
        }
    }

    /**
     * Tells whether the project already resolves a module at this path.
     *
     * <p>A declaration in rules.xml is resolved ahead of the wildcards, so one left behind for a file that does not
     * exist yet would name the new module after itself. The table would be created under a name nobody asked for,
     * and the caller that created it could not find it again.
     */
    private static boolean declaresPath(List<Module> modules, @Nullable String modulePath) {
        if (modulePath == null) {
            return false;
        }
        var path = modulePath.replace('\\', '/');
        return modules.stream()
                .map(Module::getRulesRootPath)
                .filter(Objects::nonNull)
                .anyMatch(declared -> declared.replace('\\', '/').equalsIgnoreCase(path));
    }

    private record OpenLTableContext(
            @NotNull
            IOpenLTable table,
            @NotNull
            ProjectModel module
    ) {

        public Map<Severity, List<OpenLMessage>> getMessages() {
            var tableUri = table.getUri();
            var messages = Stream.of(Severity.values())
                    .flatMap(severity -> module.getMessagesByTsn(tableUri, severity).stream())
                    .toList();
            if (OpenLTableLogic.testedRulesHaveErrors(table, module, false)) {
                messages = new ArrayList<>(messages);
                messages.add(new OpenLMessage("Tested rules have errors", Severity.WARN));
            }
            return messages.stream().collect(Collectors.groupingBy(OpenLMessage::getSeverity));
        }

    }

}
