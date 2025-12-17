package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Status of conflict resolution operation.
 */
public enum ConflictResolutionStatus {

    /**
     * All conflicts were resolved successfully.
     */
    @JsonProperty("success")
    SUCCESS,

    /**
     * Some conflicts were resolved, but some remain unresolved.
     */
    @JsonProperty("partial")
    PARTIAL,

    /**
     * Conflict resolution failed.
     */
    @JsonProperty("failed")
    FAILED

}
