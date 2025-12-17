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
public class UploadRequest {

    @Schema(description = "Name of the upload", example = "my-upload", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String name;

    @Schema(description = "Description of the upload")
    @Size(min = 10, max = 500)
    private String description;

    @Schema(description = "Priority level", example = "5")
    @Min(1)
    @Max(10)
    private Integer priority;

    @Schema(description = "Email address for notifications", example = "user@example.com")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$")
    private String email;

    @Schema(description = "List of tags associated with the upload")
    @NotNull
    @Size(min = 1)
    private List<String> tags;

    @Schema(description = "Uploaded file for processing")
    private MultipartFile file;

    @Schema(description = "Additional metadata as key-value pairs")
    private List<MetadataEntry> metadata;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public List<MetadataEntry> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataEntry> metadata) {
        this.metadata = metadata;
    }
}
