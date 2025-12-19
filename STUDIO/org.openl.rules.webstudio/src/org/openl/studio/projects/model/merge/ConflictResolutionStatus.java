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
    @JsonProperty("success")
    SUCCESS

}
