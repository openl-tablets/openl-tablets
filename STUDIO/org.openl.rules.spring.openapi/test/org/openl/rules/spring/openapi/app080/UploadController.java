package org.openl.rules.spring.openapi.app080;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller to verify @ModelAttribute parsing in OpenAPI generation.
 */
@RestController
@RequestMapping("/uploads")
@Tag(name = "Uploads", description = "File upload operations with model attributes")
public class UploadController {

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create upload with model attribute",
            description = "Creates a new upload using @ModelAttribute to bind multipart form data to a Java object. " +
                    "All fields from the UploadRequest model should be expanded as form parameters.")
    @ApiResponse(responseCode = "200", description = "Upload created successfully")
    public ResponseEntity<UploadResponse> createUpload(@ModelAttribute @Valid UploadRequest request) {
        return ResponseEntity.ok(new UploadResponse("upload-id-123", request.name()));
    }

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Batch upload with model attribute",
            description = "Batch upload endpoint demonstrating @ModelAttribute with complex nested types")
    @ApiResponse(responseCode = "200", description = "Batch upload completed")
    public ResponseEntity<Void> batchUpload(@ModelAttribute @Valid UploadRequest request) {
        return ResponseEntity.ok().build();
    }
}
