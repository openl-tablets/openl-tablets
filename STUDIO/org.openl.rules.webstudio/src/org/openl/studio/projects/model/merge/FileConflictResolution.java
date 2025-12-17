package org.openl.studio.projects.model.merge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the resolution for a single conflicted file.
 *
 * @param filePath the path of the conflicted file
 * @param strategy the resolution strategy (base, ours, theirs, or custom)
 */
public record FileConflictResolution(

        @NotBlank(message = "File path is required")
        String filePath,

        @NotNull(message = "Resolution strategy is required")
        ConflictResolutionStrategy strategy

) {
}
