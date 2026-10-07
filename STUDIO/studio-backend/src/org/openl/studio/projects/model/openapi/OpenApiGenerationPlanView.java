package org.openl.studio.projects.model.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What generating tables from a specification would write, before it writes it.
 *
 * <p>The two modules are the ones the engine scaffolds: the rules it reads from the specification's
 * operations, and the data types it reads from its schemas. A module the project already declares is
 * named with the workbook it reads, which the generation writes over; a module it does not declare is
 * named with the workbook the generation would add.
 *
 * @param algorithm the module the rules are written into
 * @param model     the module the data types are written into
 * @author Vladyslav Pikus
 */
public record OpenApiGenerationPlanView(
        @Schema(description = "projects.openapi.plan.algorithm.desc") OpenApiModuleView algorithm,
        @Schema(description = "projects.openapi.plan.model.desc") OpenApiModuleView model) {
}
