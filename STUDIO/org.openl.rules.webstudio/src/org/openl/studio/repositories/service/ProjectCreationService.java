package org.openl.studio.repositories.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.web.CopyProjectTransformer;
import org.openl.rules.webstudio.web.repository.project.CustomTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.PredefinedTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.project.TemplatesResolver;
import org.openl.rules.webstudio.web.repository.upload.ProjectUploader;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.repositories.model.ProjectTemplateGroup;
import org.openl.studio.tags.service.TagAssignmentValidator;
import org.openl.util.StringUtils;

/**
 * Creates projects the same way the legacy repository tab does, wrapping the reusable creation
 * primitives (template resolvers + {@link ExcelFilesProjectCreator}). The project is built in the
 * current user's workspace, granted a CONTRIBUTOR ACL, and the resulting file data is returned.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCreationService {

    private static final String CUSTOM_TYPE = "custom";
    private static final String PREDEFINED_TYPE = "predefined";
    private static final String ROLLBACK_UPLOAD_COMMENT = "Rollback project upload.";
    private static final long PROJECT_INDEX_TIMEOUT_SECONDS = 30;

    private final AclProjectsHelper aclProjectsHelper;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final TagAssignmentValidator tagAssignmentValidator;
    @Qualifier("zipFilter")
    private final PathFilter zipFilter;
    private final ZipCharsetDetector zipCharsetDetector;
    @Value("${openl.home:}")
    private final String openlHome;

    private final TemplatesResolver predefinedTemplatesResolver = new PredefinedTemplatesResolver();
    private TemplatesResolver customTemplatesResolver;

    @PostConstruct
    void init() {
        customTemplatesResolver = new CustomTemplatesResolver(openlHome);
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Lookup("commentService")
    protected Comments getCommentsService(String repoId) {
        return null;
    }

    private void requireCreatePermission(String repositoryId) {
        if (!aclProjectsHelper.hasCreateProjectPermission(repositoryId)) {
            throw new ForbiddenException("default.message");
        }
    }

    /**
     * Grants the current user the CONTRIBUTOR role on a freshly created project, unless an ACL already
     * exists. Shared with create paths that build the project outside this service.
     */
    public static void grantContributorAclIfAbsent(RepositoryAclService acl, AProject project) {
        if (!acl.hasAcl(project)) {
            acl.createAcl(project, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
        }
    }

    /**
     * Resyncs the user workspace with the design repository after a write.
     *
     * <p>The refresh keeps the project list metadata aligned for later open, branch and save operations.
     */
    public void refreshWorkspaceAfterDesignChange() {
        try {
            getUserWorkspace().refresh();
        } catch (RuntimeException e) {
            // The design write is already finalized and indexed. A stale user workspace must not report the
            // successful create or copy as failed.
            log.warn("The user workspace could not be refreshed after a design repository change.", e);
        }
    }

    /**
     * Waits until a branch-scoped design write is visible through the project index.
     *
     * <p>A normal response therefore guarantees that subsequent project reads can resolve the new content.
     */
    public void awaitProjectVisibility(Repository repository) {
        // The design-time repository is resolved only when there is something to wait for: a repository
        // without branches needs no wait, and looking one up requires a user workspace the caller may not have.
        awaitProjectVisibility(() -> getUserWorkspace().getDesignTimeRepository(), repository);
    }

    /**
     * Waits until a branch-scoped design write is visible through the project index, for a caller that already
     * holds the design-time repository.
     *
     * <p>A start-up task has no user workspace to look up, so it supplies the repository it works with.
     */
    public void awaitProjectVisibility(DesignTimeRepository designTimeRepository, Repository repository) {
        awaitProjectVisibility(() -> designTimeRepository, repository);
    }

    private void awaitProjectVisibility(Supplier<DesignTimeRepository> designTimeRepository, Repository repository) {
        if (!(repository instanceof BranchRepository branchRepository) || !repository.supports().branches()) {
            return;
        }
        try {
            designTimeRepository.get()
                    .refreshBranch(repository.getId(), branchRepository.getBranch())
                    .toCompletableFuture()
                    .get(PROJECT_INDEX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictException("project.indexing.incomplete.message");
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Project index did not publish branch '{}' in repository '{}'.",
                    branchRepository.getBranch(), repository.getId(), e);
            throw new ConflictException("project.indexing.incomplete.message");
        }
    }

    /**
     * Configures the tag values a project brought with it in its {@code tags.properties} file: a value of
     * an extensible tag type that is not configured yet becomes configured, so the project shows the tag
     * it carries. A value of any other tag type, and an unknown tag type, need an administrator and are
     * left as they are.
     *
     * <p>Archive upload keeps the project closed, so the tags are read from the repository artefact.
     */
    public void registerExtensibleTags(AProject project) {
        tagAssignmentValidator.applicable(new ProjectTags(project).getTags());
    }

    /**
     * Configures the tag values of a workspace project, collected from both its design and local copies so
     * every create path is covered: publishing a local project exposes them through the local copy, while
     * an archive or copy create exposes them through the design copy.
     */
    void registerExtensibleTags(RulesProject project) {
        tagAssignmentValidator.applicable(collectProjectTags(project));
    }

    /**
     * Configures the tags of a freshly written design project and resyncs the user workspace with the
     * design repository.
     */
    public void registerExtensibleTagsAfterDesignChange(AProject project) {
        registerExtensibleTags(project);
        refreshWorkspaceAfterDesignChange();
    }

    /**
     * Configures the tags of a workspace project and resyncs the user workspace after a design-repository
     * write.
     */
    public void registerExtensibleTagsAfterDesignChange(RulesProject project) {
        registerExtensibleTags(project);
        refreshWorkspaceAfterDesignChange();
    }

    private static Map<String, String> collectProjectTags(RulesProject project) {
        var tags = new HashMap<String, String>();
        try {
            tags.putAll(project.getDesignTags());
        } catch (RuntimeException e) {
            log.warn("Cannot read design tags of project '{}'", project.getName(), e);
        }
        try {
            tags.putAll(project.getLocalTags());
        } catch (RuntimeException e) {
            log.warn("Cannot read local tags of project '{}'", project.getName(), e);
        }
        return tags;
    }

    /** List the available predefined (bundled) and custom project templates, grouped by category. */
    public List<ProjectTemplateGroup> listTemplates() {
        var groups = new ArrayList<ProjectTemplateGroup>();
        addGroups(groups, PREDEFINED_TYPE, predefinedTemplatesResolver);
        addGroups(groups, CUSTOM_TYPE, customTemplatesResolver);
        return groups;
    }

    private static void addGroups(List<ProjectTemplateGroup> groups, String type, TemplatesResolver resolver) {
        if (resolver == null) {
            return;
        }
        for (String category : resolver.getCategories()) {
            var templates = resolver.getTemplates(category);
            if (!templates.isEmpty()) {
                groups.add(ProjectTemplateGroup.builder().type(type).category(category).templates(templates).build());
            }
        }
    }

    /**
     * Create a project from a bundled or custom template, granting the creator a CONTRIBUTOR ACL. The
     * template's Excel files become the project content.
     *
     * @return the created project's file data (branch/revision)
     */
    public FileData createFromTemplate(String repositoryId, String projectName, String path,
                                       String type, String category, String template,
                                       String comment, Map<String, String> tags) {
        requireCreatePermission(repositoryId);
        return createFromTemplate(getUserWorkspace().getDesignTimeRepository().getRepository(repositoryId),
                projectName, path, type, category, template, comment, tags);
    }

    public FileData createFromTemplate(Repository repository, String projectName, String path,
                                       String type, String category, String template,
                                       String comment, Map<String, String> tags) {
        var repositoryId = repository.getId();
        requireCreatePermission(repositoryId);
        var resolver = CUSTOM_TYPE.equals(type) ? customTemplatesResolver : predefinedTemplatesResolver;
        var files = resolver.getProjectFiles(category, template);
        if (files.length == 0) {
            throw new NotFoundException("project.template.not-found.message");
        }
        return createFromFiles(repository, projectName, path, new ArrayList<>(List.of(files)), comment,
                "rules/Models.xlsx", "rules/Algorithms.xlsx", "Models", "Algorithms", tags);
    }

    /**
     * Create a project from uploaded files, granting the creator a CONTRIBUTOR ACL. The upload dispatcher
     * recognises the content by extension: a single ZIP archive, one or more Excel files, or a single
     * OpenAPI (Swagger) file (which builds a data-types module and a rules module at the given paths).
     *
     * @return the created project's file data (branch/revision)
     */
    public FileData createFromFiles(String repositoryId, String projectName, String path, List<ProjectFile> files,
                                    String comment, String modelsPath, String algorithmsPath, String modelsModuleName,
                                    String algorithmsModuleName, Map<String, String> tags) {
        requireCreatePermission(repositoryId);
        return createFromFiles(getUserWorkspace().getDesignTimeRepository().getRepository(repositoryId),
                projectName, path, files, comment, modelsPath, algorithmsPath, modelsModuleName,
                algorithmsModuleName, tags);
    }

    public FileData createFromFiles(Repository repository, String projectName, String path, List<ProjectFile> files,
                                    String comment, String modelsPath, String algorithmsPath, String modelsModuleName,
                                    String algorithmsModuleName, Map<String, String> tags) {
        var repositoryId = repository.getId();
        requireCreatePermission(repositoryId);
        try {
            var created = new ProjectUploader(repository, files, projectName, StringUtils.trimToEmpty(path),
                    getUserWorkspace(), aclServiceProvider.getDesignRepoAclService(), comment, zipFilter,
                    zipCharsetDetector, modelsPath, algorithmsPath, modelsModuleName, algorithmsModuleName,
                    tags != null ? tags : Map.of(), this::registerExtensibleTags,
                    () -> awaitProjectVisibility(repository)).uploadProject();
            return created.getFileData();
        } catch (ProjectException e) {
            throw new ConflictException("project.create.failed.message");
        }
    }

    /**
     * Sets the status a freshly created project should have in the user's workspace: opened so the creator
     * can start working on it at once, or closed. A {@code null} status leaves whatever the create path
     * produced (an archive stays closed, other sources stay opened), so existing API callers are unaffected.
     *
     * <p>The wire codes {@code OPENED} and {@code CLOSED} arrive as {@link ProjectStatus#VIEWING} and
     * {@link ProjectStatus#CLOSED}; any other value is ignored.
     */
    public void applyStatusAfterCreate(String repositoryId, String projectName, @Nullable ProjectStatus status) {
        applyStatusAfterCreate(null, repositoryId, projectName, status);
    }

    /**
     * Sets the status of a freshly created project using the exact repository branch that received the write.
     */
    public void applyStatusAfterCreate(Repository repository,
                                       String projectName,
                                       @Nullable ProjectStatus status) {
        applyStatusAfterCreate(repository, repository.getId(), projectName, status);
    }

    private void applyStatusAfterCreate(@Nullable Repository repository,
                                        String repositoryId,
                                        String projectName,
                                        @Nullable ProjectStatus status) {
        if (status != ProjectStatus.VIEWING && status != ProjectStatus.CLOSED) {
            return;
        }
        var workspace = getUserWorkspace();
        // The project is already committed by the time this runs; setting its workspace status is a
        // convenience on top. A failure here (or a same-named project already open elsewhere, which blocks
        // opening) must not turn a successful create into an error — log it and leave the project created.
        try {
            var project = resolveCreatedProject(workspace, repository, repositoryId, projectName);
            if (status == ProjectStatus.VIEWING) {
                if (project.isOpened()) {
                    return;
                }
                if (workspace.isOpenedOtherProject(project)) {
                    log.info("Created project '{}' stays closed: a project with the same name is open elsewhere.",
                            projectName);
                    return;
                }
                project.open();
                workspace.refresh();
            } else if (project.isOpened()) {
                project.close();
                workspace.refresh();
            }
        } catch (ProjectException | RuntimeException e) {
            log.warn("Created project '{}' could not be set to status '{}'.", projectName, status, e);
        }
    }

    /**
     * The just-created project. The workspace copy is used when the create path already registered one
     * (Excel, OpenAPI, template). An uploaded archive is written straight to the design repository and the
     * workspace may not list it yet — then the project is assembled from its design state directly, the
     * same way the legacy project creator and the copy flow build theirs.
     */
    private RulesProject resolveCreatedProject(UserWorkspace workspace,
                                               @Nullable Repository repository,
                                               String repositoryId,
                                               String projectName)
            throws ProjectException {
        if (repository instanceof BranchRepository branchRepository && repository.supports().branches()) {
            var designTimeRepository = workspace.getDesignTimeRepository();
            var designProject = resolveBranchedDesignProject(designTimeRepository,
                    repositoryId,
                    projectName,
                    branchRepository.getBranch());
            return newWorkspaceProject(workspace, repositoryId, designProject);
        }
        try {
            return workspace.getProject(repositoryId, projectName);
        } catch (ProjectException e) {
            var designTimeRepository = workspace.getDesignTimeRepository();
            designTimeRepository.refresh();
            var designProject = designTimeRepository.getProject(repositoryId, projectName);
            return newWorkspaceProject(workspace, repositoryId, designProject);
        }
    }

    private AProject resolveBranchedDesignProject(DesignTimeRepository designTimeRepository,
                                                  String repositoryId,
                                                  String projectName,
                                                  String branch) throws ProjectException {
        try {
            return designTimeRepository.getProject(repositoryId, projectName, branch);
        } catch (ProjectException e) {
            var indexedProject = designTimeRepository.getProjects(repositoryId)
                    .stream()
                    .filter(project -> project.getBusinessName().equalsIgnoreCase(projectName))
                    .findFirst()
                    .orElseThrow(() -> e);
            return designTimeRepository.getProject(repositoryId, indexedProject.getName(), branch);
        }
    }

    /** The workspace view of a design project that has no local copy yet. A seam for tests. */
    protected RulesProject newWorkspaceProject(UserWorkspace workspace, String repositoryId, AProject designProject) {
        return new RulesProject(workspace.getUser(),
                workspace.getLocalWorkspace().getRepository(repositoryId),
                null,
                designProject.getRepository(),
                designProject.getFileData(),
                workspace.getProjectsLockEngine());
    }

    /**
     * Publish local workspace projects to a design repository, keeping each project's name and granting
     * the creator a CONTRIBUTOR ACL on every published project.
     */
    public void uploadLocalProjects(String repositoryId, List<String> names, String path, String comment) {
        requireCreatePermission(repositoryId);
        uploadLocalProjects(getUserWorkspace().getDesignTimeRepository().getRepository(repositoryId),
                names, path, comment);
    }

    public void uploadLocalProjects(Repository repository, List<String> names, String path, String comment) {
        var repositoryId = repository.getId();
        requireCreatePermission(repositoryId);
        var workspace = getUserWorkspace();
        var designRepoAclService = aclServiceProvider.getDesignRepoAclService();
        var comments = getCommentsService(repositoryId);
        var uploaded = new ArrayList<RulesProject>();
        try {
            for (String name : names) {
                // Generate a default commit message when the user gave none, so the publish commit is never
                // empty (matching the archive and copy create paths).
                var resolvedComment = StringUtils.isNotBlank(comment) ? comment : comments.createProject(name);
                var project = workspace.uploadLocalProject(repository, name, StringUtils.trimToEmpty(path), resolvedComment);
                uploaded.add(project);
                grantContributorAclIfAbsent(designRepoAclService, project);
                registerExtensibleTags(project);
            }
        } catch (ProjectException e) {
            rollbackUploadedProjects(workspace, designRepoAclService, uploaded);
            throw new ConflictException("project.workspace.upload.failed.message");
        } catch (RuntimeException e) {
            rollbackUploadedProjects(workspace, designRepoAclService, uploaded);
            throw e;
        }
        awaitProjectVisibility(repository);
        refreshWorkspaceAfterDesignChange();
    }

    private static void rollbackUploadedProjects(UserWorkspace workspace,
                                                 RepositoryAclService designRepoAclService,
                                                 List<RulesProject> uploaded) {
        for (var project : uploaded.reversed()) {
            rollbackUploadedProject(workspace, designRepoAclService, project);
        }
    }

    private static void rollbackUploadedProject(UserWorkspace workspace,
                                                RepositoryAclService designRepoAclService,
                                                RulesProject project) {
        var projectName = projectNameOf(project);
        try {
            project.delete(workspace.getUser(), ROLLBACK_UPLOAD_COMMENT);
        } catch (ProjectException | RuntimeException e) {
            log.warn("Cannot roll back published workspace project '{}'", projectName, e);
            return;
        }
        try {
            designRepoAclService.deleteAcl(project);
        } catch (RuntimeException e) {
            log.warn("Cannot delete ACL for rolled back workspace project '{}'", projectName, e);
        }
    }

    private static String projectNameOf(RulesProject project) {
        try {
            return project.getName();
        } catch (RuntimeException e) {
            return "<unknown>";
        }
    }

    /**
     * Copy an existing project into a design repository under a new name, entirely server-side (the
     * project's folder is copied in the repository, not downloaded and re-uploaded). The descriptor is
     * renamed, the creator is granted a CONTRIBUTOR ACL, and the workspace is refreshed so the copy is
     * indexed.
     *
     * <p>The source name is its business name. It must identify one readable project in the source repository.
     * This also resolves projects whose mapped repository identity contains a path-derived suffix.
     *
     * @return the created copy's file data (branch/revision)
     */
    public FileData copyProject(String targetRepositoryId, String newName, String path,
                                String sourceRepositoryId, String sourceProjectName, String comment,
                                String revision) {
        requireCreatePermission(targetRepositoryId);
        return copyProject(getUserWorkspace().getDesignTimeRepository().getRepository(targetRepositoryId),
                newName, path, sourceRepositoryId, sourceProjectName, comment, revision);
    }

    public FileData copyProject(Repository targetRepository, String newName, String path,
                                String sourceRepositoryId, String sourceProjectName, String comment,
                                String revision) {
        var targetRepositoryId = targetRepository.getId();
        requireCreatePermission(targetRepositoryId);
        var workspace = getUserWorkspace();
        var designRepoAclService = aclServiceProvider.getDesignRepoAclService();
        try {
            var source = resolveSourceProject(workspace, sourceRepositoryId, sourceProjectName);
            if (!designRepoAclService.isGranted(source, List.of(BasePermission.READ))) {
                throw new ForbiddenException("default.message");
            }
            // The state to copy is resolved first: a revision the source has none at fails before anything
            // is written to the target repository.
            var sourceCopy = sourceAtRevision(source, revision);
            var designTimeRepository = workspace.getDesignTimeRepository();
            var designPath = designTimeRepository.getRulesLocation() + newName;
            var designData = new FileData();
            designData.setName(designPath);
            designData.setComment(comment);
            if (targetRepository.supports().mappedFolders()) {
                designData.addAdditionalData(FileMappingData.forProject(designPath, path, newName));
            }
            var user = workspace.getUser();
            var targetProject = new AProject(targetRepository, designData);
            targetProject.setResourceTransformer(new CopyProjectTransformer(newName, Map.of()));
            targetProject.update(sourceCopy, user);
            targetProject.setResourceTransformer(null);
            var copied = new RulesProject(user, workspace.getLocalWorkspace().getRepository(targetRepositoryId),
                    null, targetRepository, targetProject.getFileData(), workspace.getProjectsLockEngine());
            grantContributorAclIfAbsent(designRepoAclService, copied);
            registerExtensibleTags(copied);
            awaitProjectVisibility(targetRepository);
            refreshWorkspaceAfterDesignChange();
            return copied.getFileData();
        } catch (ProjectException e) {
            throw new ConflictException("project.copy.failed.message");
        }
    }

    private RulesProject resolveSourceProject(UserWorkspace workspace,
                                              String sourceRepositoryId,
                                              String sourceProjectName) throws ProjectException {
        var matches = workspace.getProjectsByName(sourceProjectName)
                .stream()
                .filter(project -> project.getDesignRepository() != null
                        && sourceRepositoryId.equals(project.getDesignRepository().getId()))
                .toList();
        if (matches.size() != 1) {
            throw new ProjectException(
                    "Cannot uniquely resolve project ''{0}'' in repository ''{1}''.",
                    null,
                    sourceProjectName,
                    sourceRepositoryId);
        }
        return matches.getFirst();
    }

    /**
     * The state of the source project to copy: its latest one for a blank revision, otherwise the state it
     * had at that revision.
     *
     * <p>A revision the project has no state at is rejected, whatever the repository makes of the value.
     */
    private AProject sourceAtRevision(RulesProject source, @Nullable String revision) {
        var version = StringUtils.trimToNull(revision);
        var sourceCopy = new AProject(source.getRepository(), source.getFolderPath(), version);
        if (version == null) {
            return sourceCopy;
        }
        try {
            if (sourceCopy.getFileData() != null) {
                return sourceCopy;
            }
        } catch (RuntimeException e) {
            // A repository numbers its revisions its own way and may reject the value outright.
            log.debug("Revision '{}' cannot be read from the repository.", version, e);
        }
        throw new NotFoundException("project.revision.message", version);
    }

}
