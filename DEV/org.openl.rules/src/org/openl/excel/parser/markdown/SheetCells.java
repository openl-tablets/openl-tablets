package org.openl.excel.parser.markdown;

import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.jspecify.annotations.Nullable;

/**
 * The cells of a sheet holding a table of a Markdown file: the values and the merged ranges.
 *
 * <p>Rows and columns are counted from zero, from the top left corner of the sheet. A merged range holds its value
 * in its top left cell; the other cells it covers are {@code null}. Rows may differ in length: a missing cell is
 * blank.
 *
 * @param values the values by row and column; a blank cell is {@code null}
 * @param merges the merged ranges, none of them overlapping
 */
record SheetCells(@Nullable String[][] values, List<CellRangeAddress> merges) {

    /**
     * Returns the value of a cell, or {@code null} for a blank cell or one outside the rows.
     */
    @Nullable
    String value(int row, int column) {
        return row < values.length && column < values[row].length ? values[row][column] : null;
    }

    /**
     * Returns the merged range a cell lies in, or {@code null} when the cell is not merged.
     */
    @Nullable
    CellRangeAddress mergeAt(int row, int column) {
        return merges.stream().filter(range -> range.isInRange(row, column)).findFirst().orElse(null);
    }

    /** Equal to the cells holding the same values and merged ranges. */
    @Override
    public boolean equals(Object o) {
        return o instanceof SheetCells that && Arrays.deepEquals(values, that.values) && merges.equals(that.merges);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.deepHashCode(values) + merges.hashCode();
    }

    @Override
    public String toString() {
        return "SheetCells[values=" + Arrays.deepToString(values) + ", merges=" + merges + "]";
    }
}
