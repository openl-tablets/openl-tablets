package org.openl.studio.projects.service.tables.theme;

import java.util.HashSet;
import java.util.Set;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;

/**
 * The body of a table a theme lays out, and what its layout knows about the table.
 *
 * @param rows       the body: the rows of the table under its header and the properties it declares
 * @param base       the look every cell of the table starts from
 * @param look       the look of the table
 * @param transposed whether the compiler read the table with its rows and columns swapped
 * @param compiled   the table as it was compiled, and where its cells stand now
 */
@Builder
record ThemedBody(ILogicalTable rows,
                  ThemeStyle base,
                  TableTheme.Look look,
                  boolean transposed,
                  CompiledTable compiled) {

    /** No column or row of the body. */
    static final int NONE = -1;

    /** The body read the way the compiler reads it: with its rows and columns swapped when it is transposed. */
    ILogicalTable upright() {
        return transposed ? rows.transpose() : rows;
    }

    /**
     * The columns of the body, read the way the compiler reads it, that a column of the compiled table stands in now.
     *
     * <p>A column stands where the edits since the compilation moved it. A column an edit deleted stands nowhere.
     *
     * @param compiled the compiled table, read the way the compiler reads it
     * @param column   the column of the compiled table, or {@code null} for none
     * @return the columns of {@link #upright()}
     */
    Set<Integer> columnsNow(ILogicalTable compiled, @Nullable Integer column) {
        if (column == null || column < 0 || column >= compiled.getWidth()) {
            return Set.of();
        }
        return placesNow(compiled.getColumn(column).getSource(), true);
    }

    /**
     * The rows of the body, read the way the compiler reads it, that a row of the compiled table stands in now, as
     * {@link #columnsNow} tells it of a column.
     *
     * @param compiled the compiled table, read the way the compiler reads it
     * @param row      the row of the compiled table
     * @return the rows of {@link #upright()}
     */
    Set<Integer> rowsNow(ILogicalTable compiled, int row) {
        if (row < 0 || row >= compiled.getHeight()) {
            return Set.of();
        }
        return placesNow(compiled.getRow(row).getSource(), false);
    }

    /**
     * The rows or the columns of the body, read the way the compiler reads it, that a part of the compiled table stands
     * in now.
     *
     * @param part    the part of the compiled table
     * @param columns whether to tell the columns, rather than the rows
     * @return the rows or the columns of {@link #upright()}
     */
    Set<Integer> placesNow(IGridTable part, boolean columns) {
        var upright = upright();
        var places = new HashSet<Integer>();
        compiled.addPlaces(part, (row, column) -> placeAt(upright, columns, row, column), places);
        return inBody(places);
    }

    /**
     * The rows or the columns of the body, read the way the compiler reads it, that the cells of the sheet the compiler
     * read in a region stand in now.
     *
     * @param region  where the compiler read a part of the table on the sheet
     * @param columns whether to tell the columns, rather than the rows
     * @return the rows or the columns of {@link #upright()}
     */
    Set<Integer> placesNow(IGridRegion region, boolean columns) {
        var upright = upright();
        var places = new HashSet<Integer>();
        compiled.addPlaces(region, (row, column) -> placeAt(upright, columns, row, column), places);
        return inBody(places);
    }

    /** The places that stand in the body, out of the places of some cells of the sheet. */
    private static Set<Integer> inBody(Set<Integer> places) {
        places.remove(NONE);
        return Set.copyOf(places);
    }

    /**
     * The column or the row of the upright body that a cell of the sheet stands in, or {@link #NONE} for a cell outside
     * the body.
     *
     * <p>A column of the upright body takes the columns of the sheet its first cell takes, and a row the rows its first
     * cell takes. In a transposed table it is the other way round: a column takes rows of the sheet, a row columns.
     */
    private int placeAt(ILogicalTable upright, boolean columns, int row, int column) {
        var count = columns ? upright.getWidth() : upright.getHeight();
        // A column of the body is a column of the sheet, or a row of it in a transposed table; a row the other way.
        var acrossColumns = columns != transposed;
        var at = acrossColumns ? column : row;
        for (var place = 0; place < count; place++) {
            var region = (columns ? upright.getCell(place, 0) : upright.getCell(0, place)).getAbsoluteRegion();
            var from = acrossColumns ? region.getLeft() : region.getTop();
            var to = acrossColumns ? region.getRight() : region.getBottom();
            if (from <= at && at <= to) {
                return place;
            }
        }
        return NONE;
    }
}
