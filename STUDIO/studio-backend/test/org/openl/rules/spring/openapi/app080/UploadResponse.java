package org.openl.rules.spring.openapi.app080;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for upload operations.
 */
@Schema(description = "Upload operation response")
public record UploadResponse(
        @Schema(description = "Unique identifier for the upload")
        String uploadId,

        @Schema(description = "Name of the uploaded resource")
        String name
) {
}
