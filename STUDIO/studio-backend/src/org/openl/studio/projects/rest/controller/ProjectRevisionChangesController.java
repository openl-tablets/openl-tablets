package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.studio.common.projection.FieldProjectionSupport;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.repositories.model.ProjectRevisionChanges;
import org.openl.studio.repositories.service.ProjectRevisionChangesService;
import org.openl.util.StringUtils;

/**
 * REST controller for the files the revisions of a project changed.
 *
 * <p>Whoever may read the history of a project may read what its revisions changed: the project arrives resolved for
 * a reader. The files that may conflict are estimated only for an answer that keeps them: the estimate reads the
 * workbooks both sides changed.
 */
@RestController
@RequestMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects (BETA)", description = "Experimental projects API")
@RequiredArgsConstructor
public class ProjectRevisionChangesController {

    private final ProjectRevisionChangesService projectRevisionChangesService;
    private final FieldProjectionSupport fieldProjection;

    @GetMapping("/{projectId}/history/changes")
    @Operation(summary = "projects.history.changes.summary", description = "projects.history.changes.desc")
    @ApiResponse(responseCode = "200", description = "projects.history.changes.200.desc")
    @ApiResponse(responseCode = "404", description = "projects.history.changes.404.desc")
    // The revisions are told as the history tells them, their authors included.
    @JsonView(UserInfoModel.View.Short.class)
    public ProjectRevisionChanges getRevisionChanges(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.history.changes.param.revision.desc")
            @RequestParam(value = "revision", required = false) @Nullable String revision,
            @Parameter(description = "projects.history.changes.param.base.desc")
            @RequestParam(value = "base", required = false) @Nullable String base,
            HttpServletRequest request) throws IOException {
        return projectRevisionChangesService.changesOf(project,
                StringUtils.trimToNull(revision),
                StringUtils.trimToNull(base),
                fieldProjection.selectionOf(request).selects("conflicts"));
    }
}
