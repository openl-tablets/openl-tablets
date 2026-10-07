package org.openl.studio.projects.model.files;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model carrying a source and destination path within a mount. Shared by the copy and move
 * operations.
 *
 * @author Yury Molchan
 */
public record FilePathPairRequest(

        @Schema(description = "projects.files.path-pair.source-path.desc")
        @NotBlank
        String sourcePath,

        @Schema(description = "projects.files.path-pair.dest-path.desc")
        @NotBlank
        String destinationPath

) {
}
