package org.openl.rules.xls.merge;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.jspecify.annotations.NullMarked;

/**
 * Tells whether a revision of a sheet moved the content of the base revision to other rows or columns.
 *
 * <p>Rows are compared by their content. A revision keeps the content in place when matching the rows at the same
 * positions finds as many unchanged rows as the best matching of the rows does. Otherwise some rows were inserted or
 * deleted inside the content, and the content below them moved. Columns are checked the same way.
 *
 * <p>Rows or columns added after the content into empty cells do not move anything. Changes that cannot be told
 * apart from a move, such as several neighbouring rows edited into copies of their neighbours, count as a move.
 */
@NullMarked
final class XlsSheetShifts {

    /**
     * The most cell comparisons a check of one sheet may take.
     */
    private static final long MAX_COMPARISONS = 50_000_000L;

    private XlsSheetShifts() {
    }

    /**
     * Checks whether either revision moved the content of the base revision of the sheet.
     *
     * @return {@code true} if either revision inserted or deleted rows or columns inside the content
     */
    static boolean isShifted(Sheet base, Sheet our, Sheet their) {
        var rows = Math.max(base.getLastRowNum(), Math.max(our.getLastRowNum(), their.getLastRowNum())) + 1;
        var columns = Math.max(lastColumn(base), Math.max(lastColumn(our), lastColumn(their))) + 1;
        var baseSignature = new Signature(base, rows, columns);
        return isMoved(baseSignature, new Signature(our, rows, columns))
                || isMoved(baseSignature, new Signature(their, rows, columns));
    }

    private static boolean isMoved(Signature base, Signature other) {
        return isMoved(base.rows, other.rows) || isMoved(base.columns, other.columns);
    }

    /**
     * @return the index of the last column of the sheet that has a cell, or {@code -1} for an empty sheet
     */
    static int lastColumn(Sheet sheet) {
        var last = -1;
        for (Row row : sheet) {
            last = Math.max(last, row.getLastCellNum() - 1);
        }
        return last;
    }

    /**
     * Checks whether the best matching of two sequences of the same length keeps more equal elements than matching
     * them at the same positions does.
     */
    static boolean isMoved(long[] base, long[] other) {
        var from = 0;
        var to = base.length;
        while (from < to && base[from] == other[from]) {
            from++;
        }
        while (to > from && base[to - 1] == other[to - 1]) {
            to--;
        }
        var inPlace = 0;
        for (var i = from; i < to; i++) {
            if (base[i] == other[i]) {
                inPlace++;
            }
        }
        var length = to - from;
        // A matching that keeps one more element than in place pairs elements no further apart than this.
        var band = length - inPlace;
        if ((long) band * length > MAX_COMPARISONS) {
            // Too many rows changed to tell a move from edits in place: take it as a move.
            return true;
        }
        return longestMatching(base, other, from, length, band) > inPlace;
    }

    /**
     * Counts the elements of the longest common subsequence of the parts of two sequences, among the matchings that
     * pair elements no further apart than the band. Values outside the band are never smaller than zero, so the
     * count is exact whenever the longest matching lies inside the band.
     */
    private static int longestMatching(long[] base, long[] other, int from, int length, int band) {
        var previous = new int[length + 1];
        var current = new int[length + 1];
        for (var i = 1; i <= length; i++) {
            var last = Math.min(length, i + band);
            for (var j = Math.max(1, i - band); j <= last; j++) {
                current[j] = base[from + i - 1] == other[from + j - 1]
                        ? previous[j - 1] + 1
                        : Math.max(previous[j], current[j - 1]);
            }
            var swap = previous;
            previous = current;
            current = swap;
        }
        return previous[length];
    }

    /**
     * Content digests of every row and every column of a sheet, over the same area for every revision.
     */
    private static final class Signature {

        private final long[] rows;
        private final long[] columns;

        private Signature(Sheet sheet, int rowCount, int columnCount) {
            rows = new long[rowCount];
            columns = new long[columnCount];
            for (Row row : sheet) {
                for (Cell cell : row) {
                    var digest = digest(cell);
                    if (digest != 0) {
                        var rowIndex = row.getRowNum();
                        var columnIndex = cell.getColumnIndex();
                        rows[rowIndex] += mix(columnIndex, digest);
                        columns[columnIndex] += mix(rowIndex, digest);
                    }
                }
            }
        }

        /**
         * Digest of the value of a cell, or {@code 0} for a cell with nothing to show. A blank cell with a style of
         * its own is not empty, so a row of formatted cells is told apart from an empty one.
         */
        private static long digest(Cell cell) {
            if (XlsSheetsMatcher.isBlankAndUnstyled(cell)) {
                return 0;
            }
            var type = cell.getCellType();
            var value = switch (type) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> Double.toString(cell.getNumericCellValue());
                case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
                case FORMULA -> cell.getCellFormula().trim();
                case ERROR -> Byte.toString(cell.getErrorCellValue());
                default -> "";
            };
            var digest = 31L * type.ordinal() + value.hashCode();
            return digest == 0 ? 1 : digest;
        }

        /**
         * Spreads a digest by the position of the cell, so the sum of the digests of a row tells their order.
         */
        private static long mix(int position, long digest) {
            var mixed = digest * 0x9E3779B97F4A7C15L + position * 0xC2B2AE3D27D4EB4FL;
            mixed ^= mixed >>> 32;
            mixed *= 0xD6E8FEB86659FD93L;
            return mixed ^ (mixed >>> 32);
        }
    }
}
