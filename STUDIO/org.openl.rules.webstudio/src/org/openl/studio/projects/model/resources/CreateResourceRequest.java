package org.openl.studio.projects.model.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request model for uploading a new file to the project.
 */
public record CreateResourceRequest(

        @Schema(description = "projects.resources.param.relative-path.desc")
        @NotBlank
        String relativePath,

        @Schema(description = "projects.resources.param.file.desc")
        @NotNull
        MultipartFile file,

        @Schema(description = "projects.resources.param.create-folders.desc")
        boolean createFolders

) {
}
