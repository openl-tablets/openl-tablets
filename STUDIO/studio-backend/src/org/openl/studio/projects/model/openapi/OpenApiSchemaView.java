package org.openl.studio.projects.model.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The specification a project wrote for itself, and where it went.
 *
 * @param path    where in the project the specification was written
 * @param created whether the file was added, as against written over the one that stood there
 * @author Vladyslav Pikus
 */
public record OpenApiSchemaView(
        @Schema(description = "projects.openapi.schema.path.desc") String path,
        @Schema(description = "projects.openapi.schema.created.desc") boolean created) {
}
