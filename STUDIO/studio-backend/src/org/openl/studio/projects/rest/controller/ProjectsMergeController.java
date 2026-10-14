package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.ResultNotReadyView;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.compare.model.ComparisonStartedView;
import org.openl.studio.compare.service.ComparisonContent;
import org.openl.studio.compare.service.ComparisonLauncher;
import org.openl.studio.projects.model.merge.CheckMergeResult;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictDetailsResponse;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.ConflictTextView;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeRequest;
import org.openl.studio.projects.model.merge.MergeResultResponse;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.rest.model.ResolveConflictsRequest;
import org.openl.studio.projects.service.ProjectIdentifierMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.MergeChangesService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.merge.ProjectsMergeLauncher;
import org.openl.studio.projects.service.merge.ProjectsMergeService;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/projects/{projectId}/merge", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Merge (BETA)", description = "Experimental projects merge API")
public class ProjectsMergeController {

    /**
     * The most resolutions one request binds. The resolutions are bound by their indexes, and Spring stops growing
     * such a list at 256 elements, which a merge of a large project exceeds. The servers OpenL ships take at most
     * 10 000 form keys, two or three for each resolution, so a request never holds more.
     */
    private static final int MAX_RESOLUTIONS = 10_000;

    private final ProjectsMergeService mergeService;
    private final WorkspaceProjectService projectService;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private final ProjectsMergeConflictsService mergeConflictsService;
    private final ProjectIdentifierMapper projectIdentifierMapper;
    private final ComparisonLauncher comparisonLauncher;
    private final ProjectsMergeLauncher mergeLauncher;
    private final MergeChangesService mergeChangesService;

    @InitBinder
    void allowLongResolutionLists(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(MAX_RESOLUTIONS);
    }

    @Operation(summary = "projects.merge.check.summary", description = "projects.merge.check.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.check.200.desc")
    @PostMapping("/check")
    public CheckMergeResult check(@ProjectId @PathVariable("projectId") RulesProject project,
                                  @Parameter(description = "projects.merge.check.request.desc")
                                  @RequestBody @Valid MergeRequest request) throws IOException {
        conflictsSessionHolder.requireResolved(projectIdentifierMapper.map(project));
        return mergeService.checkMerge(project, request.otherBranch(), request.mode());
    }

    @Operation(summary = "projects.merge.get-conflicts.summary", description = "projects.merge.get-conflicts.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.get-conflicts.200.desc")
    @GetMapping("/conflicts")
    public ConflictDetailsResponse getMergeConflictInfo(@ProjectId @PathVariable("projectId") RulesProject project)
            throws IOException {
        var conflictInfo = getMergeConflictInfo0(project);
        return mergeConflictsService.getConflictDetails(conflictInfo);
    }

    @Operation(summary = "projects.merge.get-conflict-file.summary", description = "projects.merge.get-conflict-file.desc")
    @ApiResponse(responseCode = "200",
            description = "projects.merge.get-conflict-file.200.desc",
            content = @Content(mediaType = MediaType.ALL_VALUE, schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/conflicts/files", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getConflictedFile(@ProjectId @PathVariable("projectId") RulesProject project,
                                                    @Parameter(description = "projects.merge.param.file.desc", required = true) @RequestParam("file") String filePath,
                                                    @Parameter(description = "projects.merge.param.side.desc", required = true) @RequestParam("side") @NotNull ConflictBase side) throws IOException {
        var conflictInfo = getMergeConflictInfo0(project);
        var fileItem = mergeConflictsService.getConflictFileItem(conflictInfo, filePath, side);
        var output = new ByteArrayOutputStream();
        try (var stream = fileItem.getStream()) {
            stream.transferTo(output);
        }
        String fileName = FileUtils.getName(filePath);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(fileName))
                .body(output.toByteArray());
    }

    @Operation(summary = "projects.merge.get-conflict-text.summary",
            description = "projects.merge.get-conflict-text.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.get-conflict-text.200.desc")
    @ApiResponse(responseCode = "400", description = "projects.merge.get-conflict-text.400.desc")
    @GetMapping("/conflicts/text")
    public ConflictTextView getConflictText(@ProjectId @PathVariable("projectId") RulesProject project,
                                            @Parameter(description = "projects.merge.param.file.desc", required = true)
                                            @RequestParam("file") String filePath) throws IOException {
        return mergeConflictsService.getConflictText(getMergeConflictInfo0(project), filePath);
    }

    @Operation(summary = "projects.merge.compare-conflict.summary",
            description = "projects.merge.compare-conflict.desc")
    @ApiResponse(responseCode = "202", description = "projects.merge.compare-conflict.202.desc")
    @ApiResponse(responseCode = "400", description = "compare.file.not-excel.message")
    @PostMapping("/conflicts/compare")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ComparisonStartedView compareConflictedFile(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.merge.param.file.desc", required = true)
            @RequestParam("file") String filePath,
            @Parameter(description = "projects.merge.compare-conflict.param.first.desc")
            @RequestParam(name = "first", defaultValue = "theirs") ConflictBase first,
            @Parameter(description = "projects.merge.compare-conflict.param.second.desc")
            @RequestParam(name = "second", defaultValue = "ours") ConflictBase second) throws IOException {
        var fileName = FileUtils.getName(filePath);
        if (!FileTypeHelper.isExcelFile(fileName)) {
            // A file of any other format is compared line by line, which the screen does itself.
            throw new BadRequestException("compare.file.not-excel.message");
        }
        var conflictInfo = getMergeConflictInfo0(project);
        var firstVersion = mergeConflictsService.getConflictFileItem(conflictInfo, filePath, first);
        try {
            var secondVersion = mergeConflictsService.getConflictFileItem(conflictInfo, filePath, second);
            return comparisonLauncher.startContentOf(List.of(
                    new ComparisonContent(fileName, firstVersion.getStream()),
                    new ComparisonContent(fileName, secondVersion.getStream())));
        } catch (Exception e) {
            // The first version is open by now; a second one that cannot be read closes it.
            IOUtils.closeQuietly(firstVersion.getStream());
            throw e;
        }
    }

    private MergeConflictInfo getMergeConflictInfo0(RulesProject project) {
        var projectId = projectIdentifierMapper.map(project);
        if (!conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new NotFoundException("project.merge.result.not.found.message");
        }
        return conflictsSessionHolder.getConflictInfo(projectId);
    }

    @Operation(summary = "projects.merge.merge.summary", description = "projects.merge.merge.desc")
    @ApiResponse(responseCode = "202", description = "projects.merge.merge.202.desc")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void merge(@ProjectId @PathVariable("projectId") RulesProject project,
                      @Parameter(description = "projects.merge.merge.request.desc")
                      @RequestBody @Valid MergeRequest request,
                      @Parameter(description = "projects.merge.param.force.desc")
                      @RequestParam(name = "force", defaultValue = "false") boolean force) throws IOException {
        mergeLauncher.start(project, request.otherBranch(), request.mode(), force);
    }

    @Operation(summary = "projects.merge.get-result.summary", description = "projects.merge.get-result.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.get-result.200.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MergeResultResponse.class)))
    @ApiResponse(responseCode = "202", description = "projects.merge.get-result.202.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ResultNotReadyView.class)))
    @GetMapping
    // The endpoint answers with another body while the merge is running.
    @SuppressWarnings("java:S1452")
    public ResponseEntity<?> getMergeResult(@ProjectId @PathVariable("projectId") RulesProject project) {
        return mergeLauncher.result(project)
                .<ResponseEntity<?>>map(result -> ResponseEntity.ok(new MergeResultResponse(result.status(),
                        Optional.ofNullable(result.conflictInfo())
                                .map(mergeConflictsService::getMergeConflicts)
                                .orElseGet(List::of),
                        Optional.ofNullable(result.changes())
                                .map(changes -> mergeChangesService.summaryOf(project, changes))
                                .orElse(null))))
                .orElseGet(ResultNotReadyView::accepted);
    }

    @PostMapping(value = "/conflicts/resolve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "projects.merge.resolve-conflicts.summary", description = "projects.merge.resolve-conflicts.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.resolve-conflicts.200.desc")
    public ResolveConflictsResponse resolveConflicts(@ProjectId @PathVariable("projectId") RulesProject project,
                                                     @Parameter(description = "projects.merge.param.resolution-request.desc")
                                                     @ModelAttribute @NotNull @Valid ResolveConflictsRequest request) throws IOException, ProjectException {

        // Validate that the project has unresolved conflicts
        var mergeConflictInfo = getMergeConflictInfo0(project);

        var resolutions = new ArrayList<FileConflictResolution>();
        var customFiles = new HashMap<String, InputStreamSource>();
        request.resolutions()
                .forEach(resolution -> {
                    resolutions.add(new FileConflictResolution(resolution.filePath(), resolution.strategy()));
                    if (resolution.strategy() == ConflictResolutionStrategy.CUSTOM) {
                        var file = requireCustomFile(resolution.filePath(), resolution.file());
                        customFiles.put(resolution.filePath(), file);
                    }
                });

        var mergeOperation = mergeConflictInfo.isMerging();
        var wasOpened = project.isOpened();
        var model = editorModelOf(project);
        var dependencyManager = model == null ? null : model.getWebStudioWorkspaceDependencyManager();
        var repositoryId = project.getDesignRepository().getId();
        var realPath = project.getRealPath();
        var currentBranch = project.getBranch();
        if (dependencyManager != null) {
            dependencyManager.pause();
        }
        var studio = projectService.getWebStudio();
        // Let go of the compilation at the end of the request, whatever ends it: a resolution the service
        // refuses throws, and a compilation left paused holds up every later read of the project.
        var shouldResumeDependencies = true;
        // Delegate to service for resolution
        try {
            var result = mergeConflictsService.resolveConflicts(mergeConflictInfo, resolutions, customFiles, request.message());
            if (result.status() == ConflictResolutionStatus.SUCCESS) {
                // Clear conflict info from session if resolved successfully
                var projectId = projectIdentifierMapper.map(project);
                conflictsSessionHolder.remove(projectId);
                mergeLauncher.forget(projectId);
                refreshResolvedProject(project, wasOpened, mergeOperation, repositoryId, realPath, currentBranch);
                studio.reset();
                if (model != null) {
                    model.clearModuleInfo();
                }
                shouldResumeDependencies = false;
            }
            return result;
        } finally {
            if (shouldResumeDependencies && dependencyManager != null) {
                dependencyManager.resume();
            }
        }
    }

    private static MultipartFile requireCustomFile(String filePath, @Nullable MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("project.merge.conflict.custom.file.missing.message", new Object[]{filePath});
        }
        return file;
    }

    /**
     * Reads the project, whose conflicts are resolved, from the repository again, and opens it again when it was
     * opened, unless the resolution deleted it.
     */
    private void refreshResolvedProject(RulesProject project,
                                        boolean wasOpened,
                                        boolean mergeOperation,
                                        String repositoryId,
                                        String realPath,
                                        @Nullable String currentBranch) throws ProjectException {
        var workspace = projectService.getUserWorkspace();
        if (wasOpened && mergeOperation) {
            project.close();
        }
        workspace.refresh();
        var refreshedProject = workspace.getProjectByPath(repositoryId, realPath);
        var resolvedProject = refreshedProject.isPresent()
                ? refreshedProject.orElseThrow()
                : workspace.getProject(repositoryId, project.getName());
        if (wasOpened) {
            if (mergeOperation && currentBranch != null) {
                workspace.setProjectBranch(resolvedProject, currentBranch);
            }
            if (resolvedProject.isDeleted()) {
                resolvedProject.close();
            } else {
                resolvedProject.open();
            }
        }
    }

    @DeleteMapping("/conflicts")
    @Operation(summary = "projects.merge.cancel-conflicts.summary", description = "projects.merge.cancel-conflicts.desc")
    @ApiResponse(responseCode = "202", description = "projects.merge.cancel-conflicts.202.desc")
    @ApiResponse(responseCode = "204", description = "projects.merge.cancel-conflicts.204.desc")
    public ResponseEntity<Void> cancelMergeConflicts(@ProjectId @PathVariable("projectId") RulesProject project) {
        if (mergeLauncher.cancel(project)) {
            // The merge stops before its next step and reports its end on the status topic.
            return ResponseEntity.accepted().build();
        }
        // A merge that ended a moment ago keeps its conflicts only once it reaches the editor: they are what is
        // cancelled.
        mergeLauncher.settleFinished();
        var projectId = projectIdentifierMapper.map(project);
        if (!conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new NotFoundException("project.merge.result.not.found.message");
        }

        conflictsSessionHolder.remove(projectId);
        mergeLauncher.forget(projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * The editor model a merge has to keep consistent, or {@code null} when the project has none.
     *
     * <p>A merge is performed in the repository, so a closed project is merged just as well as an opened one.
     * Only an opened project carries editor state — a compiled model, a dependency manager to pause, modules
     * on screen — and only it is reopened afterwards.
     *
     * <p>Opening the project here instead would compile a project nobody is looking at, and it used to fail
     * outright: the merge answered "the project is not opened" and refused — see EPBDS-16420.
     */
    private @Nullable ProjectModel editorModelOf(RulesProject project) {
        return project.isOpened() ? projectService.openProject(project).awaitCompiled() : null;
    }

}
