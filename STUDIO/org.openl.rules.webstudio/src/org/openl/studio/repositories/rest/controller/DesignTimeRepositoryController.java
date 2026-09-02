package org.openl.studio.repositories.rest.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lock.Lock;
import org.openl.rules.lock.LockManager;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.utils.AclPathUtils;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.studio.projects.converter.ProjectIdentityConverter;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.model.CreateFromProjectModel;
import org.openl.studio.repositories.model.CreateFromWorkspaceModel;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.studio.repositories.model.ProjectRevision;
import org.openl.studio.repositories.model.ProjectTemplateGroup;
import org.openl.studio.repositories.model.RepositoryConfigModel;
import org.openl.studio.repositories.model.RepositoryViewModel;
import org.openl.studio.repositories.rest.resolver.DesignRepository;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;
import org.openl.studio.repositories.service.ProjectCreationService;
import org.openl.studio.repositories.service.ProjectCreationTargetResolver;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;
import org.openl.studio.repositories.service.ZipProjectSaveStrategy;
import org.openl.studio.repositories.validator.CreateUpdateProjectModelValidator;
import org.openl.studio.repositories.validator.ZipArchiveValidator;
import org.openl.studio.rest.resolver.PaginationDefault;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

@RestController
@RequestMapping(value = "/repos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Design Repository")
public class DesignTimeRepositoryController {

    private final BeanValidationProvider validationProvider;
    private final CreateUpdateProjectModelValidator createUpdateProjectModelValidator;
    private final ZipArchiveValidator zipArchiveValidator;
    private final ZipProjectSaveStrategy zipProjectSaveStrategy;
    private final LockManager lockManager;
    private final RepositoryAclService designRepositoryAclService;
    private final AclProjectsHelper aclProjectsHelper;
    private final DesignTimeRepositoryService designTimeRepositoryService;
    private final ProjectRevisionService projectRevisionService;
    private final ProtectedBranchBypassService bypassService;
    private final ProjectCreationService projectCreationService;
    private final ProjectCreationTargetResolver projectCreationTargetResolver;
    private final RepositoryConfigService repositoryConfigService;
    private final ProjectIdentityConverter projectIdentityConverter;

    @Autowired
    public DesignTimeRepositoryController(@Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
                                          BeanValidationProvider validationService,
                                          CreateUpdateProjectModelValidator createUpdateProjectModelValidator,
                                          ZipArchiveValidator zipArchiveValidator,
                                          ZipProjectSaveStrategy zipProjectSaveStrategy,
                                          @Value("${openl.home.shared}") String homeDirectory,
                                          AclProjectsHelper aclProjectsHelper,
                                          DesignTimeRepositoryService designTimeRepositoryService,
                                          ProjectRevisionService projectRevisionService,
                                          ProtectedBranchBypassService bypassService,
                                          ProjectCreationService projectCreationService,
                                          ProjectCreationTargetResolver projectCreationTargetResolver,
                                          RepositoryConfigService repositoryConfigService,
                                          ProjectIdentityConverter projectIdentityConverter) {
        this.designRepositoryAclService = designRepositoryAclService;
        this.validationProvider = validationService;
        this.createUpdateProjectModelValidator = createUpdateProjectModelValidator;
        this.zipArchiveValidator = zipArchiveValidator;
        this.zipProjectSaveStrategy = zipProjectSaveStrategy;
        this.lockManager = new LockManager(Path.of(homeDirectory).resolve("locks/api"));
        this.aclProjectsHelper = aclProjectsHelper;
        this.designTimeRepositoryService = designTimeRepositoryService;
        this.projectRevisionService = projectRevisionService;
        this.bypassService = bypassService;
        this.projectCreationService = projectCreationService;
        this.projectCreationTargetResolver = projectCreationTargetResolver;
        this.repositoryConfigService = repositoryConfigService;
        this.projectIdentityConverter = projectIdentityConverter;
    }

    @Lookup("commentService")
    protected Comments getCommentsService(String repoName) {
        // Spring overrides this method to return the repository-scoped bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    @GetMapping
    @Operation(summary = "repos.get-repository-list.summary", description = "repos.get-repository-list.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-repository-list.200.desc")
    public List<RepositoryViewModel> getRepositoryList() {
        return designTimeRepositoryService.getRepositoryList();
    }

    @GetMapping("/{repo-name}/config")
    @Operation(summary = "repos.get-config.summary", description = "repos.get-config.desc")
    public RepositoryConfigModel getConfig(@DesignRepository("repo-name") Repository repository) {
        // Creating a project needs the repository itself, so the settings of its forms are read here. A
        // form of an existing project reads them through the project instead, because access may be granted
        // on the project alone.
        return repositoryConfigService.getConfig(repository.getId());
    }

    @Operation(summary = "repos.list-branches.summary", description = "repos.list-branches.desc")
    @GetMapping("/{repo-name}/branches")
    public List<String> listBranches(@DesignRepository("repo-name") Repository repository) throws IOException {
        return designTimeRepositoryService.getBranches(repository);
    }

    /**
     * Superseded by {@code GET /projects/{projectId}/history}, which the Revisions tab — the only caller this ever
     * had — now asks instead. A project is named here by the name the repository published it under, which stops
     * being the name its own user knows it by once the project is renamed in {@code rules.xml}.
     */
    @GetMapping({"/{repo-name}/projects/{project-name}/history",
            "/{repo-name}/branches/{branch-name}/projects/{project-name}/history"})
    @Operation(summary = "repos.get-project-revs.summary", description = "repos.get-project-revs.desc")
    @JsonView({UserInfoModel.View.Short.class})
    @Deprecated(forRemoval = false)
    public PageResponse<ProjectRevision> getProjectRevision(@DesignRepository("repo-name") Repository repository,
                                                            @Parameter(description = "repo.param.branch-name.desc") @PathVariable("branch-name") Optional<String> branch,
                                                            @Parameter(description = "repo.param.project-name.desc") @PathVariable("project-name") String projectName,
                                                            @Parameter(description = "repo.param.search.desc") @RequestParam(value = "search", required = false) String searchTerm,
                                                            @Parameter(description = "repo.param.techRevs.desc") @RequestParam(name = "techRevs", required = false, defaultValue = "false") boolean techRevs,
                                                            @PaginationDefault Pageable page) throws IOException, ProjectException {
        return projectRevisionService.getProjectRevision(
                repository,
                projectName,
                branch.orElse(null),
                searchTerm,
                techRevs,
                page);
    }

    @PutMapping(value = "/{repo-name}/projects/{project-name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "repos.create-project.summary", description = "repos.create-project.desc")
    @JsonView(GenericView.CreateOrUpdate.class)
    public ProjectViewModel createProject(@DesignRepository("repo-name") Repository repository,
                                          @Parameter(description = "repos.create-project.param.project-name.desc") @PathVariable("project-name") String projectName,
                                          @Parameter(description = "repos.create-project.param.path.desc") @RequestParam(value = "path", required = false) String path,
                                          @Parameter(description = "repos.create-project.param.comment.desc") @RequestParam(value = "comment", required = false) String comment,
                                          @Parameter(description = "repos.create-project.param.template.desc", content = @Content(encoding = @Encoding(contentType = "application/octet-stream"))) @RequestParam(value = "template", required = false) List<MultipartFile> files,
                                          @Parameter(description = "repos.create-project.param.template-type.desc") @RequestParam(value = "templateType", required = false) String templateType,
                                          @Parameter(description = "repos.create-project.param.template-category.desc") @RequestParam(value = "templateCategory", required = false) String templateCategory,
                                          @Parameter(description = "repos.create-project.param.template-name.desc") @RequestParam(value = "templateName", required = false) String templateName,
                                          @Parameter(description = "repos.create-project.param.models-module-name.desc") @RequestParam(value = "modelsModuleName", defaultValue = "Models") String modelsModuleName,
                                          @Parameter(description = "repos.create-project.param.models-path.desc") @RequestParam(value = "modelsPath", defaultValue = "rules/Models.xlsx") String modelsPath,
                                          @Parameter(description = "repos.create-project.param.algorithms-module-name.desc") @RequestParam(value = "algorithmsModuleName", defaultValue = "Algorithms") String algorithmsModuleName,
                                          @Parameter(description = "repos.create-project.param.algorithms-path.desc") @RequestParam(value = "algorithmsPath", defaultValue = "rules/Algorithms.xlsx") String algorithmsPath,
                                          @Parameter(description = "repos.create-project.param.overwrite.desc") @RequestParam(value = "overwrite", required = false, defaultValue = "false") Boolean overwrite,
                                          @Parameter(description = "repos.create-project.param.status.desc", schema = @Schema(allowableValues = {"OPENED", "CLOSED"})) @RequestParam(value = "status", required = false) @Nullable ProjectStatus status,
                                          @Parameter(description = "repos.create-project.param.branch.desc") @RequestParam(value = "branch", required = false) @Nullable String branch,
                                          @Parameter(description = "repos.create-project.param.force.desc") @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) throws IOException {
        var hasFiles = files != null && !files.isEmpty();
        var archiveUpload = hasFiles && files.size() == 1 && FileTypeHelper.isZipFile(files.getFirst().getOriginalFilename());
        if (!hasFiles && StringUtils.isBlank(templateName)) {
            throw new BadRequestException("repos.create-project.no-source.message");
        }

        String resolvedComment = StringUtils.isNotBlank(comment) ? comment
                : getCommentsService(repository.getId()).createProject(projectName);
        var model = new CreateUpdateProjectModel(repository.getId(), getUserName(),
                StringUtils.trimToNull(projectName), StringUtils.trimToNull(path), resolvedComment, overwrite, branch);
        validationProvider.validate(model);
        validationProvider.validate(model, createUpdateProjectModelValidator);

        // Overwrite only applies to re-uploading a project archive; otherwise a create grant is required.
        var archiveOverwrite = overwrite && archiveUpload;
        if (!archiveOverwrite && !aclProjectsHelper.hasCreateProjectPermission(repository.getId())) {
            throw new ForbiddenException();
        }
        allowedToPushRequestedBranch(repository, branch, force);
        var targetRepository = projectCreationTargetResolver.resolve(repository, branch, !archiveOverwrite);
        if (archiveOverwrite) {
            String pathInRepo = targetRepository.supports().mappedFolders()
                    ? AclPathUtils.concatPaths(path, projectName)
                    : projectName;
            if (!designRepositoryAclService
                    .isGranted(targetRepository.getId(), pathInRepo, List.of(BasePermission.WRITE))) {
                throw new ForbiddenException();
            }
        }

        allowedToPush(targetRepository, force);

        FileData data;
        // An uploaded archive keeps the robust create/overwrite path (locking, overwrite).
        if (archiveUpload) {
            data = createFromArchive(targetRepository, projectName, files.getFirst(), model);
        } else {
            data = createFromContent(targetRepository, projectName, path, resolvedComment, files,
                    templateType, templateCategory, templateName, modelsPath, algorithmsPath, modelsModuleName,
                    algorithmsModuleName);
        }
        // Content-based creation historically leaves a project opened. An archive stays closed unless the caller
        // explicitly requests otherwise.
        var effectiveStatus = status == null && !archiveUpload ? ProjectStatus.VIEWING : status;
        projectCreationService.applyStatusAfterCreate(targetRepository, FileUtils.getName(data.getName()),
                effectiveStatus);
        return mapFileDataResponse(data, targetRepository.supports());
    }

    private FileData createFromArchive(Repository repository, String projectName, MultipartFile file,
                                       CreateUpdateProjectModel model) throws IOException {
        final Path archiveTmp = FileUtils.createPrivateTempFile(projectName, ".zip");
        Lock lock = null;
        try {
            lock = getLock(repository, model);
            IOUtils.copyAndClose(file.getInputStream(), Files.newOutputStream(archiveTmp));
            if (!lock.tryLock(getUserName(), 15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Cannot create a lock.");
            }
            validationProvider.validate(model, createUpdateProjectModelValidator);
            validationProvider.validate(archiveTmp, zipArchiveValidator);
            var data = zipProjectSaveStrategy.save(repository, model, archiveTmp);
            var project = new AProject(repository, data);
            ProjectCreationService.grantContributorAclIfAbsent(designRepositoryAclService, project);
            projectCreationService.registerExtensibleTags(project);
            projectCreationService.awaitProjectVisibility(repository);
            projectCreationService.refreshWorkspaceAfterDesignChange();
            return data;
        } finally {
            FileUtils.deleteQuietly(archiveTmp);
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private FileData createFromContent(Repository repository, String projectName, String path, String comment,
                                       List<MultipartFile> files, String templateType, String templateCategory,
                                       String templateName, String modelsPath, String algorithmsPath,
                                       String modelsModuleName, String algorithmsModuleName) throws IOException {
        if (files == null || files.isEmpty()) {
            return projectCreationService.createFromTemplate(repository, StringUtils.trimToNull(projectName),
                    path, templateType, templateCategory, templateName, comment, null);
        }
        var projectFiles = new ArrayList<ProjectFile>();
        try {
            for (MultipartFile file : files) {
                var projectFile = new ProjectFile(file.getOriginalFilename(), file.getInputStream());
                projectFiles.add(projectFile);
                verifyIntegrity(projectFile);
            }
            return projectCreationService.createFromFiles(repository, StringUtils.trimToNull(projectName), path,
                    projectFiles, comment, modelsPath, algorithmsPath, modelsModuleName, algorithmsModuleName, null);
        } finally {
            projectFiles.forEach(ProjectFile::destroy);
        }
    }

    /**
     * Verifies that an uploaded workbook or archive arrived complete, so a project is never created
     * from a module that was cut short on its way here. A file of any other type is left alone.
     */
    private static void verifyIntegrity(ProjectFile file) throws IOException {
        if (!FileIntegrityValidator.isVerified(file.getName())) {
            return;
        }
        var buffered = file.getTempFile().toPath();
        try {
            FileIntegrityValidator.verify(file.getName(), buffered);
        } catch (IOException e) {
            throw FileIntegrityValidator.damagedContent(file.getName(), e);
        }
    }

    @GetMapping("/project-templates")
    @Operation(summary = "repos.list-project-templates.summary", description = "repos.list-project-templates.desc")
    public List<ProjectTemplateGroup> getProjectTemplates() {
        return projectCreationService.listTemplates();
    }

    @PostMapping(value = "/{repo-name}/projects/from-workspace", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "repos.create-from-workspace.summary", description = "repos.create-from-workspace.desc")
    public void createProjectsFromWorkspace(@DesignRepository("repo-name") Repository repository,
                                            @Valid @RequestBody CreateFromWorkspaceModel request) {
        if (request.names() == null || request.names().isEmpty()) {
            throw new BadRequestException("repos.create-project.no-source.message");
        }
        requireCreatePermission(repository);
        // Reject a duplicate name or an invalid comment before publishing.
        for (String name : request.names()) {
            validatedCreateModel(repository, name, request.path(), request.comment(), request.branch());
        }
        allowedToPushRequestedBranch(repository, request.branch(), false);
        var targetRepository = projectCreationTargetResolver.resolve(repository, request.branch());
        allowedToPush(targetRepository, false);
        projectCreationService.uploadLocalProjects(targetRepository,
                request.names(),
                request.path(),
                request.comment());
    }

    @PostMapping(value = "/{repo-name}/projects/{project-name}/from-project", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "repos.create-from-project.summary", description = "repos.create-from-project.desc")
    public ProjectViewModel createProjectFromProject(@DesignRepository("repo-name") Repository repository,
                                                     @Parameter(description = "repos.create-from-project.param.project-name.desc") @PathVariable("project-name") String projectName,
                                                     @Valid @RequestBody CreateFromProjectModel request) {
        requireCreatePermission(repository);
        // The source is resolved first: the default comment names the project that is actually copied, not the
        // identifier the request happened to address it by. Reading it is the copy's own check to make, so that a
        // refusal carries the endpoint's message.
        var source = projectIdentityConverter.resolveProjectIdentity(request.sourceProject(),
                request.sourceRepositoryId());
        if (source == null) {
            throw new NotFoundException("project.identifier.message");
        }
        // Validate the target name, comment and path before copying. An omitted comment falls back to the
        // repository "copied from" template rather than to the create-project one.
        String comment = StringUtils.isNotBlank(request.comment()) ? request.comment()
                : getCommentsService(repository.getId()).copiedFrom(source.getBusinessName());
        var model = validatedCreateModel(repository, projectName, request.path(), comment, request.branch());
        allowedToPushRequestedBranch(repository, request.branch(), false);
        var targetRepository = projectCreationTargetResolver.resolve(repository, request.branch());
        allowedToPush(targetRepository, false);
        var data = projectCreationService.copyProject(targetRepository, model.getProjectName(),
                request.path(), source, model.getComment(), request.revision());
        return mapFileDataResponse(data, repository.supports());
    }

    private void requireCreatePermission(Repository repository) {
        if (!aclProjectsHelper.hasCreateProjectPermission(repository.getId())) {
            throw new ForbiddenException();
        }
    }

    /**
     * Build and validate a create model — project-name format, comment (length + repo pattern) and the
     * duplicate/path-conflict check — reused by the copy and publish flows so they match the archive path
     * and the legacy repository tab. Never allows overwrite.
     */
    private CreateUpdateProjectModel validatedCreateModel(Repository repository,
                                                          String projectName,
                                                          String path,
                                                          String comment,
                                                          @Nullable String branch) {
        String resolved = StringUtils.isNotBlank(comment) ? comment
                : getCommentsService(repository.getId()).createProject(projectName);
        var model = new CreateUpdateProjectModel(repository.getId(), getUserName(),
                StringUtils.trimToNull(projectName), StringUtils.trimToNull(path), resolved, false, branch);
        validationProvider.validate(model);
        validationProvider.validate(model, createUpdateProjectModelValidator);
        return model;
    }

    private Lock getLock(Repository repository, CreateUpdateProjectModel model) {
        var lockId = new StringBuilder(model.getRepoName());
        if (repository.supports().branches()) {
            lockId.append("/[branches]/").append(((BranchRepository) repository).getBranch()).append('/');
        }
        if (repository.supports().mappedFolders() && StringUtils.isNotEmpty(model.getPath())) {
            lockId.append(model.getPath());
        }
        lockId.append(model.getProjectName());
        return lockManager.getLock(lockId.toString());
    }

    private ProjectViewModel mapFileDataResponse(FileData src, Features features) {
        var builder = ProjectViewModel.builder();
        if (features.branches()) {
            builder.branch(src.getBranch());
        }
        builder.revision(src.getVersion());
        return builder.build();
    }

    private String getUserName() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void allowedToPush(Repository repo, boolean force) {
        if (repo.supports().branches()) {
            var branchRepo = (BranchRepository) repo;
            bypassService.requireBypassOrThrow(branchRepo, branchRepo.getBranch(), repo.getId(), force);
        }
    }

    private void allowedToPushRequestedBranch(Repository repo, @Nullable String requestedBranch, boolean force) {
        var branch = StringUtils.trimToNull(requestedBranch);
        if (branch != null && repo.supports().branches()) {
            bypassService.requireBypassOrThrow((BranchRepository) repo, branch, repo.getId(), force);
        }
    }

}
