package org.openl.studio.projects.model.files;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for copying a resource to a new location within the project.
 */
public record CopyFileRequest(

        @Schema(description = "projects.files.copy.param.source-path.desc")
        @NotBlank
        String sourcePath,

        @Schema(description = "projects.files.copy.param.dest-path.desc")
        @NotBlank
        String destinationPath

) {
}
