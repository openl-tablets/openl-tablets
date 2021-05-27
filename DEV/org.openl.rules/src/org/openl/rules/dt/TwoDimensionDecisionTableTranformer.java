package org.openl.rules.dt;

import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.IGridTable;

/**
 * Transforms coordinates for table with two dimensions(table that has vertical and horizontal conditions).
 *
 * @author PUdalau
 */
public class TwoDimensionDecisionTableTranformer implements CoordinatesTransformer {

    private static final int CONDITION_HEADERS_HEIGHT = 4;
    private static final int HCONDITION_HEADERS_HEIGHT = 3;

    // width of simple(vertical) conditions in columns
    private final int conditionsWidth;
    private final int hConditionsCount;
    private final int lookupValuesTableHeight;
    private final int lookupValuesTableWidth;
    private final int retTableWidth;

    private final int dtHeaderHeight;

    /**
     * @param entireTable The entire table with two dimensions(WITHOUT a header).
     * @param lookupValuesTable The "values subtable"
     */
    TwoDimensionDecisionTableTranformer(IGridTable entireTable, IGridTable lookupValuesTable, int retTableWidth) {
        this.lookupValuesTableHeight = lookupValuesTable.getHeight();
        this.lookupValuesTableWidth = lookupValuesTable.getWidth();
        this.conditionsWidth = entireTable.getWidth() - lookupValuesTableWidth;
        this.hConditionsCount = entireTable.getHeight() - lookupValuesTableHeight - HCONDITION_HEADERS_HEIGHT;
        this.retTableWidth = retTableWidth;
        this.dtHeaderHeight = CONDITION_HEADERS_HEIGHT + hConditionsCount - 1;
    }

    int getRetTableWidth() {
        return retTableWidth;
    }

    @Override
    public int getHeight() {
        return dtHeaderHeight + lookupValuesTableWidth / retTableWidth * lookupValuesTableHeight;
    }

    @Override
    public int getWidth() {
        return conditionsWidth + hConditionsCount + retTableWidth;
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
        } else if (col < conditionsWidth + hConditionsCount) {
            int hConditionValueIndex = (row - dtHeaderHeight) / lookupValuesTableHeight * retTableWidth;
            return conditionsWidth + hConditionValueIndex;
        } else {
            int hConditionValueIndex = (row - dtHeaderHeight) / lookupValuesTableHeight * retTableWidth;
            return conditionsWidth + hConditionValueIndex + col - conditionsWidth - hConditionsCount;
        }
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
        } else if (col < conditionsWidth) {
            // getCoordinatesFromConditionValues
            int conditionValueIndex = (row - dtHeaderHeight) % lookupValuesTableHeight;
            return dtHeaderHeight + conditionValueIndex;
        } else if (col < conditionsWidth + hConditionsCount) {
            // getCoordinatesFromHConditionValues
            int hConditionIndex = col - conditionsWidth;
            return HCONDITION_HEADERS_HEIGHT + hConditionIndex;
        } else {
            // getCoordinatesFromLookupValues
            int conditionValueIndex = (row - dtHeaderHeight) % lookupValuesTableHeight;
            return HCONDITION_HEADERS_HEIGHT + hConditionsCount + conditionValueIndex;
        }
    }
}
