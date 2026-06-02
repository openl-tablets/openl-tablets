package org.openl.studio.projects.model.files;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request model for updating an existing file resource.
 */
public record UpdateFileRequest(

        @Schema(description = "projects.files.param.file.desc")
        @NotNull
        MultipartFile file

) {
}
