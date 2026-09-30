package org.openl.rules.spring.openapi.app080;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Test model for @ModelAttribute parsing with various field types and annotations.
 */
public record UploadRequest(
        @Schema(description = "Name of the upload", example = "my-upload", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,

        @Schema(description = "Description of the upload")
        @Size(min = 10, max = 500)
        String description,

        @Schema(description = "Priority level", example = "5")
        @Min(1)
        @Max(10)
        Integer priority,

        @Schema(description = "Email address for notifications", example = "user@example.com")
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$")
        String email,

        @Schema(description = "List of tags associated with the upload")
        @NotNull
        @Size(min = 1)
        List<String> tags,

        @Schema(description = "Uploaded file for processing")
        MultipartFile file,

        @Schema(description = "Additional metadata as key-value pairs")
        List<MetadataEntry> metadata
) {
}
