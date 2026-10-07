package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status of merge check operation.
 */
@Schema(description = "Status of merge check operation")
public enum CheckMergeStatus {

    /**
     * Branches can be merged without conflicts.
     */
    @Schema(description = "Branches can be merged without conflicts")
    @JsonProperty("mergeable")
    MERGEABLE,

    /**
     * Target branch is already up-to-date with source branch, no merge needed.
     */
    @Schema(description = "Target branch is already up-to-date with source branch")
    @JsonProperty("up-to-date")
    UP2DATE,
}
