package org.openl.studio.compare.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.compare.model.CompareProjectFilesRequest;
import org.openl.studio.compare.model.ComparisonSideRequest;
import org.openl.studio.compare.model.ComparisonStartedView;
import org.openl.studio.compare.service.ComparisonContent;
import org.openl.studio.compare.service.ComparisonLauncher;
import org.openl.studio.compare.service.ProjectComparisonService;
import org.openl.studio.projects.rest.annotations.ChangesNothing;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.util.IOUtils;

/**
 * REST controller for comparing two Excel files of one project.
 *
 * <p>Either file is read from the working copy or from a revision the repository holds, so a user
 * sees what a revision changed without leaving the workspace. The comparison itself is the one the
 * Compare API answers for: this controller only starts it.
 *
 * <p>With a base, both files are compared with it, as the two versions of a conflict are, so a user sees what each
 * side changed and where both changed the same cells before any merge is started.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/compare", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Compare")
public class ProjectCompareController {

    private final ProjectComparisonService projectComparisonService;
    private final ComparisonLauncher launcher;

    @Operation(summary = "compare.project.summary", description = "compare.project.desc")
    @ApiResponse(responseCode = "202", description = "compare.project.202.desc")
    @ApiResponse(responseCode = "400", description = "compare.file.not-excel.message")
    @ChangesNothing
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ComparisonStartedView compare(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "compare.project.req-body.desc")
            @Valid @RequestBody CompareProjectFilesRequest request) throws IOException {
        // A conflict is compared as the base, your version and their version
        var sides = request.base() == null
                ? List.of(request.first(), request.second())
                : List.of(request.base(), request.first(), request.second());
        return launcher.startContentOf(read(project, sides));
    }

    /** Reads the file of each side. A side that cannot be read closes the files of the sides read before it. */
    private List<ComparisonContent> read(RulesProject project, List<ComparisonSideRequest> sides) {
        var contents = new ArrayList<ComparisonContent>();
        try {
            for (var side : sides) {
                contents.add(projectComparisonService.read(project, side));
            }
        } catch (RuntimeException e) {
            contents.forEach(content -> IOUtils.closeQuietly(content.content()));
            throw e;
        }
        return contents;
    }
}
