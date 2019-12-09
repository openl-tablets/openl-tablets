package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.table.*;
import org.openl.rules.utils.ParserUtils;

/**
 * Lookup table is a decision table that is created by transforming lookup tables to create a single-column return
 * value.<br>
 * <br>
 *
 * The lookup values could appear either left of the lookup table or on top of it.<br>
 * <br>
 *
 * The values on the left will be called <b>"vertical"</b> and values on top will be called <b>"horizontal"</b>.<br>
 * <br>
 *
 * The table should have at least one vertical condition column, it can have the Rule column, it (in theory) might have
 * vertical Actions which will be processed the same way as vertical conditions, it must have one or more Horizontal
 * Conditions, and exactly one (optional in the future release) <b>RET</b> or <b>CRET</b> column<br>
 * . <br>
 * <b>RET</b> or <b>CRET</b> section can be placed in any place of lookup headers row, after vertical conditions (for
 * users convenience).
 *
 * The Horizontal Conditions will be marked <b>HC1</b>, <b>HC2</b> etc. The first HC column or RET column will mark the
 * starting column of the lookup matrix
 */

public class DecisionTableLookupConvertor {

    private static final int HEADER_ROW = 0;
    private static final int DISPLAY_ROW = 3;

    private List<ILogicalTable> hcHeaders = new ArrayList<>();
    private ILogicalTable retTable;
    private DTScale scale;

    IGridTable convertTable(ILogicalTable table) throws OpenLCompilationException {
        ILogicalTable headerRow = table.getRow(HEADER_ROW);

        int firstLookupColumn = findFirstLookupColumn(headerRow);
        loadHorizConditionsAndReturnColumns(headerRow, firstLookupColumn);
        validateLookupSection();

        IGridRegion displayRowRegion = getDisplayRowRegion(table);

        int firstLookupGridColumn = headerRow.getColumn(firstLookupColumn).getSource().getGridColumn(0, 0);

        IGrid grid = table.getSource().getGrid();

        processHorizConditionsHeaders(displayRowRegion, firstLookupGridColumn, grid);

        IGridTable lookupValuesTable = getLookupValuesTable(table, firstLookupGridColumn, grid);

        Integer lookupValuesTableHeight = getLookupValuesTableHeight(table, grid);

        isMultiplier(lookupValuesTable);

        CoordinatesTransformer transformer = getTransformer(headerRow,
            table,
            lookupValuesTable,
            lookupValuesTableHeight);

        return new TransformedGridTable(table.getSource(), transformer);
    }

    /**
     *
     * @param headerRow row with lookup table headers.
     * @return physical index from grid table, indicating first empty cell in the header row
     */
    private int findFirstEmptyCellInHeader(ILogicalTable headerRow) {
        int ncol = headerRow.getSource().getWidth();
        for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
            String headerStr = headerRow.getSource().getCell(columnIndex, 0).getStringValue();

            if (headerStr == null) {
                return columnIndex;
            }
        }
        return 0;
    }

    private CoordinatesTransformer getTransformer(ILogicalTable headerRow,
            ILogicalTable table,
            IGridTable lookupValuesTable,
            Integer lookupValuesTableHeight) throws OpenLCompilationException {
        int retColumnStart = findRetColumnStart(headerRow);
        int firstEmptyCell = findFirstEmptyCellInHeader(headerRow);
        int retTableWidth = retTable.getSource().getCell(0, 0).getWidth();

        if (lookupValuesTableHeight == null) {
            lookupValuesTableHeight = lookupValuesTable.getHeight();
        }
        scale = new DTScale(lookupValuesTableHeight, lookupValuesTable.getWidth() / retTableWidth);

        if (isRetLastColumn(retColumnStart, retTableWidth, firstEmptyCell)) {
            return new TwoDimensionDecisionTableTranformer(table.getSource(), lookupValuesTable, retTableWidth);
        } else {
            return new LookupHeadersTransformer(table
                .getSource(), lookupValuesTable, retTableWidth, retColumnStart, firstEmptyCell);
        }
    }

    /**
     * Checks if the RET section is the last one in the header row
     *
     * @param retColumnStart index, indicating beginning of RET section
     * @param retTableWidth width of RET section
     * @param firstEmptyCell index, indicating first empty cell in the header
     * @return true if RET section is the last one
     */
    private boolean isRetLastColumn(int retColumnStart, int retTableWidth, int firstEmptyCell) {
        return retColumnStart + retTableWidth == firstEmptyCell;
    }

    /**
     * Finds the physical index from grid table, indicating beginning of RET section.
     *
     * @param headerRow row with lookup table headers. For example:<br>
     *            <table cellspacing="2">
     *            <tr>
     *            <td align="center" bgcolor="#8FCB52"><b>C1</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>C2</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>C3</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>HC1</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>HC2</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>HC3</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>RET1</b> or <b>CRET1</b></td>
     *            </tr>
     *            </table>
     *
     * @return the physical index from grid table, indicating beginning of RET section
     * @throws OpenLCompilationException if there is no RET or CRET section in the table.
     */
    private int findRetColumnStart(ILogicalTable headerRow) throws OpenLCompilationException {
        int ncol = headerRow.getSource().getWidth();

        for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
            String headerStr = headerRow.getSource().getCell(columnIndex, 0).getStringValue();

            if (headerStr != null) {
                headerStr = headerStr.toUpperCase();

                if (DecisionTableHelper.isValidRetHeader(headerStr) || DecisionTableHelper
                    .isValidCRetHeader(headerStr)) {
                    return columnIndex;
                }
            }
        }
        throw new OpenLCompilationException("Lookup table must have at least one RET or CRET column");
    }

    private void processHorizConditionsHeaders(IGridRegion displayRowRegion,
            int firstLookupGridColumn,
            IGrid grid) throws OpenLCompilationException {
        GridRegion hcHeadersRegion = new GridRegion(displayRowRegion);
        hcHeadersRegion.setLeft(firstLookupGridColumn);
        IGridTable hcHeaderTable = new GridTable(hcHeadersRegion, grid);

        validateHCHeaders(hcHeaderTable);
    }

    private IGridTable getLookupValuesTable(ILogicalTable originalTable,
            int firstLookupGridColumn,
            IGrid grid) throws OpenLCompilationException {
        ILogicalTable valueTable = originalTable.getRows(DISPLAY_ROW + 1);
        GridRegion lookupValuesRegion;
        if (valueTable != null) {
            lookupValuesRegion = new GridRegion(valueTable.getSource().getRegion());
        } else {
            throw new OpenLCompilationException("The table must have at least one row with values.");
        }
        lookupValuesRegion.setLeft(firstLookupGridColumn);

        return new GridTable(lookupValuesRegion, grid);
    }

    private Integer getLookupValuesTableHeight(ILogicalTable originalTable, IGrid grid) {
        String stringValue = originalTable.getCell(0, 0).getStringValue();
        if (stringValue == null) {
            stringValue = "";
        }
        stringValue = stringValue.toUpperCase();
        ILogicalTable valueTable = originalTable.getRows(DISPLAY_ROW + 1);
        if (DecisionTableHelper.isValidRuleHeader(stringValue) || DecisionTableHelper
            .isValidMergedConditionHeader(stringValue)) {
            return valueTable.getHeight();
        } else {
            return null;
        }
    }

    private int getWidthWithIgnoredEmptyCells(IGridTable table) {
        int width = table.getWidth();
        while (width > 0) {
            for (int i = 0; i < table.getHeight(); i++) {
                if (table.getCell(width - 1, 0).getStringValue() != null) {
                    return width;
                }
            }
            width--;
        }

        return 0;
    }

    private void isMultiplier(IGridTable lookupValuesTable) throws OpenLCompilationException {
        int retTableWidth = retTable.getSource().getWidth();
        int lookupTableWidth = lookupValuesTable.getWidth();

        boolean isMultiplier = lookupTableWidth % retTableWidth == 0;
        // lookupTableWidth/retTableWidth*retTableWidth == lookupTableWidth;

        if (!isMultiplier) {
            int w = getWidthWithIgnoredEmptyCells(lookupValuesTable);
            isMultiplier = w % retTableWidth == 0;
            if (!isMultiplier) {
                String message = String.format(
                    "The width of the lookup table(%d) is not a multiple of the RET width(%d)",
                    lookupTableWidth,
                    retTableWidth);
                throw new OpenLCompilationException(message);
            }
        }
    }

    private IGridRegion getDisplayRowRegion(ILogicalTable originaltable) {
        ILogicalTable tableWithDisplay = originaltable.getRows(DISPLAY_ROW);
        ILogicalTable displayRow = tableWithDisplay.getRow(0);
        return displayRow.getSource().getRegion();
    }

    private void validateHCHeaders(IGridTable hcHeaderTable) throws OpenLCompilationException {
        String message = String.format(
            "The height of the horizontal keys must be equal to the number of the %s headers.",
            DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
        assertEQ(hcHeaders.size(), hcHeaderTable.getHeight(), message);
    }

    private void assertEQ(int v1, int v2, String message) throws OpenLCompilationException {

        if (v1 == v2) {
            return;
        }

        throw new OpenLCompilationException(message);
    }

    /**
     *
     * @param headerRow row with lookup table headers. For example:
     *            <table cellspacing="2">
     *            <tr>
     *            <td align="center" bgcolor="#8FCB52"><b>C1</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>C2</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>HC1</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>HC2</b></td>
     *            <td align="center" bgcolor="#8FCB52"><b>RET1</b></td>
     *            </tr>
     *            </table>
     *            In this case the return will be <code>2</code>.
     *
     * @return NOTE!!! it returns an index of logical column!
     * @throws OpenLCompilationException when there is no lookup headers.
     */
    private int findFirstLookupColumn(ILogicalTable headerRow) throws OpenLCompilationException {
        int ncol = headerRow.getWidth();

        for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
            String headerStr = headerRow.getColumn(columnIndex).getSource().getCell(0, 0).getStringValue();

            if (headerStr != null) {
                headerStr = headerStr.toUpperCase();

                if (!(DecisionTableHelper.isValidRuleHeader(headerStr) || DecisionTableHelper
                    .isValidConditionHeader(headerStr) || DecisionTableHelper
                        .isValidMergedConditionHeader(headerStr) || ParserUtils.isBlankOrCommented(headerStr))) { // if
                    // the
                    // header in
                    // the
                    // column is
                    // not a
                    // valid
                    // header
                    // for common Decision Table, we consider that this column
                    // is going to be the beginning for Lookup table section.
                    return columnIndex;
                }
            }
        }
        throw new OpenLCompilationException("Lookup table must have at least one horizontal condition");
    }

    private void loadHorizConditionsAndReturnColumns(ILogicalTable rowHeader,
            int firstLookupColumn) throws OpenLCompilationException {

        int ncol = rowHeader.getWidth();

        while (firstLookupColumn < ncol) {

            ILogicalTable htable = rowHeader.getColumn(firstLookupColumn);
            String headerStr = htable.getSource().getCell(0, 0).getStringValue();

            if (headerStr != null) {
                headerStr = headerStr.toUpperCase();

                if (DecisionTableHelper.isValidHConditionHeader(headerStr)) {
                    if (htable.getSource().getWidth() != 1) {
                        throw new OpenLCompilationException("Column HC must have width=1");
                    }
                    hcHeaders.add(htable);
                } else if (DecisionTableHelper.isValidRetHeader(headerStr) || DecisionTableHelper
                    .isValidCRetHeader(headerStr)) {
                    if (retTable != null) {
                        throw new OpenLCompilationException("Lookup Table can have only one RET column");
                    }
                    retTable = htable;
                } else {
                    throw new OpenLCompilationException(
                        "Lookup Table allows only HC or RET or CRET columns after vertical conditions: " + headerStr);
                }
            }

            firstLookupColumn = firstLookupColumn + htable.getSource().getCell(0, 0).getWidth();
        }
    }

    private void validateLookupSection() throws OpenLCompilationException {
        if (hcHeaders.isEmpty()) {
            String message = String.format("Lookup Table must have at least one Horizontal Condition (%s1)",
                DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
            throw new OpenLCompilationException(message);
        }

        if (retTable == null) {
            String message = String.format("Lookup Table must have %s column",
                DecisionTableColumnHeaders.RETURN.getHeaderKey());
            throw new OpenLCompilationException(message);
        }

    }

    public DTScale getScale() {
        return scale;
    }

}
