package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Strategy for resolving a merge conflict.
 */
public enum ConflictResolutionStrategy {

    /**
     * Use the base version (common ancestor).
     */
    @JsonProperty("base")
    BASE,

    /**
     * Use our version (current branch).
     */
    @JsonProperty("ours")
    OURS,

    /**
     * Use their version (merging branch).
     */
    @JsonProperty("theirs")
    THEIRS,

    /**
     * Use a custom uploaded file.
     */
    @JsonProperty("custom")
    CUSTOM

}
