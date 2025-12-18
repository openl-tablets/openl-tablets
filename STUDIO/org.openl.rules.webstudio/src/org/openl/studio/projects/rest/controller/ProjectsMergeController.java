package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.projects.model.merge.CheckMergeResult;
import org.openl.studio.projects.model.merge.CheckMergeStatus;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.ConflictResolutionStatus;
import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;
import org.openl.studio.projects.model.merge.FileConflictResolution;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeRequest;
import org.openl.studio.projects.model.merge.MergeResultResponse;
import org.openl.studio.projects.model.merge.MergeResultStatus;
import org.openl.studio.projects.model.merge.ResolveConflictsResponse;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.rest.model.ResolveConflictsRequest;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.merge.ProjectsMergeService;
import org.openl.util.FileUtils;

@Validated
@RestController
@RequestMapping(value = "/projects/{projectId}/merge", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Merge (BETA)", description = "Experimental projects merge API")
public class ProjectsMergeController {

    private final ProjectsMergeService mergeService;
    private final WorkspaceProjectService projectService;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private final ProjectsMergeConflictsService mergeConflictsService;

    public ProjectsMergeController(ProjectsMergeService mergeService,
                                   WorkspaceProjectService projectService,
                                   ProjectsMergeConflictsSessionHolder conflictsSessionHolder,
                                   ProjectsMergeConflictsService mergeConflictsService) {
        this.mergeService = mergeService;
        this.projectService = projectService;
        this.conflictsSessionHolder = conflictsSessionHolder;
        this.mergeConflictsService = mergeConflictsService;
    }

    @Operation(summary = "projects.merge.check.summary", description = "projects.merge.check.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.check.200.desc")
    @PostMapping("/check")
    public CheckMergeResult check(@ProjectId @PathVariable("projectId") RulesProject project,
                                  @Parameter(description = "projects.merge.check.request.desc")
                                  @RequestBody @Valid MergeRequest request) throws IOException {
        validateUnresolvedConflict(project);
        return mergeService.checkMerge(project, request.otherBranch(), request.mode());
    }

    @Operation(summary = "projects.merge.get-conflicts.summary", description = "projects.merge.get-conflicts.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.get-conflicts.200.desc")
    @GetMapping("/conflicts")
    public List<ConflictGroup> getMergeConflictInfo(@ProjectId @PathVariable("projectId") RulesProject project) {
        var conflictInfo = getMergeConflictInfo0(project);
        return mergeConflictsService.getMergeConflicts(conflictInfo);
    }

    @Operation(summary = "projects.merge.get-conflict-file.summary", description = "projects.merge.get-conflict-file.desc")
    @ApiResponse(responseCode = "200",
            description = "projects.merge.get-conflict-file.200.desc",
            content = @Content(mediaType = MediaType.ALL_VALUE, schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/conflicts/files", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getConflictedFile(@ProjectId @PathVariable("projectId") RulesProject project,
                                                    @Parameter(description = "projects.merge.param.file.desc") @RequestParam("file") String filePath,
                                                    @Parameter(description = "projects.merge.param.side.desc") @RequestParam("side") @NotNull ConflictBase side) throws IOException {
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

    private MergeConflictInfo getMergeConflictInfo0(RulesProject project) {
        var projectId = projectService.resolveProjectId(project);
        if (!conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new NotFoundException("project.merge.result.not.found.message");
        }
        return conflictsSessionHolder.getConflictInfo(projectId);
    }

    @Operation(summary = "projects.merge.merge.summary", description = "projects.merge.merge.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.merge.200.desc")
    @PostMapping
    public MergeResultResponse merge(@ProjectId @PathVariable("projectId") RulesProject project,
                                     @Parameter(description = "projects.merge.merge.request.desc")
                                     @RequestBody @Valid MergeRequest request) throws IOException, ProjectException {
        validateUnresolvedConflict(project);
        var checkMergeResult = mergeService.checkMerge(project, request.otherBranch(), request.mode());
        if (checkMergeResult.status() != CheckMergeStatus.MERGEABLE) {
            throw new ConflictException("project.branch.merge.not.mergeable.message");
        }
        var model = projectService.getProjectModel(project);
        var dependencyManager = model.getWebStudioWorkspaceDependencyManager();
        if (dependencyManager != null) {
            dependencyManager.pause();
        }
        var studio = projectService.getWebStudio();
        String nameBeforeMerge = project.getName();
        String nameAfterMerge = nameBeforeMerge;
        String realPath = project.getRealPath();
        String currentBranch = project.getBranch();
        boolean wasOpened = project.isOpened();
        var repoId = project.getDesignRepository().getId();
        boolean shouldResumeDependencies = false;
        try {
            studio.freezeProject(nameBeforeMerge);
            var mergeRsult = mergeService.merge(project, request.otherBranch(), request.mode());
            if (mergeRsult.status() == MergeResultStatus.SUCCESS) {
                var workspace = projectService.getUserWorkspace();
                if (wasOpened) {
                    if (project.isDeleted()) {
                        project.close();
                    } else {
                        // Project can be renamed after merge, so we close it before opening to ensure that
                        // project folder name in editor is up to date.
                        project.close();

                        Optional<RulesProject> refreshedProject = workspace.getProjectByPath(repoId, realPath);
                        if (refreshedProject.isPresent()) {
                            RulesProject mergedProject = refreshedProject.get();
                            mergedProject.setBranch(currentBranch);
                            mergedProject.open();
                            nameAfterMerge = mergedProject.getName();
                        }
                    }
                }
                workspace.refresh();
                studio.reset();
                model.clearModuleInfo();
                if (!nameAfterMerge.equals(nameBeforeMerge)) {
                    studio.init(repoId, currentBranch, nameAfterMerge, null);
                }
            } else {
                shouldResumeDependencies = true;
                var projectId = projectService.resolveProjectId(project);
                conflictsSessionHolder.store(projectId, mergeRsult.conflictInfo());
            }
            return new MergeResultResponse(
                    mergeRsult.status(),
                    Optional.ofNullable(mergeRsult.conflictInfo())
                            .map(mergeConflictsService::getMergeConflicts)
                            .orElseGet(List::of)
            );
        } catch (ProjectException | IOException e) {
            shouldResumeDependencies = true;
            throw e;
        } finally {
            if (shouldResumeDependencies && dependencyManager != null) {
                dependencyManager.resume();
            }
            studio.releaseProject(nameBeforeMerge);
        }
    }

    @PostMapping(value = "/conflicts/resolve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "projects.merge.resolve-conflicts.summary", description = "projects.merge.resolve-conflicts.desc")
    @ApiResponse(responseCode = "200", description = "projects.merge.resolve-conflicts.200.desc")
    public ResolveConflictsResponse resolveConflicts(@ProjectId @PathVariable("projectId") RulesProject project,
                                                     @Parameter(description = "projects.merge.param.files.desc")
                                                     @ModelAttribute @NotNull @Valid ResolveConflictsRequest request) throws IOException, ProjectException {

        // Validate that the project has unresolved conflicts
        var mergeConflictInfo = getMergeConflictInfo0(project);

        List<FileConflictResolution> resolutions = new ArrayList<>();
        Map<String, InputStreamSource> customFiles = new HashMap<>();
        request.resolutions()
                .forEach(resolution -> {
                    resolutions.add(new FileConflictResolution(resolution.filePath(), resolution.strategy()));
                    if (resolution.strategy() == ConflictResolutionStrategy.CUSTOM) {
                        if (resolution.file() == null || resolution.file().isEmpty()) {
                            throw new BadRequestException("project.merge.conflict.custom.file.missing.message", new Object[]{resolution.filePath()});
                        }
                        customFiles.put(resolution.filePath(), resolution.file());
                    }
                });

        var mergeOperation = mergeConflictInfo.isMerging();
        var model = projectService.getProjectModel(project);
        var dependencyManager = model.getWebStudioWorkspaceDependencyManager();
        boolean wasOpened = project.isOpened();
        if (dependencyManager != null) {
            dependencyManager.pause();
        }
        var studio = projectService.getWebStudio();
        boolean shouldResumeDependencies = false;
        // Delegate to service for resolution
        try {
            if (!mergeOperation) {
                studio.freezeProject(project.getName());
            }
            var result = mergeConflictsService.resolveConflicts(mergeConflictInfo, resolutions, customFiles, request.message());
            if (result.status() == ConflictResolutionStatus.SUCCESS) {
                // Clear conflict info from session if resolved successfully
                var projectId = projectService.resolveProjectId(project);
                conflictsSessionHolder.remove(projectId);
                var workspace = projectService.getUserWorkspace();
                project = workspace.getProject(project.getRepository().getId(), project.getName());
                if (wasOpened) {
                    if (project.isDeleted()) {
                        project.close();
                    } else {
                        project.open();
                    }
                }
                workspace.refresh();
                studio.reset();
                model.clearModuleInfo();
            } else {
                shouldResumeDependencies = true;
            }
            return result;
        } catch (ProjectException | IOException e) {
            shouldResumeDependencies = true;
            throw e;
        } finally {
            if (shouldResumeDependencies && dependencyManager != null) {
                dependencyManager.resume();
            }
            if (!mergeOperation) {
                studio.releaseProject(project.getName());
            }
        }
    }

    @DeleteMapping("/conflicts")
    @Operation(summary = "projects.merge.cancel-conflicts.summary", description = "projects.merge.cancel-conflicts.desc")
    @ApiResponse(responseCode = "204", description = "projects.merge.cancel-conflicts.204.desc")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelMergeConflicts(@ProjectId @PathVariable("projectId") RulesProject project) {
        var projectId = projectService.resolveProjectId(project);
        if (!conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new NotFoundException("project.merge.result.not.found.message");
        }

        conflictsSessionHolder.remove(projectId);
    }

    private void validateUnresolvedConflict(RulesProject project) {
        var projectId = projectService.resolveProjectId(project);
        if (conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new ConflictException("project.unresolved.merge.conflicts.message");
        }
    }

}
