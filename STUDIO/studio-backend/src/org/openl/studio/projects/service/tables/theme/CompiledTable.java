package org.openl.studio.projects.service.tables.theme;

import java.util.Set;
import java.util.function.IntBinaryOperator;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * A table as the compiler laid it out.
 *
 * <p>A theme is laid over the table as it was compiled, so a cell of a part the compiler read stands where the
 * compiler read it.
 *
 * @param node the table, compiled
 */
record CompiledTable(TableSyntaxNode node) {

    /**
     * Adds the place each cell of a part of the compiled table takes, such as its row.
     *
     * @param part  the part, as the compiler read it
     * @param place the place of a row and a column of the sheet
     * @param into  the places to add to
     */
    void addPlaces(IGridTable part, IntBinaryOperator place, Set<Integer> into) {
        for (var row = 0; row < part.getHeight(); row++) {
            for (var column = 0; column < part.getWidth(); column++) {
                var cell = part.getCell(column, row);
                into.add(place.applyAsInt(cell.getAbsoluteRow(), cell.getAbsoluteColumn()));
            }
        }
    }

    /**
     * Adds the place each cell of the sheet the compiler read in a region takes, as
     * {@link #addPlaces(IGridTable, IntBinaryOperator, Set)} adds the places of a part.
     *
     * @param region where the compiler read a part of the table on the sheet
     * @param place  the place of a row and a column of the sheet
     * @param into   the places to add to
     */
    void addPlaces(IGridRegion region, IntBinaryOperator place, Set<Integer> into) {
        for (var row = region.getTop(); row <= region.getBottom(); row++) {
            for (var column = region.getLeft(); column <= region.getRight(); column++) {
                into.add(place.applyAsInt(row, column));
            }
        }
    }
}
