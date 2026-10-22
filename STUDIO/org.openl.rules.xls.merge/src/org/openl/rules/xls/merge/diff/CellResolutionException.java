package org.openl.rules.xls.merge.diff;

import lombok.Getter;

/**
 * Thrown when a table conflict resolved cell by cell names what its merged table cannot take: a cell outside the table,
 * a value typed for a cell that merged cells cover, or a formula that cannot be read.
 */
@Getter
public class CellResolutionException extends IllegalArgumentException {

    /**
     * What the resolution names that the merged table cannot take.
     */
    public enum Kind {
        /**
         * A cell outside the merged table
         */
        OUTSIDE,
        /**
         * A value typed for a cell that merged cells cover, other than their first cell
         */
        COVERED,
        /**
         * A typed formula that cannot be read
         */
        FORMULA
    }

    /** The identifier of the table conflict. */
    private final String conflict;

    private final Kind kind;

    /** The row of the merged table, counted from 0. */
    private final int row;

    /** The column of the merged table, counted from 0. */
    private final int column;

    public CellResolutionException(String conflict, Kind kind, int row, int column) {
        super("The resolution of the table '%s' cannot take %s at row %d, column %d."
                .formatted(conflict, kind, row, column));
        this.conflict = conflict;
        this.kind = kind;
        this.row = row;
        this.column = column;
    }
}
