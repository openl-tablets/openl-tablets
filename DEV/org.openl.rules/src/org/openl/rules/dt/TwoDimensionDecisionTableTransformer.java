package org.openl.rules.dt;

import java.util.stream.IntStream;

import org.jspecify.annotations.NonNull;

import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.IGridTable;

/**
 * Transforms coordinates for table with two dimensions(table that has vertical and horizontal conditions).
 *
 * <p>Each group of columns of the lookup values makes one rule for each row of the values. A group takes as many
 * columns as the return header, or, for a lookup that returns the values under each key, as many columns as its key.
 * The return then takes as many columns as the widest group, and the columns a narrower group lacks are empty.
 *
 * @author PUdalau
 */
public class TwoDimensionDecisionTableTransformer implements CoordinatesTransformer {

    public static final int HCONDITION_HEADERS_HEIGHT = 3;

    // width of simple(vertical) conditions in columns
    private final int conditionsWidth;
    // the row of the horizontal keys each horizontal condition takes its values from
    private final int @NonNull [] hConditionRows;
    // the first column of the values of each group, and the end of the last group
    private final int @NonNull [] groupStarts;
    private final int lookupValuesTableHeight;
    // the width of the return header
    private final int retTableWidth;
    // the width of the widest group, which the return takes
    private final int returnWidth;

    // the rows of the headers and of the horizontal keys
    private final int dtHeaderHeight;

    /**
     * @param entireTable       The entire table with two dimensions(WITHOUT a header).
     * @param lookupValuesTable The "values subtable"
     * @param retTableWidth     The width of the return header.
     * @param hConditionRows    The row of the horizontal keys each horizontal condition starts at. A condition whose
     *                          keys are merged down takes several rows of the keys.
     * @param groupStarts       The first column of the values of each group, and the end of the last group.
     */
    TwoDimensionDecisionTableTransformer(@NonNull IGridTable entireTable,
                                         @NonNull IGridTable lookupValuesTable,
                                         int retTableWidth,
                                         int @NonNull [] hConditionRows,
                                         int @NonNull [] groupStarts) {
        this.lookupValuesTableHeight = lookupValuesTable.getHeight();
        this.conditionsWidth = entireTable.getWidth() - lookupValuesTable.getWidth();
        this.hConditionRows = hConditionRows;
        this.groupStarts = groupStarts;
        this.retTableWidth = retTableWidth;
        this.returnWidth = IntStream.range(0, groupStarts.length - 1)
                .map(this::groupWidth)
                .reduce(retTableWidth, Math::max);
        this.dtHeaderHeight = entireTable.getHeight() - lookupValuesTableHeight;
    }

    @Override
    public int getHeight() {
        return dtHeaderHeight + (groupStarts.length - 1) * lookupValuesTableHeight;
    }

    @Override
    public int getWidth() {
        return returnColumn() + returnWidth;
    }

    /**
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return Coordinates inside the source table.
     */
    @Override
    public int getColumn(int col, int row) {
        if (row < dtHeaderHeight) {
            // a header of the return wider than the return header stands on the return header, empty
            return col < returnColumn() + retTableWidth ? col : getColumn(returnColumn(), row);
        } else if (col < conditionsWidth) {
            return col;
        }
        var group = group(row);
        var groupStart = conditionsWidth + groupStarts[group];
        if (col < returnColumn()) {
            // the key over the group
            return groupStart;
        }
        // a value of the group: a column a narrower group lacks stands on its last column, empty
        return groupStart + Math.min(col - returnColumn(), groupWidth(group) - 1);
    }

    /**
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return Coordinates inside the source table.
     */
    @Override
    public int getRow(int col, int row) {
        if (row < dtHeaderHeight) {
            return row;
        } else if (col >= conditionsWidth && col < returnColumn()) {
            // the key of the horizontal condition
            return HCONDITION_HEADERS_HEIGHT + hConditionRows[col - conditionsWidth];
        }
        // a vertical condition value or a lookup value of the row
        return dtHeaderHeight + (row - dtHeaderHeight) % lookupValuesTableHeight;
    }

    /**
     * The columns of the return wider than the return header hold nothing in the headers, nor in the rules of a group
     * narrower than them.
     */
    @Override
    public boolean isEmpty(int col, int row) {
        var returnIndex = col - returnColumn();
        if (returnIndex < retTableWidth) {
            return false;
        }
        return row < dtHeaderHeight || returnIndex >= groupWidth(group(row));
    }

    /**
     * The headers of the return span the columns of the widest group: its kind, its expression, its parameter and its
     * title.
     */
    @Override
    public int getCellWidth(int col, int row) {
        return row < dtHeaderHeight && col == returnColumn() && returnWidth > retTableWidth ? returnWidth : 0;
    }

    private int returnColumn() {
        return conditionsWidth + hConditionRows.length;
    }

    private int group(int row) {
        return (row - dtHeaderHeight) / lookupValuesTableHeight;
    }

    private int groupWidth(int group) {
        return groupStarts[group + 1] - groupStarts[group];
    }
}
