package org.openl.studio.projects.model.resources;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for moving or renaming a resource within the project.
 */
public record MoveResourceRequest(

        @Schema(description = "projects.resources.move.param.dest-path.desc")
        @NotBlank
        String destinationPath

) {
}
