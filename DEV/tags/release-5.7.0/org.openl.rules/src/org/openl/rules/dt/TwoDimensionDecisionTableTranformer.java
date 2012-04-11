package org.openl.rules.dt;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.Point;

/**
 * Transforms coordinates for table with two dimensions(table that has vertical
 * and horizontal conditions).
 * 
 * @author PUdalau
 */
public class TwoDimensionDecisionTableTranformer implements CoordinatesTransformer {
    private static final int CONDITION_HEADERS_HEIGHT = 4;
    private static final int HCONDITION_HEADERS_HEIGHT = 3;
    private int conditionsCount;
    private int hConditionsCount;
    private int lookupValuesTableHeight;
    private int lookupValuesTableWidth;

    /**
     * @param conditionsCount Vertical conditions count.
     * @param hConditionsCount Horizontal conditions count.
     * @param lookupValuesTableHeight Height of "values subtable" == vertical
     *            condition values height.
     * @param lookupValuesTableWidth Width of "values subtable" == vertical
     *            condition values width.
     */
    public TwoDimensionDecisionTableTranformer(int conditionsCount, int hConditionsCount, int lookupValuesTableHeight,
            int lookupValuesTableWidth) {
        this.conditionsCount = conditionsCount;
        this.hConditionsCount = hConditionsCount;
        this.lookupValuesTableHeight = lookupValuesTableHeight;
        this.lookupValuesTableWidth = lookupValuesTableWidth;
    }

    /**
     * @param entireTable The entire table with two dimensions(WITHOUT a
     *            header).
     * @param lookupValuesTable The "values subtable"
     */
    public TwoDimensionDecisionTableTranformer(IGridTable entireTable, IGridTable lookupValuesTable) {
        lookupValuesTableHeight = lookupValuesTable.getGridHeight();
        lookupValuesTableWidth = lookupValuesTable.getGridWidth();
        this.conditionsCount = entireTable.getGridWidth() - lookupValuesTableWidth;
        this.hConditionsCount = entireTable.getGridHeight() - lookupValuesTableHeight - HCONDITION_HEADERS_HEIGHT;
    }

    public Point calculateCoordinates(int column, int row) {
        if (row < CONDITION_HEADERS_HEIGHT) {
            return getCoordinatesFromConditionHeaders(column, row);
        }
        if (column < conditionsCount) {
            return getCoordinatesFromConditionValues(column, row);
        }
        if (column < conditionsCount + hConditionsCount) {
            return getCoordinatesFromHConditionValues(column, row);
        }
        return getCoordinatesFromLookupValues(column, row);
    }

    private Point getCoordinatesFromConditionHeaders(int column, int row) {
        return new Point(column, row);
    }

    private Point getCoordinatesFromConditionValues(int column, int row) {
        int conditionValueIndex = (row - CONDITION_HEADERS_HEIGHT) % lookupValuesTableHeight;
        return new Point(column, HCONDITION_HEADERS_HEIGHT + hConditionsCount + conditionValueIndex);
    }

    private Point getCoordinatesFromHConditionValues(int column, int row) {
        int hConditionIndex = column - conditionsCount;
        int hConditionValueIndex = (row - CONDITION_HEADERS_HEIGHT) / lookupValuesTableHeight;
        return new Point(conditionsCount + hConditionValueIndex, HCONDITION_HEADERS_HEIGHT + hConditionIndex);
    }

    private Point getCoordinatesFromLookupValues(int column, int row) {
        int conditionValueIndex = (row - CONDITION_HEADERS_HEIGHT) % lookupValuesTableHeight;
        int hConditionValueIndex = (row - CONDITION_HEADERS_HEIGHT) / lookupValuesTableHeight;
        return new Point(conditionsCount + hConditionValueIndex, HCONDITION_HEADERS_HEIGHT + hConditionsCount
                + conditionValueIndex);
    }

    public int getHeight() {
        return CONDITION_HEADERS_HEIGHT + lookupValuesTableWidth * lookupValuesTableHeight;
    }

    public int getWidth() {
        return conditionsCount + hConditionsCount + 1;
    }
}
