package org.openl.studio.projects.model.openapi;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What to generate a project's tables from, and where to put them.
 *
 * <p>The two modules are asked for by name and by workbook, so that a generation run again writes over the
 * same two rather than leaving a second pair beside them. A name the project already declares names the
 * module whose workbook is written over; a name it does not is a module the generation declares.
 *
 * @param path                the specification to read, relative to the project
 * @param algorithmModuleName what to call the module the rules are written into
 * @param algorithmModulePath the workbook the rules are written to, relative to the project
 * @param modelModuleName     what to call the module the data types are written into
 * @param modelModulePath     the workbook the data types are written to, relative to the project
 * @author Vladyslav Pikus
 */
public record OpenApiGenerationRequest(
        @NotBlank @Schema(description = "projects.openapi.generate.path.desc") String path,
        @NotBlank @Schema(description = "projects.openapi.generate.algorithm-name.desc") String algorithmModuleName,
        @NotBlank @Schema(description = "projects.openapi.generate.algorithm-path.desc") String algorithmModulePath,
        @NotBlank @Schema(description = "projects.openapi.generate.model-name.desc") String modelModuleName,
        @NotBlank @Schema(description = "projects.openapi.generate.model-path.desc") String modelModulePath) {
}
