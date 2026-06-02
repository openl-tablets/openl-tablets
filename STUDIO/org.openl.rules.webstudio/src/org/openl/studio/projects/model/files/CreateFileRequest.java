package org.openl.studio.projects.model.files;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request model for uploading a new file to the project.
 */
public record CreateFileRequest(

        @Schema(description = "projects.files.param.relative-path.desc")
        @NotBlank
        String relativePath,

        @Schema(description = "projects.files.param.file.desc")
        @NotNull
        MultipartFile file,

        @Schema(description = "projects.files.param.create-folders.desc")
        boolean createFolders

) {
}
