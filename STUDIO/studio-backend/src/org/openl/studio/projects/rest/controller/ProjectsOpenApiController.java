package org.openl.studio.projects.rest.controller;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.openapi.OpenApiGenerationPlanView;
import org.openl.studio.projects.model.openapi.OpenApiGenerationRequest;
import org.openl.studio.projects.model.openapi.OpenApiSchemaView;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.openapi.ProjectOpenApiGenerationService;
import org.openl.studio.projects.service.openapi.ProjectOpenApiService;

/**
 * REST controller for the OpenAPI specification of a project.
 * <p>
 * A project and its specification are held to each other in one of two directions: the specification is
 * written from the rules the project compiled, or the project's tables are generated from a specification
 * somebody wrote first. Both were the legacy Editor's, and neither had an API of its own.
 * </p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/openapi", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: OpenAPI (BETA)", description = "Experimental OpenAPI specification API")
@Validated
public class ProjectsOpenApiController {

    private final ProjectOpenApiService openApiService;
    private final ProjectOpenApiGenerationService generationService;

    @PostMapping("/schema")
    @Operation(summary = "projects.openapi.schema.write.summary", description = "projects.openapi.schema.write.desc")
    public OpenApiSchemaView writeSchema(@ProjectId @PathVariable("projectId") RulesProject project) {
        return openApiService.writeSchema(project);
    }

    @GetMapping("/generation")
    @Operation(summary = "projects.openapi.generate.plan.summary", description = "projects.openapi.generate.plan.desc")
    public OpenApiGenerationPlanView planGeneration(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "algorithmModuleName", required = false)
            @Parameter(description = "projects.openapi.generate.algorithm-name.desc") String algorithmModuleName,
            @RequestParam(value = "modelModuleName", required = false)
            @Parameter(description = "projects.openapi.generate.model-name.desc") String modelModuleName) {
        return generationService.plan(project, algorithmModuleName, modelModuleName);
    }

    @PostMapping("/generation")
    @Operation(summary = "projects.openapi.generate.run.summary", description = "projects.openapi.generate.run.desc")
    public void generateTables(@ProjectId @PathVariable("projectId") RulesProject project,
            @Valid @RequestBody OpenApiGenerationRequest request) {
        generationService.generateTables(project, request);
    }
}
