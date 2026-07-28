/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import org.openl.rules.lang.xls.SpreadsheetConstants;
import org.openl.util.StringUtils;

/**
 * @author snshor
 */
public interface IGridRegion {
    IGridRegion[] EMPTY_REGION = new IGridRegion[0];

    class Tool {
        public static boolean contains(IGridRegion i1, int x, int y) {
            return i1.getLeft() <= x && x <= i1.getRight() && i1.getTop() <= y && y <= i1.getBottom();
        }

        /**
         * Return absolute column index taking into account region.
         *
         * @param region Region which contains column.
         * @param column Column index in the region.
         * @return Absolute column index.
         */
        public static int getAbsoluteColumn(IGridRegion region, int column) {
            return column + region.getLeft();
        }

        /**
         * Return absolute row index taking into account region.
         *
         * @param region Region which contains row.
         * @param row    Row index in the region.
         * @return Absolute row index.
         */
        public static int getAbsoluteRow(IGridRegion region, int row) {
            return row + region.getTop();
        }

        /**
         * Calculates grid region height.
         *
         * @param i1 grid region
         * @return grid region height.
         */
        public static int height(IGridRegion i1) {
            return i1.getBottom() - i1.getTop() + 1;
        }

        /**
         * Finds common {@link IGridRegion} for incoming regions.
         *
         * @param i1 first grid region
         * @param i2 second grid region
         * @return intersection of incoming grid regions. <code>null</code> if there is no intersection.
         */
        public static IGridRegion intersect(IGridRegion i1, IGridRegion i2) {
            var left = Math.max(i1.getLeft(), i2.getLeft());
            var right = Math.min(i1.getRight(), i2.getRight());
            var top = Math.max(i1.getTop(), i2.getTop());
            var bottom = Math.min(i1.getBottom(), i2.getBottom());
            return top <= bottom && left <= right ? new GridRegion(top, left, bottom, right) : null;
        }

        public static boolean intersects(IGridRegion i1, IGridRegion i2) {
            var left = Math.max(i1.getLeft(), i2.getLeft());
            var right = Math.min(i1.getRight(), i2.getRight());
            if (right < left) {
                return false;
            }
            var top = Math.max(i1.getTop(), i2.getTop());
            var bottom = Math.min(i1.getBottom(), i2.getBottom());
            return top <= bottom;
        }

        public static GridRegion move(IGridRegion reg, int dx, int dy) {
            return new GridRegion(reg.getTop() + dy, reg.getLeft() + dx, reg.getBottom() + dy, reg.getRight() + dx);
        }

        public static int width(IGridRegion i1) {
            return i1.getRight() - i1.getLeft() + 1;
        }

        public static int getColumn(String cell) {
            var col = 0;
            var mul = 'Z' - 'A' + 1;
            for (var i = 0; i < cell.length(); i++) {
                var ch = cell.charAt(i);
                if (!Character.isLetter(ch)) {
                    return col - 1;
                }
                col = col * mul + ch - 'A' + 1;
            }
            throw new RuntimeException("Invalid cell: " + cell);
        }

        public static int getRow(String cell) {
            for (var i = 0; i < cell.length(); i++) {
                var ch = cell.charAt(i);
                if (Character.isDigit(ch)) {
                    return Integer.parseInt(cell.substring(i)) - 1;
                }
            }
            throw new RuntimeException("Invalid cell: " + cell);
        }

        public static IGridRegion makeRegion(String range) {

            var idx = range.indexOf(AGrid.RANGE_SEPARATOR);
            if (idx < 0) {
                var col1 = getColumn(range);
                var row1 = getRow(range);
                return new GridRegion(row1, col1, row1, col1);
            }
            String[] rr = StringUtils.split(range, AGrid.RANGE_SEPARATOR);

            var col1 = getColumn(rr[0]);
            var row1 = getRow(rr[0]);
            var col2 = getColumn(rr[1]);
            var row2 = getRow(rr[1]);

            return new GridRegion(row1, col1, row2, col2);
        }

        public static boolean isValidRegion(IGridRegion region, SpreadsheetConstants spreadsheetConstants) {
            var rowIndex = region.getBottom();
            var columnIndex = region.getRight();
            return rowIndex >= 0 && rowIndex <= spreadsheetConstants
                    .getMaxRowIndex() && columnIndex >= 0 && columnIndex <= spreadsheetConstants.getMaxColumnIndex();
        }
    }

    int getBottom();

    int getLeft();

    int getRight();

    int getTop();

}
