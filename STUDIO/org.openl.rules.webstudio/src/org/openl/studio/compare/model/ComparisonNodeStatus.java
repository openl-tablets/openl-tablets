package org.openl.studio.compare.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How an element of the second file relates to the same element of the first one.
 */
@Schema(description = "How an element of the second file relates to the same element of the first one")
public enum ComparisonNodeStatus {

    @Schema(description = "The element is the same in both files")
    @JsonProperty("equal")
    EQUAL,

    @Schema(description = "The element is in both files and differs")
    @JsonProperty("changed")
    CHANGED,

    @Schema(description = "The element is in the second file only")
    @JsonProperty("added")
    ADDED,

    @Schema(description = "The element is in the first file only")
    @JsonProperty("removed")
    REMOVED
}
