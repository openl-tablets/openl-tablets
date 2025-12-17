package org.openl.studio.projects.rest.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import org.openl.studio.projects.model.merge.ConflictResolutionStrategy;

public record FileConflictResolution(
        @Schema(description = "Path of the conflicted file within the project", example = "rules/MyRules.xlsx", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "File path is required")
        String filePath,

        @Schema(description = "Resolution strategy: 'base' (use common ancestor), 'ours' (use current branch), 'theirs' (use merging branch), or 'custom' (upload resolved file)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Resolution strategy is required")
        ConflictResolutionStrategy strategy,

        @Schema(description = "Uploaded file for 'custom' resolution strategy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        MultipartFile file
) {
}
