package org.openl.studio.projects.model.merge;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Strategy for resolving a merge conflict.
 */
@Schema(description = "Strategy for resolving a merge conflict")
public enum ConflictResolutionStrategy {

    /**
     * Use the base version (common ancestor).
     */
    @Schema(description = "Use the base version (common ancestor before branches diverged)")
    BASE,

    /**
     * Use our version (current branch).
     */
    @Schema(description = "Use our version (from the current branch)")
    OURS,

    /**
     * Use their version (merging branch).
     */
    @Schema(description = "Use their version (from the merging branch)")
    THEIRS,

    /**
     * Use a custom uploaded file.
     */
    @Schema(description = "Use a custom uploaded file (must be provided in the 'files' parameter)")
    CUSTOM

}
