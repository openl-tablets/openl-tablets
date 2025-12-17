package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Version of a conflicted file to retrieve.
 */
@Schema(description = "Version of a conflicted file")
public enum ConflictBase {

    /**
     * Base version: common ancestor before branches diverged.
     */
    @Schema(description = "Common ancestor version before branches diverged")
    @JsonProperty("base")
    BASE,

    /**
     * Ours version: from the current branch.
     */
    @Schema(description = "Version from the current branch")
    @JsonProperty("ours")
    OURS,

    /**
     * Theirs version: from the merging branch.
     */
    @Schema(description = "Version from the merging branch")
    @JsonProperty("theirs")
    THEIRS

}
