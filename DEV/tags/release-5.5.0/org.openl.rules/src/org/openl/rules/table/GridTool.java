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
public class GridTool {

    public static int compare(int c1, int r1, int c2, int r2) {
        if (r1 + c1 == r2 + c2) {
            return c2 - c1;
        }

        return r1 + c1 - r2 - c2;
    }

    public static boolean contains(IGridRegion reg1, IGridRegion reg2) {
        return contains(reg1, reg2.getLeft(), reg2.getTop()) && contains(reg1, reg2.getRight(), reg2.getBottom());
    }

    public static boolean contains(IGridRegion reg, int c, int r) {
        return contains(reg.getTop(), reg.getLeft(), reg.getBottom(), reg.getRight(), c, r);
    }

    public static boolean contains(int top, int left, int bottom, int right, int c, int r) {
        return top <= r && r <= bottom && left <= c && c <= right;
    }

}
