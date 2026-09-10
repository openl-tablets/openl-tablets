package org.openl.studio.compare.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kind of element in a comparison tree.
 */
@Schema(description = "Kind of element in a comparison tree")
public enum ComparisonNodeType {

    @Schema(description = "A sheet of the compared workbooks")
    @JsonProperty("sheet")
    SHEET,

    @Schema(description = "A table of a sheet")
    @JsonProperty("table")
    TABLE
}
