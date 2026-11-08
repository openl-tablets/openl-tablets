package org.openl.studio.projects.rest.controller;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessagePositions;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;

/**
 * REST controller for the localization files of a project: the {@code message*.properties} files of its {@code i18n}
 * folder.
 *
 * <p>The files are checked without compiling the project, so their problems are told at once. What a compilation
 * finds about the calls of {@code msg} in the rules comes with the status of the project.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/localization", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Localization (BETA)", description = "Experimental localization API")
@Validated
public class ProjectLocalizationController {

    private final WorkspaceProjectService workspaceProjectService;
    private final DetailedMessageDescriptionMapper messageMapper;

    @GetMapping("/problems")
    @Operation(summary = "projects.localization.problems.summary",
            description = "projects.localization.problems.desc")
    @JsonView(GenericView.Detailed.class)
    public List<DetailedMessageDescription> getProblems(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "positions", defaultValue = "file")
            @Parameter(description = "projects.localization.problems.param.positions.desc")
            MessagePositions positions) {
        // A project nobody opened has no files in the workspace to read: it is refused, as by the other APIs.
        var descriptor = workspaceProjectService.getProjectDescriptor(project);
        var messages = workspaceProjectService.checkLocalization(descriptor);
        return messageMapper.mapFileMessages(messages, descriptor.getProjectFolder(), positions);
    }
}
