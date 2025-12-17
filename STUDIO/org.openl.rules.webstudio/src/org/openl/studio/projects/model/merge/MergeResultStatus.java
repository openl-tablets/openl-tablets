package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status of merge operation result.
 */
@Schema(description = "Status of merge operation result")
public enum MergeResultStatus {
    /**
     * Merge completed successfully without conflicts.
     */
    @Schema(description = "Merge completed successfully without conflicts")
    @JsonProperty("success")
    SUCCESS,

    /**
     * Merge detected conflicts that require manual resolution.
     */
    @Schema(description = "Merge detected conflicts that require manual resolution")
    @JsonProperty("conflicts")
    CONFLICTS
}
