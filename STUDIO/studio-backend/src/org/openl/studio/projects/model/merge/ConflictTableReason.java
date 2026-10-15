package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Why both branches changed a table of a conflicted workbook in ways the merge cannot combine.
 */
@Schema(description = "Why both branches changed a table in ways the merge cannot combine")
public enum ConflictTableReason {
    /**
     * Both branches changed the value, the style or the comment of the same cell differently.
     */
    @Schema(description = "Both branches changed the value, the style or the comment of the same cell differently")
    @JsonProperty("cells")
    CELLS,

    /**
     * Both branches inserted rows at the same place, or one deleted a row the other changed; columns alike.
     */
    @Schema(description = "Both branches inserted different rows at the same place, one inserted a row next to a "
            + "row the other deleted, or deleted a row the other changed; columns follow the same rule")
    @JsonProperty("rows")
    ROWS,

    /**
     * One branch inserted or deleted rows and the other columns, or too many rows changed to align them.
     */
    @Schema(description = "One branch inserted or deleted rows of the table and the other columns, or so many rows "
            + "changed that they cannot be aligned")
    @JsonProperty("rowsAndColumns")
    ROWS_AND_COLUMNS,

    /**
     * Both branches moved the table to different places.
     */
    @Schema(description = "Both branches moved the table to different places")
    @JsonProperty("moved")
    MOVED,

    /**
     * One branch removed the table, and the other one changed it.
     */
    @Schema(description = "One branch removed the table, and the other one changed it")
    @JsonProperty("removedAndChanged")
    REMOVED_AND_CHANGED,

    /**
     * Both branches changed the merged cells of the table differently, or a change crosses them.
     */
    @Schema(description = "Both branches changed the merged cells of the table differently, or a change crosses them")
    @JsonProperty("mergedCells")
    MERGED_CELLS,

    /**
     * Both branches added different tables at the same place.
     */
    @Schema(description = "Both branches added different tables at the same place")
    @JsonProperty("addedInBoth")
    ADDED_IN_BOTH
}
