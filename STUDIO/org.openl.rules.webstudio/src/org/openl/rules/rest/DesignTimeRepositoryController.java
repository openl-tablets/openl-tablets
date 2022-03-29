package org.openl.rules.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openl.rules.lock.Lock;
import org.openl.rules.lock.LockManager;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.rest.model.GenericView;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.rest.model.RepositoryViewModel;
import org.openl.rules.rest.resolver.DesignRepository;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.rest.validation.CreateUpdateProjectModelValidator;
import org.openl.rules.rest.validation.ZipArchiveValidator;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.PropertyResolver;
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

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/repos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Design Repository")
public class DesignTimeRepositoryController {

    private final DesignTimeRepository designTimeRepository;
    private final PropertyResolver propertyResolver;
    private final BeanValidationProvider validationProvider;
    private final CreateUpdateProjectModelValidator createUpdateProjectModelValidator;
    private final ZipArchiveValidator zipArchiveValidator;
    private final ZipProjectSaveStrategy zipProjectSaveStrategy;
    private final LockManager lockManager;

    @Autowired
    public DesignTimeRepositoryController(DesignTimeRepository designTimeRepository,
            PropertyResolver propertyResolver,
            BeanValidationProvider validationService,
            CreateUpdateProjectModelValidator createUpdateProjectModelValidator,
            ZipArchiveValidator zipArchiveValidator,
            ZipProjectSaveStrategy zipProjectSaveStrategy,
            @Value("${openl.home.shared}") String homeDirectory) {
        this.designTimeRepository = designTimeRepository;
        this.propertyResolver = propertyResolver;
        this.validationProvider = validationService;
        this.createUpdateProjectModelValidator = createUpdateProjectModelValidator;
        this.zipArchiveValidator = zipArchiveValidator;
        this.zipProjectSaveStrategy = zipProjectSaveStrategy;
        this.lockManager = new LockManager(Paths.get(homeDirectory).resolve("locks/api"));
    }

    @GetMapping
    @Operation(summary = "repos.get-repository-list.summary", description = "repos.get-repository-list.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-repository-list.200.desc")
    public List<RepositoryViewModel> getRepositoryList() {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        return designTimeRepository.getRepositories()
            .stream()
            .map(repo -> new RepositoryViewModel(repo.getId(), repo.getName()))
            .collect(Collectors.toList());
    }

    @GetMapping("/{repo-name}/projects")
    @Operation(summary = "repos.get-project-list-by-repository.summary", description = "repos.get-project-list-by-repository.desc")
    @ApiResponse(responseCode = "200", description = "repos.get-project-list-by-repository.200.desc")
    public List<ProjectViewModel> getProjectListByRepository(@DesignRepository("repo-name") Repository repository) {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        return designTimeRepository.getProjects(repository.getId())
            .stream()
            .filter(proj -> !proj.isDeleted())
            .sorted(Comparator.comparing(AProject::getBusinessName, String.CASE_INSENSITIVE_ORDER))
            .map(src -> mapProjectResponse(src, repository.supports()))
            .collect(Collectors.toList());
    }

    private <T extends AProject> ProjectViewModel mapProjectResponse(T src, Features features) {
        var builder = ProjectViewModel.builder().name(src.getBusinessName());
        Optional.of(src.getFileData()).map(FileData::getAuthor).map(UserInfo::getName).ifPresent(builder::modifiedBy);
        Optional.of(src.getFileData())
            .map(FileData::getModifiedAt)
            .map(Date::toInstant)
            .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
            .ifPresent(builder::modifiedAt);
        Optional.of(src.getFileData()).map(FileData::getVersion).ifPresent(builder::rev);
        if (features.branches()) {
            Optional.of(src.getFileData()).map(FileData::getBranch).ifPresent(builder::branch);
        }
        if (features.mappedFolders()) {
            Optional.ofNullable(src.getRealPath()).map(p -> p.replace('\\', '/')).ifPresent(builder::path);
        }
        return builder.build();
    }

    @PutMapping(value = "/{repo-name}/projects/{project-name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "repos.create-project-from-zip.summary", description = "repos.create-project-from-zip.desc")
    @JsonView(GenericView.CreateOrUpdate.class)
    public ProjectViewModel createProjectFromZip(@DesignRepository("repo-name") Repository repository,
            @Parameter(description = "repos.create-project-from-zip.param.project-name.desc") @PathVariable("project-name") String projectName,
            @Parameter(description = "repos.create-project-from-zip.param.path.desc") @RequestParam(value = "path", required = false) String path,
            @Parameter(description = "repos.create-project-from-zip.param.comment.desc") @RequestParam(value = "comment", required = false) String comment,
            @Parameter(description = "repos.create-project-from-zip.param.template.desc", content = @Content(encoding = @Encoding(contentType = "application/zip"))) @RequestParam("template") MultipartFile file,
            @Parameter(description = "repos.create-project-from-zip.param.overwrite.desc") @RequestParam(value = "overwrite", required = false, defaultValue = "false") Boolean overwrite) throws IOException {

        SecurityChecker.allow(overwrite ? Privileges.EDIT_PROJECTS : Privileges.CREATE_PROJECTS);
        allowedToPush(repository);

        CreateUpdateProjectModel model = new CreateUpdateProjectModel(repository.getId(),
            getUserName(),
            StringUtils.trimToNull(projectName),
            StringUtils.trimToNull(path),
            StringUtils.isNotBlank(comment) ? comment
                                            : createCommentsService(repository.getId()).createProject(projectName),
            overwrite);
        validationProvider.validate(model); // perform basic validation

        final Path archiveTmp = Files.createTempFile(projectName, ".zip");
        final Lock lock = getLock(repository, model);
        try {
            IOUtils.copyAndClose(file.getInputStream(), Files.newOutputStream(archiveTmp));
            if (!lock.tryLock(getUserName(), 15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Can't create a lock.");
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

    private Comments createCommentsService(String repoName) {
        return new Comments(propertyResolver, repoName);
    }

    private ProjectViewModel mapFileDataResponse(FileData src, Features features) {
        var builder = ProjectViewModel.builder();
        if (features.branches()) {
            builder.branch(src.getBranch());
        }
        builder.rev(src.getVersion());
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
