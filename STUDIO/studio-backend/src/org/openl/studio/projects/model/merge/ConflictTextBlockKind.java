package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a part of a conflicted text file is.
 */
@Schema(description = "What a part of a conflicted text file is")
public enum ConflictTextBlockKind {
    /**
     * Lines the merge took by itself.
     */
    @Schema(description = "Lines the merge took by itself")
    @JsonProperty("merged")
    MERGED,

    /**
     * Lines the two branches changed in different ways.
     */
    @Schema(description = "Lines the two branches changed in different ways")
    @JsonProperty("conflict")
    CONFLICT
}
