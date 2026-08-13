package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import java.util.List;
import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.files.FilePathPairRequest;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.files.FileSearchQuery;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.validator.file.FileSearchQueryValidator;
import org.openl.studio.repositories.model.ProjectRevision;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.rest.resolver.PaginationDefault;

/**
 * REST controller for project file operations that involve two paths: copy and move, plus search.
 *
 * <p>These operations live outside the {@code /files/{*path}} address space, so a command name can never
 * shadow a real file whose path starts with the same segment — a project may hold a folder called
 * {@code history} of its own. Copy and move carry both paths in the request body; the history reads one
 * path from the URL, which is safe for the same reason: it is a sibling of {@code /files}, not a segment
 * inside it.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping(value = "/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Files (BETA)", description = ProjectFilesController.TAG_DESCRIPTION)
@Validated
public class ProjectFileOperationsController extends AbstractFileOperationsController {

    private final ProjectFileRootFactory fileRootFactory;
    private final ProjectRevisionService projectRevisionService;

    public ProjectFileOperationsController(ProjectFilesService filesService,
                                           ProjectFileRootFactory fileRootFactory,
                                           BeanValidationProvider validationProvider,
                                           FileSearchQueryValidator searchValidator,
                                           ProjectRevisionService projectRevisionService) {
        super(filesService, validationProvider, searchValidator);
        this.fileRootFactory = fileRootFactory;
        this.projectRevisionService = projectRevisionService;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @Override
    protected void postWrite() {
        getWebStudio().reset();
    }

    @GetMapping("/file-history/{*path}")
    @Operation(summary = "projects.files.history.summary", description = "projects.files.history.desc")
    @JsonView(UserInfoModel.View.Short.class)
    public PageResponse<ProjectRevision> getFileHistory(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @Parameter(description = "repo.param.search.desc")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "projects.files.history.param.tech-revs.desc")
            @RequestParam(value = "techRevs", required = false, defaultValue = "false") boolean techRevs,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            @PaginationDefault Pageable page) throws IOException {
        BranchGuard.requireBranch(project, branch);
        return projectRevisionService.getFileRevision(project, path, search, techRevs, page);
    }

    @PostMapping("/file-copy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.copy.summary", description = "projects.files.copy.desc")
    public void copyFile(@ProjectId @PathVariable("projectId") RulesProject project,
                         @RequestParam(value = "branch", required = false)
                         @Parameter(description = "projects.files.param.branch.desc") String branch,
                         @RequestBody @Valid FilePathPairRequest request) {
        BranchGuard.requireBranch(project, branch);
        handleCopy(fileRootFactory.of(project), request);
    }

    @PostMapping("/file-move")
    @Operation(summary = "projects.files.move.summary", description = "projects.files.move.desc")
    public void moveFile(@ProjectId @PathVariable("projectId") RulesProject project,
                         @RequestParam(value = "branch", required = false)
                         @Parameter(description = "projects.files.param.branch.desc") String branch,
                         @RequestBody @Valid FilePathPairRequest request) {
        BranchGuard.requireBranch(project, branch);
        handleMove(fileRootFactory.of(project), request);
    }

    @PostMapping("/file-search")
    @Operation(summary = "projects.files.search.summary", description = "projects.files.search.desc")
    public List<FsNode> searchFiles(@ProjectId @PathVariable("projectId") RulesProject project,
                                    @RequestParam(value = "branch", required = false)
                                    @Parameter(description = "projects.files.param.branch.desc") String branch,
                                    @RequestBody FileSearchQuery query) {
        BranchGuard.requireBranch(project, branch);
        return handleSearch(fileRootFactory.of(project), query);
    }
}
