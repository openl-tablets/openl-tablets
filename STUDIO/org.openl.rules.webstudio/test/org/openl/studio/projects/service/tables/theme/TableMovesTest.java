package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Covers where a row or a column of a table as compiled stands once edits inserted or deleted rows and columns.
 */
class TableMovesTest {

    @Test
    void keepsEveryRowAndColumnWhereItWasWhenNothingMoved() {
        assertEquals(3, TableMoves.NONE.row(3));
        assertEquals(3, TableMoves.NONE.column(3));
    }

    @Test
    void movesTheRowsFromTheOneAnInsertLandsOnDown() {
        var moves = TableMoves.NONE.insertedRows(2, 3);

        assertEquals(1, moves.row(1));
        assertEquals(5, moves.row(2), "The row that stood where the insert lands moves down with the rest");
        assertEquals(6, moves.row(3));
        assertEquals(2, moves.column(2), "An insert of rows moves no column");
    }

    @Test
    void movesTheColumnsAfterADeleteLeftAndLosesTheColumnsDeleted() {
        var moves = TableMoves.NONE.deletedColumns(1, 2);

        assertEquals(0, moves.column(0));
        assertEquals(TableMoves.DELETED, moves.column(1));
        assertEquals(TableMoves.DELETED, moves.column(2));
        assertEquals(1, moves.column(3));
        assertEquals(1, moves.row(1), "A delete of columns moves no row");
    }

    @Test
    void followsTheEditsInTheOrderTheyWereMade() {
        // A column inserted at the start, then the column that stood first deleted from where it moved to.
        var moves = TableMoves.NONE.insertedColumns(0, 1).deletedColumns(1, 1).deletedRows(0, 1).insertedRows(1, 1);

        assertEquals(TableMoves.DELETED, moves.column(0));
        assertEquals(1, moves.column(1));
        assertEquals(TableMoves.DELETED, moves.row(0), "A row deleted stays deleted whatever comes after");
        assertEquals(0, moves.row(1));
        assertEquals(2, moves.row(2));
    }
}
