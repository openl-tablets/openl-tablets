package org.openl.studio.projects.model.merge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the resolution for a single conflicted file.
 *
 * @param filePath the path of the conflicted file
 * @param strategy the resolution strategy (base, ours, theirs, or custom)
 */
@Schema(description = "Resolution strategy for a single conflicted file")
public record FileConflictResolution(

        @Schema(description = "Path of the conflicted file within the project", example = "rules/MyRules.xlsx", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "File path is required")
        String filePath,

        @Schema(description = "Resolution strategy: 'base' (use common ancestor), 'ours' (use current branch), 'theirs' (use merging branch), or 'custom' (upload resolved file)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Resolution strategy is required")
        ConflictResolutionStrategy strategy

) {
}
