package org.openl.rules.dt;

import org.jspecify.annotations.NonNull;

import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.IGridTable;

/**
 * Transforms coordinates for table with two dimensions(table that has vertical and horizontal conditions).
 *
 * @author PUdalau
 */
public class TwoDimensionDecisionTableTransformer implements CoordinatesTransformer {

    public static final int HCONDITION_HEADERS_HEIGHT = 3;

    // width of simple(vertical) conditions in columns
    private final int conditionsWidth;
    // the row of the horizontal keys each horizontal condition takes its values from
    private final int @NonNull [] hConditionRows;
    private final int lookupValuesTableHeight;
    private final int lookupValuesTableWidth;
    private final int retTableWidth;

    // the rows of the headers and of the horizontal keys
    private final int dtHeaderHeight;

    /**
     * @param entireTable       The entire table with two dimensions(WITHOUT a header).
     * @param lookupValuesTable The "values subtable"
     * @param hConditionRows    The row of the horizontal keys each horizontal condition starts at. A condition whose
     *                          keys are merged down takes several rows of the keys.
     */
    TwoDimensionDecisionTableTransformer(@NonNull IGridTable entireTable,
                                         @NonNull IGridTable lookupValuesTable,
                                         int retTableWidth,
                                         int @NonNull [] hConditionRows) {
        this.lookupValuesTableHeight = lookupValuesTable.getHeight();
        this.lookupValuesTableWidth = lookupValuesTable.getWidth();
        this.conditionsWidth = entireTable.getWidth() - lookupValuesTableWidth;
        this.hConditionRows = hConditionRows;
        this.retTableWidth = retTableWidth;
        this.dtHeaderHeight = entireTable.getHeight() - lookupValuesTableHeight;
    }

    @Override
    public int getHeight() {
        return dtHeaderHeight + lookupValuesTableWidth / retTableWidth * lookupValuesTableHeight;
    }

    @Override
    public int getWidth() {
        return conditionsWidth + hConditionRows.length + retTableWidth;
    }

    /**
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return Coordinates inside the source table.
     */
    @Override
    public int getColumn(int col, int row) {
        if (row < dtHeaderHeight) {
            return col;
        } else if (col < conditionsWidth) {
            return col;
        } else if (col < conditionsWidth + hConditionRows.length) {
            var hConditionValueIndex = (row - dtHeaderHeight) / lookupValuesTableHeight * retTableWidth;
            return conditionsWidth + hConditionValueIndex;
        }
        var hConditionValueIndex = (row - dtHeaderHeight) / lookupValuesTableHeight * retTableWidth;
        return conditionsWidth + hConditionValueIndex + col - conditionsWidth - hConditionRows.length;
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
        } else if (col >= conditionsWidth && col < conditionsWidth + hConditionRows.length) {
            // the key of the horizontal condition
            return HCONDITION_HEADERS_HEIGHT + hConditionRows[col - conditionsWidth];
        }
        // a vertical condition value or a lookup value of the row
        return dtHeaderHeight + (row - dtHeaderHeight) % lookupValuesTableHeight;
    }
}
