package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Merge operation mode defining the direction of the merge.
 */
@Schema(description = "Merge operation mode defining the direction of the merge")
public enum MergeOpMode {

    /**
     * Receive mode: merge changes from the other branch into the current branch.
     */
    @Schema(description = "Merge changes from the other branch into the current branch")
    @JsonProperty("receive")
    RECEIVE,

    /**
     * Send mode: merge changes from the current branch into the other branch.
     */
    @Schema(description = "Merge changes from the current branch into the other branch")
    @JsonProperty("send")
    SEND

}
