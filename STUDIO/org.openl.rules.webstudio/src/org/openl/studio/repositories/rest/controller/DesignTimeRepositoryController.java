package org.openl.studio.repositories.rest.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import jakarta.xml.bind.JAXBException;

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
import org.springframework.web.bind.annotation.PutMapping;
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
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.utils.AclPathUtils;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.ProjectViewModel;
import org.openl.studio.projects.service.ProjectCriteriaQuery;
import org.openl.studio.projects.service.RepositoryProjectService;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.studio.repositories.model.ProjectRevision;
import org.openl.studio.repositories.model.RepositoryFeatures;
import org.openl.studio.repositories.model.RepositoryViewModel;
import org.openl.studio.repositories.rest.resolver.DesignRepository;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;
import org.openl.studio.repositories.service.HistoryRepositoryMapper;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.ZipProjectSaveStrategy;
import org.openl.studio.repositories.validator.CreateUpdateProjectModelValidator;
import org.openl.studio.repositories.validator.ZipArchiveValidator;
import org.openl.studio.rest.resolver.PaginationDefault;
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
    private final AclProjectsHelper aclProjectsHelper;
    private final DesignTimeRepositoryService designTimeRepositoryService;
    private final ProjectRevisionService projectRevisionService;

    @Autowired
    public DesignTimeRepositoryController(DesignTimeRepository designTimeRepository,
                                          @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
                                          BeanValidationProvider validationService,
                                          CreateUpdateProjectModelValidator createUpdateProjectModelValidator,
                                          ZipArchiveValidator zipArchiveValidator,
                                          ZipProjectSaveStrategy zipProjectSaveStrategy,
                                          @Value("${openl.home.shared}") String homeDirectory,
                                          RepositoryProjectService projectService, AclProjectsHelper aclProjectsHelper,
                                          DesignTimeRepositoryService designTimeRepositoryService,
                                          ProjectRevisionService projectRevisionService) {
        this.designTimeRepository = designTimeRepository;
        this.designRepositoryAclService = designRepositoryAclService;
        this.validationProvider = validationService;
        this.createUpdateProjectModelValidator = createUpdateProjectModelValidator;
        this.zipArchiveValidator = zipArchiveValidator;
        this.zipProjectSaveStrategy = zipProjectSaveStrategy;
        this.lockManager = new LockManager(Paths.get(homeDirectory).resolve("locks/api"));
        this.projectService = projectService;
        this.aclProjectsHelper = aclProjectsHelper;
        this.designTimeRepositoryService = designTimeRepositoryService;
        this.projectRevisionService = projectRevisionService;
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
        return designTimeRepositoryService.getFeatures(repository);
    }

    @GetMapping
    @Operation(summary = "repos.get-repository-list.summary", description = "repos.get-repository-list.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-repository-list.200.desc")
    public List<RepositoryViewModel> getRepositoryList() {
        return designTimeRepositoryService.getRepositoryList();
    }

    @GetMapping("/{repo-name}/projects")
    @Operation(summary = "repos.get-project-list-by-repository.summary", description = "repos.get-project-list-by-repository.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-project-list-by-repository.200.desc")
    @Deprecated(forRemoval = true)
    public Collection<ProjectViewModel> getProjectListByRepository(@DesignRepository("repo-name") Repository repository) {
        if (!designRepositoryAclService.isGranted(repository.getId(), null, List.of(BasePermission.READ))) {
            throw new ForbiddenException();
        }
        var query = ProjectCriteriaQuery.builder().repositoryId(repository.getId()).build();
        return projectService.getProjects(query, Pageable.unpaged()).getContent();
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
    @Operation(summary = "repos.create-project-from-zip.summary", description = "repos.create-project-from-zip.desc")
    @JsonView(GenericView.CreateOrUpdate.class)
    public ProjectViewModel createProjectFromZip(@DesignRepository("repo-name") Repository repository,
                                                 @Parameter(description = "repos.create-project-from-zip.param.project-name.desc") @PathVariable("project-name") String projectName,
                                                 @Parameter(description = "repos.create-project-from-zip.param.path.desc") @RequestParam(value = "path", required = false) String path,
                                                 @Parameter(description = "repos.create-project-from-zip.param.comment.desc") @RequestParam(value = "comment", required = false) String comment,
                                                 @Parameter(description = "repos.create-project-from-zip.param.template.desc", content = @Content(encoding = @Encoding(contentType = "application/zip"))) @RequestParam("template") MultipartFile file,
                                                 @Parameter(description = "repos.create-project-from-zip.param.overwrite.desc") @RequestParam(value = "overwrite", required = false, defaultValue = "false") Boolean overwrite) throws IOException,
            JAXBException, ProjectException {
        if (overwrite) {
            String pathInRepo = repository.supports().mappedFolders() ? AclPathUtils.concatPaths(path, projectName) : projectName;
            if (!designRepositoryAclService.isGranted(repository.getId(), pathInRepo, List.of(BasePermission.WRITE))) {
                throw new ForbiddenException();
            }
        } else if (!aclProjectsHelper.hasCreateProjectPermission(repository.getId())) {
            throw new ForbiddenException();
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
            var project = designTimeRepository.getProject(repository.getId(), projectName);
            if (!designRepositoryAclService.hasAcl(project)) {
                designRepositoryAclService.createAcl(project, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
            }
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

}
