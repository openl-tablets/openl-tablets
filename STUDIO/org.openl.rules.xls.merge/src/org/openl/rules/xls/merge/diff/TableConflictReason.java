package org.openl.rules.xls.merge.diff;

/**
 * Why both revisions changed a table in ways the merge cannot combine.
 */
public enum TableConflictReason {

    /**
     * Both revisions changed the value or the comment of the same cell differently; a style changed differently keeps
     * OUR one
     */
    CELLS,
    /**
     * One revision inserted a row next to a row the other one deleted, or deleted a row the other one changed; columns
     * follow the same rule. Both revisions inserted rows right under the header of the table, where it keeps its
     * properties, or columns before its first column, which holds the header
     */
    ROWS,
    /**
     * One revision inserted or deleted rows of the table and the other one columns, or so many rows changed that they
     * cannot be aligned
     */
    ROWS_AND_COLUMNS,
    /**
     * Both revisions moved the table on purpose to different places: into another column, or away from the table
     * above it, which rows inserted above the table do not explain
     */
    MOVED,
    /**
     * One revision removed the table, and the other one changed it
     */
    REMOVED_AND_CHANGED,
    /**
     * Both revisions changed the merged cells of the table differently, or a change crosses the merged cells
     */
    MERGED_CELLS,
    /**
     * Both revisions added different tables at the same place
     */
    ADDED_IN_BOTH

}
