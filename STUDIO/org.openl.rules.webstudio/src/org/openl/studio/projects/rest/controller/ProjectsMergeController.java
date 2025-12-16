package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.webstudio.util.WebTool;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.merge.CheckMergeResult;
import org.openl.studio.projects.model.merge.CheckMergeStatus;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictGroup;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.model.merge.MergeRequest;
import org.openl.studio.projects.model.merge.MergeResultResponse;
import org.openl.studio.projects.model.merge.MergeResultStatus;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsService;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.studio.projects.service.merge.ProjectsMergeService;

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

    @PostMapping("/check")
    public CheckMergeResult check(@ProjectId @PathVariable("projectId") RulesProject project,
                                  @RequestBody @Valid MergeRequest request) throws IOException {
        validateUnresolvedConflict(project);
        return mergeService.checkMerge(project, request.otherBranch(), request.mode());
    }

    @GetMapping("/conflicts")
    public List<ConflictGroup> getMergeConflictInfo(@ProjectId @PathVariable("projectId") RulesProject project) {
        var conflictInfo = getMergeConflictInfo0(project);
        return mergeConflictsService.getMergeConflicts(conflictInfo);
    }

    @GetMapping("/conflicts/files")
    public ResponseEntity<byte[]> getConflictedFile(@ProjectId @PathVariable("projectId") RulesProject project,
                                  @RequestParam("file") String filePath,
                                  @RequestParam("side") @NotNull ConflictBase side) throws IOException {
        var conflictInfo = getMergeConflictInfo0(project);
        var fileItem = mergeConflictsService.getConflictFileItem(conflictInfo, filePath, side);
        var output = new ByteArrayOutputStream();
        try (var stream = fileItem.getStream()) {
            stream.transferTo(output);
        }
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
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

    @PostMapping
    public MergeResultResponse merge(@ProjectId @PathVariable("projectId") RulesProject project,
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
                    mergeConflictsService.getMergeConflicts(mergeRsult.conflictInfo())
            );
        } catch (ProjectException e) {
            shouldResumeDependencies = true;
            throw e;
        } finally {
            if (shouldResumeDependencies && dependencyManager != null) {
                dependencyManager.resume();
            }
            studio.releaseProject(nameBeforeMerge);
        }
    }

    private void validateUnresolvedConflict(RulesProject project) {
        var projectId = projectService.resolveProjectId(project);
        if (conflictsSessionHolder.hasConflictInfo(projectId)) {
            throw new ConflictException("project.unresolved.merge.conflicts.message");
        }
    }

}
