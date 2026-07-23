package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What stands between the user and the merge they asked about.
 *
 * <p>Reported next to the merge status: whether the branches differ is one question, and whether this
 * user may merge them is another. A user who may not merge still sees where the branches stand.
 */
@Schema(description = "What prevents the current user from performing the merge")
public enum MergeBlockedBy {

    /**
     * The target branch is protected and the user may confirm the merge as an explicit bypass.
     */
    @Schema(description = "The target branch is protected; the merge needs an explicit bypass confirmation")
    @JsonProperty("bypass-required")
    BYPASS_REQUIRED,

    /**
     * The target branch is protected and the user has no right to bypass its protection.
     */
    @Schema(description = "The target branch is protected and the user cannot bypass its protection")
    @JsonProperty("protected-branch")
    PROTECTED_BRANCH,

    /**
     * The target branch is locked by another user's operation.
     */
    @Schema(description = "The target branch is locked by another operation")
    @JsonProperty("locked")
    LOCKED,
}
