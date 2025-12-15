package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import java.util.Optional;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.merge.CheckMergeResult;
import org.openl.studio.projects.model.merge.CheckMergeStatus;
import org.openl.studio.projects.model.merge.MergeRequest;
import org.openl.studio.projects.model.merge.MergeResult;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.merge.ProjectsMergeService;

@Validated
@RestController
@RequestMapping(value = "/projects/{projectId}/merge", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Merge (BETA)", description = "Experimental projects merge API")
public class ProjectsMergeController {

    private final ProjectsMergeService projectsMergeService;
    private final WorkspaceProjectService projectService;

    public ProjectsMergeController(ProjectsMergeService projectsMergeService,
                                   WorkspaceProjectService projectService) {
        this.projectsMergeService = projectsMergeService;
        this.projectService = projectService;
    }

    @PostMapping("/check")
    public CheckMergeResult check(@ProjectId @PathVariable("projectId") RulesProject project,
                                  @RequestBody @Valid MergeRequest request) throws IOException {
        return projectsMergeService.checkMerge(project, request.otherBranch(), request.mode());
    }

    @PostMapping
    public MergeResult merge(@ProjectId @PathVariable("projectId") RulesProject project,
                             @RequestBody @Valid MergeRequest request) throws IOException, ProjectException {
        var checkMergeResult = projectsMergeService.checkMerge(project, request.otherBranch(), request.mode());
        if (checkMergeResult.status() != CheckMergeStatus.MERGEABLE) {
            throw new ConflictException("prject.branch.merge.not.mergeable");
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
            var mergeRsult = projectsMergeService.merge(project, request.otherBranch(), request.mode());
            if (mergeRsult.status() == MergeResult.Status.SUCCESS) {
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
            }
            return mergeRsult;
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

}
