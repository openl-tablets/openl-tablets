package org.openl.studio.compare.rest;

import java.io.IOException;
import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.compare.model.CompareProjectFilesRequest;
import org.openl.studio.compare.model.ComparisonStartedView;
import org.openl.studio.compare.service.ComparisonContent;
import org.openl.studio.compare.service.ComparisonLauncher;
import org.openl.studio.compare.service.ProjectComparisonService;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.util.IOUtils;

/**
 * REST controller for comparing two Excel files of one project.
 *
 * <p>Either file is read from the working copy or from a revision the repository holds, so a user
 * sees what a revision changed without leaving the workspace. The comparison itself is the one the
 * Compare API answers for: this controller only says what there is to compare and starts it.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/compare", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Compare")
public class ProjectCompareController {

    private final ProjectComparisonService projectComparisonService;
    private final ComparisonLauncher launcher;

    @Operation(summary = "compare.project-files.summary", description = "compare.project-files.desc")
    @ApiResponse(responseCode = "200", description = "compare.project-files.200.desc",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String.class)))
    @GetMapping("/files")
    public List<String> getFiles(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "compare.param.branch.desc")
            @RequestParam(value = "branch", required = false) @Nullable String branch,
            @Parameter(description = "compare.param.revision.desc")
            @RequestParam(value = "revision", required = false) @Nullable String revision) {
        return projectComparisonService.excelFiles(project, branch, revision);
    }

    @Operation(summary = "compare.project.summary", description = "compare.project.desc")
    @ApiResponse(responseCode = "202", description = "compare.project.202.desc")
    @ApiResponse(responseCode = "400", description = "compare.file.not-excel.message")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ComparisonStartedView compare(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "compare.project.req-body.desc")
            @Valid @RequestBody CompareProjectFilesRequest request) throws IOException {
        var first = projectComparisonService.read(project, request.first());
        ComparisonContent second;
        try {
            second = projectComparisonService.read(project, request.second());
        } catch (RuntimeException e) {
            // The file of the first side is open by now; a second side that cannot be read closes it.
            IOUtils.closeQuietly(first.content());
            throw e;
        }
        return launcher.startContentOf(List.of(first, second));
    }
}
