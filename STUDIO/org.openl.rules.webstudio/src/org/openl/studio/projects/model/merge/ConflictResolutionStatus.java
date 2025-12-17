package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status of conflict resolution operation.
 */
@Schema(description = "Status of conflict resolution operation")
public enum ConflictResolutionStatus {

    /**
     * All conflicts were resolved successfully.
     */
    @Schema(description = "All conflicts were resolved successfully")
    @JsonProperty("success")
    SUCCESS,

    /**
     * Some conflicts were resolved, but some remain unresolved.
     */
    @Schema(description = "Some conflicts were resolved, but some remain unresolved")
    @JsonProperty("partial")
    PARTIAL,

    /**
     * Conflict resolution failed.
     */
    @Schema(description = "Conflict resolution failed")
    @JsonProperty("failed")
    FAILED

}
