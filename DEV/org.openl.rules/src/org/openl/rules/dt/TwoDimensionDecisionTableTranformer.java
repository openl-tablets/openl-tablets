package org.openl.rules.dt;

import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.IGridTable;

/**
 * Transforms coordinates for table with two dimensions(table that has vertical
 * and horizontal conditions).
 * 
 * @author PUdalau
 */
public class TwoDimensionDecisionTableTranformer implements CoordinatesTransformer {

    private static final int CONDITION_HEADERS_HEIGHT = 4;
    private static final int HCONDITION_HEADERS_HEIGHT = 3;

    // width of simple(vertical) conditions in columns
    private int conditionsWidth;
    private int hConditionsCount;
    private int lookupValuesTableHeight;
    private int lookupValuesTableWidth;
    private int retTableWidth;

    private int dtHeaderHeight;

    /**
     * @param entireTable The entire table with two dimensions(WITHOUT a
     *            header).
     * @param lookupValuesTable The "values subtable"
     */
    TwoDimensionDecisionTableTranformer(IGridTable entireTable, IGridTable lookupValuesTable, int retTableWidth) {
        this.lookupValuesTableHeight = lookupValuesTable.getHeight();
        this.lookupValuesTableWidth = lookupValuesTable.getWidth();
        this.conditionsWidth = entireTable.getWidth() - lookupValuesTableWidth;
        this.hConditionsCount = entireTable.getHeight() - lookupValuesTableHeight - HCONDITION_HEADERS_HEIGHT;
        this.retTableWidth = retTableWidth;
        this.dtHeaderHeight = CONDITION_HEADERS_HEIGHT + (hConditionsCount - 1);
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
        int res;
        if (row < dtHeaderHeight) {
            // getCoordinatesFromConditionHeaders
            res = col;
        } else if (col < conditionsWidth) {
            // getCoordinatesFromConditionValues(
            res = col;
        } else if (col < conditionsWidth + hConditionsCount) {
            // getCoordinatesFromHConditionValues
            int hConditionValueIndex = (row - dtHeaderHeight) / lookupValuesTableHeight * retTableWidth;
            res = conditionsWidth + hConditionValueIndex;
        } else {
            // getCoordinatesFromLookupValues
            int hConditionValueIndex = (row - dtHeaderHeight) / lookupValuesTableHeight * retTableWidth;
            res = conditionsWidth + hConditionValueIndex + (col - conditionsWidth - hConditionsCount);
        }
        return res;
    }

    /**
     * @param col The column of logical table.
     * @param row The row of logical table.
     * @return Coordinates inside the source table.
     */
    @Override
    public int getRow(int col, int row) {
        int res;
        if (row < dtHeaderHeight) {
            // getCoordinatesFromConditionHeaders
            res = row;
        } else if (col < conditionsWidth) {
            // getCoordinatesFromConditionValues
            int conditionValueIndex = (row - dtHeaderHeight) % lookupValuesTableHeight;
            res = dtHeaderHeight + conditionValueIndex;
        } else if (col < conditionsWidth + hConditionsCount) {
            // getCoordinatesFromHConditionValues
            int hConditionIndex = col - conditionsWidth;
            res = HCONDITION_HEADERS_HEIGHT + hConditionIndex;
        } else {
            // getCoordinatesFromLookupValues
            int conditionValueIndex = (row - dtHeaderHeight) % lookupValuesTableHeight;
            res = HCONDITION_HEADERS_HEIGHT + hConditionsCount + conditionValueIndex;
        }
        return res;
    }
}
