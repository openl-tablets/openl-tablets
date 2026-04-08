package org.openl.studio.projects.model.resources;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for copying a resource to a new location within the project.
 */
public record CopyResourceRequest(

        @Schema(description = "projects.resources.copy.param.dest-path.desc")
        @NotBlank
        String destinationPath

) {
}
