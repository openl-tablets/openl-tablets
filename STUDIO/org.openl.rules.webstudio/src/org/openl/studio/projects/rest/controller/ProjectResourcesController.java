package org.openl.studio.projects.rest.controller;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.resources.ProjectResourcesService;
import org.openl.studio.projects.service.resources.ResourceCriteriaQuery;
import org.openl.studio.projects.service.resources.ResourceViewMode;

/**
 * REST controller for project resources (files and folders).
 *
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Resources", description = "APIs for managing project resources")
@Validated
public class ProjectResourcesController {

    private final ProjectResourcesService resourcesService;

    public ProjectResourcesController(ProjectResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    @GetMapping
    @Operation(
            summary = "Get project resources",
            description = "Retrieves files and folders from the project. " +
                    "Use 'viewMode=FLAT' for a flat list or 'viewMode=NESTED' for a tree structure. " +
                    "Set 'recursive=true' to include nested resources."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Resources retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = Resource.class))
            )
    )
    @ApiResponse(responseCode = "404", description = "Project or base path not found")
    public List<Resource> getResources(
            @ProjectId
            @PathVariable("projectId")
            RulesProject project,

            @RequestParam(value = "basePath", required = false)
            @Parameter(description = "Base path to start listing from. If not specified, starts from project root.")
            String basePath,

            @RequestParam(value = "extensions", required = false)
            @Parameter(description = "Filter by file extensions (without dot). Example: xlsx,xml")
            Set<String> extensions,

            @RequestParam(value = "namePattern", required = false)
            @Parameter(description = "Filter by name pattern (case-insensitive contains match)")
            String namePattern,

            @RequestParam(value = "recursive", defaultValue = "false")
            @Parameter(description = "Whether to include nested resources recursively")
            boolean recursive,

            @RequestParam(value = "viewMode", defaultValue = "FLAT")
            @Parameter(description = "View mode: FLAT returns a flat list, NESTED returns a tree structure")
            ResourceViewMode viewMode
    ) {
        var query = ResourceCriteriaQuery.builder()
                .basePath(basePath)
                .extensions(extensions)
                .namePattern(namePattern)
                .build();

        return resourcesService.getResources(project, query, recursive, viewMode);
    }
}
