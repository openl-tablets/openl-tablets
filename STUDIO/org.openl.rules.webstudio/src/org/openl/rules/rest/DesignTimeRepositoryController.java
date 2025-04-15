package org.openl.rules.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lock.Lock;
import org.openl.rules.lock.LockManager;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.rest.model.GenericView;
import org.openl.rules.rest.model.PageResponse;
import org.openl.rules.rest.model.ProjectRevision;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.rest.model.RepositoryFeatures;
import org.openl.rules.rest.model.RepositoryViewModel;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.resolver.DesignRepository;
import org.openl.rules.rest.resolver.PaginationDefault;
import org.openl.rules.rest.service.HistoryRepositoryMapper;
import org.openl.rules.rest.service.ProjectCriteriaQuery;
import org.openl.rules.rest.service.RepositoryProjectService;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.rest.validation.CreateUpdateProjectModelValidator;
import org.openl.rules.rest.validation.ZipArchiveValidator;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.utils.AclPathUtils;
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
    private final RepositoryProjectService projectService;

    @Autowired
    public DesignTimeRepositoryController(DesignTimeRepository designTimeRepository,
                                          @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
                                          BeanValidationProvider validationService,
                                          CreateUpdateProjectModelValidator createUpdateProjectModelValidator,
                                          ZipArchiveValidator zipArchiveValidator,
                                          ZipProjectSaveStrategy zipProjectSaveStrategy,
                                          @Value("${openl.home.shared}") String homeDirectory,
                                          RepositoryProjectService projectService) {
        this.designTimeRepository = designTimeRepository;
        this.designRepositoryAclService = designRepositoryAclService;
        this.validationProvider = validationService;
        this.createUpdateProjectModelValidator = createUpdateProjectModelValidator;
        this.zipArchiveValidator = zipArchiveValidator;
        this.zipProjectSaveStrategy = zipProjectSaveStrategy;
        this.lockManager = new LockManager(Paths.get(homeDirectory).resolve("locks/api"));
        this.projectService = projectService;
    }

    @Lookup
    protected HistoryRepositoryMapper getHistoryRepositoryMapper(Repository repository) {
        return null;
    }

    @Lookup("commentService")
    protected Comments getCommentsService(String repoName) {
        return null;
    }

    @GetMapping("/{repo-name}/features")
    @Operation(summary = "repos.get-features.summary", description = "repos.get-features.desc")
    public RepositoryFeatures getFeatures(@DesignRepository("repo-name") Repository repository) {
        var supports = repository.supports();
        return new RepositoryFeatures(supports.branches(), supports.searchable());
    }

    @GetMapping
    @Operation(summary = "repos.get-repository-list.summary", description = "repos.get-repository-list.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-repository-list.200.desc")
    public List<RepositoryViewModel> getRepositoryList() {
        return designTimeRepository.getRepositories()
                .stream()
                .filter(repo -> designRepositoryAclService.isGranted(repo.getId(), null, List.of(AclPermission.VIEW)))
                .map(repo -> new RepositoryViewModel(repo.getId(), repo.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{repo-name}/projects")
    @Operation(summary = "repos.get-project-list-by-repository.summary", description = "repos.get-project-list-by-repository.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-project-list-by-repository.200.desc")
    public List<ProjectViewModel> getProjectListByRepository(@DesignRepository("repo-name") Repository repository) {
        if (!designRepositoryAclService.isGranted(repository.getId(), null, List.of(AclPermission.VIEW))) {
            throw new SecurityException();
        }
        return projectService.getProjects(
                ProjectCriteriaQuery.builder().repositoryId(repository.getId()).build());
    }

    @Operation(summary = "repos.list-branches.summary", description = "repos.list-branches.desc")
    @GetMapping("/{repo-name}/branches")
    public List<String> listBranches(@DesignRepository("repo-name") Repository repository) throws IOException {
        if (!designRepositoryAclService.isGranted(repository.getId(), null, List.of(AclPermission.VIEW))) {
            throw new SecurityException();
        }
        if (!repository.supports().branches()) {
            throw new ConflictException("repository.branch.unsupported.message");
        }
        var branches = ((BranchRepository) repository).getBranches(null);
        branches.sort(String.CASE_INSENSITIVE_ORDER);
        return branches;
    }

    @GetMapping({"/{repo-name}/projects/{project-name}/history",
            "/{repo-name}/branches/{branch-name}/projects/{project-name}/history"})
    @Parameters({
            @Parameter(name = "page", description = "pagination.param.page.desc", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0")),
            @Parameter(name = "size", description = "pagination.param.size.desc", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32", minimum = "1", defaultValue = "50"))})
    @Operation(summary = "repos.get-project-revs.summary", description = "repos.get-project-revs.desc")
    @JsonView({UserInfoModel.View.Short.class})
    public PageResponse<ProjectRevision> getProjectRevision(@DesignRepository("repo-name") Repository repository,
                                                            @Parameter(description = "repo.param.branch-name.desc") @PathVariable(value = "branch-name") Optional<String> branch,
                                                            @Parameter(description = "repo.param.project-name.desc") @PathVariable("project-name") String projectName,
                                                            @Parameter(description = "repo.param.search.desc") @RequestParam(value = "search", required = false) String searchTerm,
                                                            @Parameter(description = "repo.param.techRevs.desc") @RequestParam(name = "techRevs", required = false, defaultValue = "false") boolean techRevs,
                                                            @PaginationDefault(size = 50) Pageable page) throws IOException, ProjectException {
        if (branch.isPresent()) {
            repository = checkoutBranchIfPresent(repository, branch.get());
        }
        AProject project;
        try {
            project = designTimeRepository.getProject(repository.getId(), projectName);
        } catch (ProjectException e) {
            throw new NotFoundException("project.message", projectName);
        }

        if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
            throw new SecurityException();
        }

        String fullPath;
        if (repository.supports().mappedFolders()) {
            fullPath = designTimeRepository.getProject(repository.getId(), projectName).getFolderPath();
        } else {
            fullPath = designTimeRepository.getRulesLocation() + projectName;
        }

        return getHistoryRepositoryMapper(repository).getProjectHistory(fullPath, searchTerm, techRevs, page);
    }

    @PutMapping(value = "/{repo-name}/projects/{project-name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "repos.create-project-from-zip.summary", description = "repos.create-project-from-zip.desc")
    @JsonView(GenericView.CreateOrUpdate.class)
    public ProjectViewModel createProjectFromZip(@DesignRepository("repo-name") Repository repository,
                                                 @Parameter(description = "repos.create-project-from-zip.param.project-name.desc") @PathVariable("project-name") String projectName,
                                                 @Parameter(description = "repos.create-project-from-zip.param.path.desc") @RequestParam(value = "path", required = false) String path,
                                                 @Parameter(description = "repos.create-project-from-zip.param.comment.desc") @RequestParam(value = "comment", required = false) String comment,
                                                 @Parameter(description = "repos.create-project-from-zip.param.template.desc", content = @Content(encoding = @Encoding(contentType = "application/zip"))) @RequestParam("template") MultipartFile file,
                                                 @Parameter(description = "repos.create-project-from-zip.param.overwrite.desc") @RequestParam(value = "overwrite", required = false, defaultValue = "false") Boolean overwrite) throws IOException,
            JAXBException {
        if (overwrite) {
            String pathInRepo = repository.supports().mappedFolders() ? AclPathUtils.concatPaths(path, projectName) : projectName;
            if (!designRepositoryAclService.isGranted(repository.getId(), pathInRepo, List.of(AclPermission.EDIT))) {
                throw new SecurityException();
            }
        } else if (!designRepositoryAclService.isGranted(repository.getId(), null, List.of(AclPermission.CREATE))) {
            throw new SecurityException();
        }

        allowedToPush(repository);

        CreateUpdateProjectModel model = new CreateUpdateProjectModel(repository.getId(),
                getUserName(),
                StringUtils.trimToNull(projectName),
                StringUtils.trimToNull(path),
                StringUtils.isNotBlank(comment) ? comment
                        : getCommentsService(repository.getId()).createProject(projectName),
                overwrite);
        validationProvider.validate(model); // perform basic validation

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
            return mapFileDataResponse(data, repository.supports());
        } finally {
            FileUtils.deleteQuietly(archiveTmp);
            lock.unlock();
        }
    }

    private Lock getLock(Repository repository, CreateUpdateProjectModel model) {
        StringBuilder lockId = new StringBuilder(model.getRepoName());
        if (repository.supports().branches()) {
            lockId.append("/[branches]/").append(((BranchRepository) repository).getBaseBranch()).append('/');
        }
        if (repository.supports().mappedFolders() && !StringUtils.isNotEmpty(model.getPath())) {
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

    private void allowedToPush(Repository repo) {
        if (repo.supports().branches()) {
            BranchRepository branchRepo = (BranchRepository) repo;
            if (branchRepo.isBranchProtected(branchRepo.getBranch())) {
                throw new ForbiddenException("default.message");
            }
        }
    }

    private Repository checkoutBranchIfPresent(Repository repository, String branch) throws IOException {
        if (!repository.supports().branches()) {
            throw new NotFoundException("repository.branch.message");
        }
        branch = branch.replace(' ', '/');
        BranchRepository branchRepo = ((BranchRepository) repository);
        if (!branchRepo.branchExists(branch)) {
            throw new NotFoundException("repository.branch.message");
        }
        return branchRepo.forBranch(branch);
    }
}
