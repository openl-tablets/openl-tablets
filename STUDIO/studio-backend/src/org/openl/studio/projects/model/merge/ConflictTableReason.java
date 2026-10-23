package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Why both branches changed a table of a conflicted workbook in ways the merge cannot combine.
 */
@Schema(description = "Why both branches changed a table in ways the merge cannot combine")
public enum ConflictTableReason {
    /**
     * Both branches changed the value or the comment of the same cell differently. A style changed differently keeps
     * the one of your version.
     */
    @Schema(description = "Both branches changed the value or the comment of the same cell differently")
    @JsonProperty("cells")
    CELLS,

    /**
     * One branch inserted a row next to a row the other deleted, or deleted a row the other changed; columns alike.
     * Both inserted rows right under the header, where the properties are, or columns before the first column.
     */
    @Schema(description = """
            One branch inserted a row next to a row the other deleted, or deleted a row the other \
            changed; columns follow the same rule. Both branches inserted rows right under the header of the table, \
            where it keeps its properties, or columns before its first column, which holds the header""")
    @JsonProperty("rows")
    ROWS,

    /**
     * One branch inserted or deleted rows and the other columns, or too many rows changed to align them.
     */
    @Schema(description = """
            One branch inserted or deleted rows of the table and the other columns, or so many rows \
            changed that they cannot be aligned""")
    @JsonProperty("rowsAndColumns")
    ROWS_AND_COLUMNS,

    /**
     * Both branches moved the table on purpose to different places, not only pushed it by rows inserted above it.
     */
    @Schema(description = "Both branches moved the table on purpose to different places")
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
