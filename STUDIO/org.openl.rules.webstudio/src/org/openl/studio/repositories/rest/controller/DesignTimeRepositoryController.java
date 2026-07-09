package org.openl.studio.repositories.rest.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.Authentication;
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
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.utils.AclPathUtils;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.model.CreateFromProjectModel;
import org.openl.studio.repositories.model.CreateFromRepositoryModel;
import org.openl.studio.repositories.model.CreateFromWorkspaceModel;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.studio.repositories.model.ProjectRevision;
import org.openl.studio.repositories.model.ProjectTemplateGroup;
import org.openl.studio.repositories.model.RepositoryFolder;
import org.openl.studio.repositories.model.RepositoryViewModel;
import org.openl.studio.repositories.rest.resolver.DesignRepository;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;
import org.openl.studio.repositories.service.HistoryRepositoryMapper;
import org.openl.studio.repositories.service.ProjectCreationService;
import org.openl.studio.repositories.service.ProjectRevisionService;
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

    private final DesignTimeRepository designTimeRepository;
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

    @Autowired
    public DesignTimeRepositoryController(DesignTimeRepository designTimeRepository,
                                          @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
                                          BeanValidationProvider validationService,
                                          CreateUpdateProjectModelValidator createUpdateProjectModelValidator,
                                          ZipArchiveValidator zipArchiveValidator,
                                          ZipProjectSaveStrategy zipProjectSaveStrategy,
                                          @Value("${openl.home.shared}") String homeDirectory,
                                          AclProjectsHelper aclProjectsHelper,
                                          DesignTimeRepositoryService designTimeRepositoryService,
                                          ProjectRevisionService projectRevisionService,
                                          ProtectedBranchBypassService bypassService,
                                          ProjectCreationService projectCreationService) {
        this.designTimeRepository = designTimeRepository;
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
    }

    @Lookup
    protected HistoryRepositoryMapper getHistoryRepositoryMapper(Repository repository) {
        return null;
    }

    @Lookup("commentService")
    protected Comments getCommentsService(String repoName) {
        return null;
    }

    @GetMapping
    @Operation(summary = "repos.get-repository-list.summary", description = "repos.get-repository-list.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-repository-list.200.desc")
    public List<RepositoryViewModel> getRepositoryList() {
        return designTimeRepositoryService.getRepositoryList();
    }

    @Operation(summary = "repos.list-branches.summary", description = "repos.list-branches.desc")
    @GetMapping("/{repo-name}/branches")
    public List<String> listBranches(@DesignRepository("repo-name") Repository repository) throws IOException {
        return designTimeRepositoryService.getBranches(repository);
    }

    @GetMapping({"/{repo-name}/projects/{project-name}/history",
            "/{repo-name}/branches/{branch-name}/projects/{project-name}/history"})
    @Operation(summary = "repos.get-project-revs.summary", description = "repos.get-project-revs.desc")
    @JsonView({UserInfoModel.View.Short.class})
    public PageResponse<ProjectRevision> getProjectRevision(@DesignRepository("repo-name") Repository repository,
                                                            @Parameter(description = "repo.param.branch-name.desc") @PathVariable(value = "branch-name") Optional<String> branch,
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
                                          @Parameter(description = "repos.create-project.param.force.desc") @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) throws IOException,
            ProjectException {
        boolean hasFiles = files != null && !files.isEmpty();
        boolean archiveUpload = hasFiles && files.size() == 1 && FileTypeHelper.isZipFile(files.getFirst().getOriginalFilename());
        if (!hasFiles && StringUtils.isBlank(templateName)) {
            throw new BadRequestException("repos.create-project.no-source.message");
        }

        // Overwrite only applies to re-uploading a project archive; otherwise a create grant is required.
        if (overwrite && archiveUpload) {
            String pathInRepo = repository.supports().mappedFolders() ? AclPathUtils.concatPaths(path, projectName) : projectName;
            if (!designRepositoryAclService.isGranted(repository.getId(), pathInRepo, List.of(BasePermission.WRITE))) {
                throw new ForbiddenException();
            }
        } else if (!aclProjectsHelper.hasCreateProjectPermission(repository.getId())) {
            throw new ForbiddenException();
        }

        allowedToPush(repository, force);

        String resolvedComment = StringUtils.isNotBlank(comment) ? comment
                : getCommentsService(repository.getId()).createProject(projectName);

        // Validate the project name and comment for EVERY create mode (archive, excel, openapi, template).
        CreateUpdateProjectModel model = new CreateUpdateProjectModel(repository.getId(), getUserName(),
                StringUtils.trimToNull(projectName), StringUtils.trimToNull(path), resolvedComment, overwrite);
        validationProvider.validate(model);

        // An uploaded archive keeps the robust create/overwrite path (locking, overwrite).
        if (archiveUpload) {
            return createFromArchive(repository, projectName, files.getFirst(), model);
        }
        // Excel/OpenAPI uploads and templates: reject duplicate names and comment violations up front,
        // matching the archive path and the legacy tab (the content dispatcher has no such guard).
        validationProvider.validate(model, createUpdateProjectModelValidator);
        FileData data = createFromContent(repository, projectName, path, resolvedComment, files,
                templateType, templateCategory, templateName, modelsPath, algorithmsPath, modelsModuleName,
                algorithmsModuleName);
        return mapFileDataResponse(data, repository.supports());
    }

    private ProjectViewModel createFromArchive(Repository repository, String projectName, MultipartFile file,
                                               CreateUpdateProjectModel model) throws IOException, ProjectException {
        final Path archiveTmp = Files.createTempFile(projectName, ".zip");
        final Lock lock = getLock(repository, model);
        try {
            IOUtils.copyAndClose(file.getInputStream(), Files.newOutputStream(archiveTmp));
            if (!lock.tryLock(getUserName(), 15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Cannot create a lock.");
            }
            validationProvider.validate(model, createUpdateProjectModelValidator);
            validationProvider.validate(archiveTmp, zipArchiveValidator);
            FileData data = zipProjectSaveStrategy.save(model, archiveTmp);
            var project = designTimeRepository.getProject(repository.getId(), projectName);
            ProjectCreationService.grantContributorAclIfAbsent(designRepositoryAclService, project);
            projectCreationService.registerExtensibleTagsAfterDesignChange(project);
            return mapFileDataResponse(data, repository.supports());
        } finally {
            FileUtils.deleteQuietly(archiveTmp);
            lock.unlock();
        }
    }

    private FileData createFromContent(Repository repository, String projectName, String path, String comment,
                                       List<MultipartFile> files, String templateType, String templateCategory,
                                       String templateName, String modelsPath, String algorithmsPath,
                                       String modelsModuleName, String algorithmsModuleName) throws IOException {
        if (files == null || files.isEmpty()) {
            return projectCreationService.createFromTemplate(repository.getId(), StringUtils.trimToNull(projectName),
                    path, templateType, templateCategory, templateName, comment, null);
        }
        List<ProjectFile> projectFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                projectFiles.add(new ProjectFile(file.getOriginalFilename(), file.getInputStream()));
            }
            return projectCreationService.createFromFiles(repository.getId(), StringUtils.trimToNull(projectName), path,
                    projectFiles, comment, modelsPath, algorithmsPath, modelsModuleName, algorithmsModuleName, null);
        } finally {
            projectFiles.forEach(ProjectFile::destroy);
        }
    }

    @GetMapping("/project-templates")
    @Operation(summary = "repos.list-project-templates.summary", description = "repos.list-project-templates.desc")
    public List<ProjectTemplateGroup> getProjectTemplates() {
        return projectCreationService.listTemplates();
    }

    @PostMapping(value = "/{repo-name}/projects/from-workspace", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Publish local workspace projects to a repository (BETA)")
    public void createProjectsFromWorkspace(@DesignRepository("repo-name") Repository repository,
                                            @Valid @RequestBody CreateFromWorkspaceModel request) {
        if (request.names() == null || request.names().isEmpty()) {
            throw new BadRequestException("repos.create-project.no-source.message");
        }
        allowedToPush(repository, false);
        // Reject a duplicate name or an invalid comment before publishing.
        for (String name : request.names()) {
            validatedCreateModel(repository, name, request.path(), request.comment());
        }
        projectCreationService.uploadLocalProjects(repository.getId(), request.names(), request.path(), request.comment());
    }

    @PostMapping(value = "/{repo-name}/projects/{project-name}/from-project", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Copy an existing project into a repository under a new name (BETA)")
    public ProjectViewModel createProjectFromProject(@DesignRepository("repo-name") Repository repository,
                                                     @Parameter(description = "New project name") @PathVariable("project-name") String projectName,
                                                     @Valid @RequestBody CreateFromProjectModel request) {
        allowedToPush(repository, false);
        // Validate the target name, comment and path before copying.
        var model = validatedCreateModel(repository, projectName, request.path(), request.comment());
        var data = projectCreationService.copyProject(repository.getId(), model.getProjectName(),
                request.path(), request.sourceRepositoryId(), request.sourceProjectName(), model.getComment());
        return mapFileDataResponse(data, repository.supports());
    }

    /**
     * Build and validate a create model — project-name format, comment (length + repo pattern) and the
     * duplicate/path-conflict check — reused by the copy and publish flows so they match the archive path
     * and the legacy repository tab. Never allows overwrite.
     */
    private CreateUpdateProjectModel validatedCreateModel(Repository repository, String projectName, String path,
                                                          String comment) {
        String resolved = StringUtils.isNotBlank(comment) ? comment
                : getCommentsService(repository.getId()).createProject(projectName);
        CreateUpdateProjectModel model = new CreateUpdateProjectModel(repository.getId(), getUserName(),
                StringUtils.trimToNull(projectName), StringUtils.trimToNull(path), resolved, false);
        validationProvider.validate(model);
        validationProvider.validate(model, createUpdateProjectModelValidator);
        return model;
    }

    @GetMapping("/{repo-name}/folders")
    @Operation(summary = "List importable folders of a non-flat repository (BETA)")
    public List<RepositoryFolder> listImportableFolders(@DesignRepository("repo-name") Repository repository,
                                                        @Parameter(description = "Internal folder whose children to list; empty lists the repository root") @RequestParam(value = "path", required = false) String path) {
        return projectCreationService.listImportableFolders(repository.getId(), path);
    }

    @PostMapping(value = "/{repo-name}/projects/from-repository", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Import an existing repository folder as a project (BETA)")
    public ProjectViewModel createProjectFromRepository(@DesignRepository("repo-name") Repository repository,
                                                        @Valid @RequestBody CreateFromRepositoryModel request) {
        allowedToPush(repository, false);
        var data = projectCreationService.importFromRepository(repository.getId(), request.path(), request.tags());
        return mapFileDataResponse(data, repository.supports());
    }

    private Lock getLock(Repository repository, CreateUpdateProjectModel model) {
        StringBuilder lockId = new StringBuilder(model.getRepoName());
        if (repository.supports().branches()) {
            lockId.append("/[branches]/").append(((BranchRepository) repository).getBaseBranch()).append('/');
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void allowedToPush(Repository repo, boolean force) {
        if (repo.supports().branches()) {
            BranchRepository branchRepo = (BranchRepository) repo;
            bypassService.requireBypassOrThrow(branchRepo, branchRepo.getBranch(), repo.getId(), force);
        }
    }

}
