package org.openl.studio.projects.service.tables.theme;

import java.util.Set;
import java.util.function.IntBinaryOperator;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * A table as the compiler laid it out, and where its cells stand now.
 *
 * <p>The edits made since the table was compiled may have inserted or deleted rows and columns, or moved the whole
 * table on its sheet. A cell of the table as compiled stands where the edits moved it.
 *
 * @param node  the table, compiled
 * @param table where the table stands on its sheet now, header included
 * @param moves the rows and the columns the edits inserted or deleted since the table was compiled
 */
record CompiledTable(TableSyntaxNode node, IGridRegion table, TableMoves moves) {

    /**
     * Adds the place each cell of a part of the compiled table stands in now, such as its row. A cell an edit deleted
     * adds nothing.
     *
     * @param part  the part, as the compiler read it
     * @param place the place of a row and a column of the sheet
     * @param into  the places to add to
     */
    void addPlaces(IGridTable part, IntBinaryOperator place, Set<Integer> into) {
        for (var row = 0; row < part.getHeight(); row++) {
            for (var column = 0; column < part.getWidth(); column++) {
                var cell = part.getCell(column, row);
                addPlace(cell.getAbsoluteRow(), cell.getAbsoluteColumn(), place, into);
            }
        }
    }

    /**
     * Adds the place each cell of the sheet the compiler read in a region stands in now, as
     * {@link #addPlaces(IGridTable, IntBinaryOperator, Set)} adds the places of a part.
     *
     * @param region where the compiler read a part of the table on the sheet
     * @param place  the place of a row and a column of the sheet
     * @param into   the places to add to
     */
    void addPlaces(IGridRegion region, IntBinaryOperator place, Set<Integer> into) {
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var column = region.getLeft(); column <= region.getRight(); column++) {
                addPlace(row, column, place, into);
            }
        }
    }

    /**
     * Adds the place a cell of the sheet the compiler read stands in now. A cell an edit deleted adds nothing.
     *
     * @param row    the row of the sheet the compiler read the cell in
     * @param column the column of the sheet the compiler read the cell in
     * @param place  the place of a row and a column of the sheet
     * @param into   the places to add to
     */
    void addPlace(int row, int column, IntBinaryOperator place, Set<Integer> into) {
        var rowNow = rowNow(row);
        var columnNow = columnNow(column);
        if (rowNow != TableMoves.DELETED && columnNow != TableMoves.DELETED) {
            into.add(place.applyAsInt(rowNow, columnNow));
        }
    }

    /**
     * The row of the sheet a row of the compiled table stands in now.
     *
     * @param row the row of the sheet the compiler read
     * @return the row of the sheet now, or {@link TableMoves#DELETED} for a row an edit deleted
     */
    int rowNow(int row) {
        var moved = moves.row(row - node.getGridTable().getRegion().getTop());
        return moved == TableMoves.DELETED ? moved : table.getTop() + moved;
    }

    /**
     * The column of the sheet a column of the compiled table stands in now.
     *
     * @param column the column of the sheet the compiler read
     * @return the column of the sheet now, or {@link TableMoves#DELETED} for a column an edit deleted
     */
    int columnNow(int column) {
        var moved = moves.column(column - node.getGridTable().getRegion().getLeft());
        return moved == TableMoves.DELETED ? moved : table.getLeft() + moved;
    }
}
