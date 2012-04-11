/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public interface IGridRegion {
	
	short TOP = 0;
	short LEFT = 1;
	short BOTTOM = 2;
	short RIGHT = 3; 
	
    public class Tool {
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
         * @param row Row index in the region.
         * @return Absolute row index.
         */
        public static int getAbsoluteRow(IGridRegion region, int row) {
            return row + region.getTop();
        }

        public static int height(IGridRegion i1) {
            return i1.getBottom() - i1.getTop() + 1;
        }

        public static IGridRegion intersect(IGridRegion i1, IGridRegion i2) {
            int left = Math.max(i1.getLeft(), i2.getLeft());
            int right = Math.min(i1.getRight(), i2.getRight());
            int top = Math.max(i1.getTop(), i2.getTop());
            int bottom = Math.min(i1.getBottom(), i2.getBottom());
            return top <= bottom && left <= right ? new GridRegion(top, left, bottom, right) : null;
        }

        public static boolean intersects(IGridRegion i1, IGridRegion i2) {
            int left = Math.max(i1.getLeft(), i2.getLeft());
            int right = Math.min(i1.getRight(), i2.getRight());
            if (right < left) {
                return false;
            }
            int top = Math.max(i1.getTop(), i2.getTop());
            int bottom = Math.min(i1.getBottom(), i2.getBottom());
            return top <= bottom;
        }

        public static GridRegion move(IGridRegion reg, int dx, int dy) {
            return new GridRegion(reg.getTop() + dy, reg.getLeft() + dx, reg.getBottom() + dy, reg.getRight() + dx);
        }

        public static int width(IGridRegion i1) {
            return i1.getRight() - i1.getLeft() + 1;
        }
    }

    int getBottom();

    int getLeft();

    int getRight();

    int getTop();

}
