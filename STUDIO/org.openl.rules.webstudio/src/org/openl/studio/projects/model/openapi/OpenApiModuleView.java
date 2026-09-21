package org.openl.studio.projects.model.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One of the two modules a generation writes, as it will stand once the generation has run.
 *
 * @param name     what the module is called
 * @param path     the workbook the module reads, relative to the project
 * @param declared whether the project already reads a module under this name — declared in {@code rules.xml}
 *                 or matched by one of its patterns — whose workbook the generation writes over
 * @author Vladyslav Pikus
 */
public record OpenApiModuleView(
        @Schema(description = "projects.openapi.module.name.desc") String name,
        @Schema(description = "projects.openapi.module.path.desc") String path,
        @Schema(description = "projects.openapi.module.declared.desc") boolean declared) {
}
