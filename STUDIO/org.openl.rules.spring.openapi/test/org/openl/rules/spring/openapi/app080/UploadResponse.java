package org.openl.rules.spring.openapi.app080;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for upload operations.
 */
@Schema(description = "Upload operation response")
public class UploadResponse {

    @Schema(description = "Unique identifier for the upload")
    private String uploadId;

    @Schema(description = "Name of the uploaded resource")
    private String name;

    public UploadResponse() {
    }

    public UploadResponse(String uploadId, String name) {
        this.uploadId = uploadId;
        this.name = name;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
