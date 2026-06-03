package org.openl.studio.repositories.rest.controller;

import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.repository.api.Repository;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.files.FilePathPairRequest;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.rest.controller.AbstractFileOperationsController;
import org.openl.studio.projects.service.files.FileSearchQuery;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.files.RepoFileRootFactory;
import org.openl.studio.projects.validator.file.FileSearchQueryValidator;
import org.openl.studio.repositories.rest.resolver.DesignRepository;

/**
 * REST controller for repository file operations that involve two paths: copy and move, plus search.
 *
 * <p>These operations live outside the {@code /repos/{repo-name}/files/{*path}} address space. Both
 * paths travel in the request body, so a command name can never shadow a real file.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping(value = "/repos/{repo-name}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Repositories: Files (BETA)", description = "APIs for managing repository files")
@Validated
public class RepoFileOperationsController extends AbstractFileOperationsController {

    private final RepoFileRootFactory fileRootFactory;

    public RepoFileOperationsController(ProjectFilesService filesService,
                                        RepoFileRootFactory fileRootFactory,
                                        BeanValidationProvider validationProvider,
                                        FileSearchQueryValidator searchValidator) {
        super(filesService, validationProvider, searchValidator);
        this.fileRootFactory = fileRootFactory;
    }

    @PostMapping("/file-copy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.copy.summary", description = "projects.files.copy.desc")
    public void copyFile(@DesignRepository("repo-name") Repository repository,
                         @RequestParam(value = "branch", required = false)
                         @Parameter(description = "projects.files.param.branch.desc") String branch,
                         @RequestBody @Valid FilePathPairRequest request) {
        handleCopy(fileRootFactory.of(repository, branch), request);
    }

    @PostMapping("/file-move")
    @Operation(summary = "projects.files.move.summary", description = "projects.files.move.desc")
    public void moveFile(@DesignRepository("repo-name") Repository repository,
                         @RequestParam(value = "branch", required = false)
                         @Parameter(description = "projects.files.param.branch.desc") String branch,
                         @RequestBody @Valid FilePathPairRequest request) {
        handleMove(fileRootFactory.of(repository, branch), request);
    }

    @PostMapping("/file-search")
    @Operation(summary = "projects.files.search.summary", description = "projects.files.search.desc")
    public List<FsNode> searchFiles(@DesignRepository("repo-name") Repository repository,
                                    @RequestParam(value = "branch", required = false)
                                    @Parameter(description = "projects.files.param.branch.desc") String branch,
                                    @RequestBody FileSearchQuery query) {
        return handleSearch(fileRootFactory.of(repository, branch), query);
    }
}
