package org.openl.studio.projects.model.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One of the two modules a generation writes, as it will stand once the generation has run.
 *
 * @param name       what the module is called
 * @param path       the workbook the module reads, relative to the project
 * @param declared   whether the project already reads a module under this name — declared in
 *                   {@code rules.xml} or matched by one of its patterns. Such a module is written where it
 *                   reads, so the workbook asked for it is not the caller's to choose
 * @param overwrites whether the module is read from that workbook today and the generation writes over it.
 *                   False when nothing stands there, and false when a file does but no module reads it —
 *                   such a file is not the generation's to replace, and writing a module over it is refused
 * @author Vladyslav Pikus
 */
public record OpenApiModuleView(
        @Schema(description = "projects.openapi.module.name.desc") String name,
        @Schema(description = "projects.openapi.module.path.desc") String path,
        @Schema(description = "projects.openapi.module.declared.desc") boolean declared,
        @Schema(description = "projects.openapi.module.overwrites.desc") boolean overwrites) {
}
