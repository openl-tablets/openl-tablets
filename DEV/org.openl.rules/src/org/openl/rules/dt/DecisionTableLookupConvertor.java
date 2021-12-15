package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.TransformedGridTable;
import org.openl.util.ParserUtils;

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

    private final List<IGridTable> hcHeaders = new ArrayList<>();
    private IGridTable retTable;
    private DTScale scale;

    IGridTable convertTable(ILogicalTable table) throws OpenLCompilationException {
        ILogicalTable headerRow = table.getRow(HEADER_ROW);

        int firstLookupColumn = findFirstLookupColumn(headerRow);
        loadHorizConditionsAndReturnColumns(headerRow, firstLookupColumn);
        validateLookupSection();

        processHorizConditionsHeaders(table, firstLookupColumn);

        IGridTable lookupValuesTable = getLookupValuesTable(table, firstLookupColumn).getSource();

        Integer lookupValuesTableHeight = getLookupValuesTableHeight(table);

        isMultiplier(lookupValuesTable);

        CoordinatesTransformer transformer = getTransformer(table,
            headerRow,
            lookupValuesTable,
            lookupValuesTableHeight);

        return new TransformedGridTable(table.getSource(), transformer);
    }

    private CoordinatesTransformer getTransformer(ILogicalTable table,
            ILogicalTable headerRow,
            IGridTable lookupValuesTable,
            Integer lookupValuesTableHeight) throws OpenLCompilationException {
        validateRetColumn(headerRow);
        int retTableWidth = retTable.getCell(0, 0).getWidth();

        if (lookupValuesTableHeight == null) {
            lookupValuesTableHeight = lookupValuesTable.getHeight();
        }

        this.scale = new DTScale(lookupValuesTableHeight, lookupValuesTable.getWidth() / retTableWidth);

        return new LookupHeadersTransformer(table.getSource(),
            lookupValuesTable,
            retTableWidth,
            firstVerticalColumn(headerRow),
            buildHorizontalHeaderOffsets(headerRow));
    }

    private int firstVerticalColumn(ILogicalTable headerRow) {
        for (int columnIndex = 0; columnIndex < headerRow.getSource().getWidth(); columnIndex++) {
            String headerStr = headerRow.getSource().getCell(columnIndex, 0).getStringValue();
            if (headerStr != null) {
                headerStr = headerStr.toUpperCase();
                if (DecisionTableHelper.isValidRetHeader(headerStr) || DecisionTableHelper
                    .isValidCRetHeader(headerStr) || DecisionTableHelper.isValidHConditionHeader(headerStr)) {
                    return columnIndex;
                }
            }
        }
        throw new IllegalStateException("Unexpected table structure");
    }

    private int[] buildHorizontalHeaderOffsets(ILogicalTable headerRow) {
        List<Integer> hcOffsets = new ArrayList<>();
        List<Integer> retOffsets = new ArrayList<>();
        int columnIndex = 0;
        while (columnIndex < headerRow.getSource().getWidth()) {
            ICell cell = headerRow.getSource().getCell(columnIndex, 0);
            String headerStr = cell.getStringValue();
            if (headerStr != null) {
                headerStr = headerStr.toUpperCase();
                if (DecisionTableHelper.isValidHConditionHeader(headerStr)) {
                    hcOffsets.add(columnIndex);
                } else if (DecisionTableHelper.isValidRetHeader(headerStr) || DecisionTableHelper
                    .isValidCRetHeader(headerStr)) {
                    retOffsets.add(columnIndex);
                }
            }
            columnIndex = columnIndex + cell.getWidth();
        }
        hcOffsets.addAll(retOffsets);
        return hcOffsets.stream().mapToInt(Integer::intValue).toArray();
    }

    private void validateRetColumn(ILogicalTable headerRow) throws OpenLCompilationException {
        int ncol = headerRow.getSource().getWidth();
        for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
            String headerStr = headerRow.getSource().getCell(columnIndex, 0).getStringValue();
            if (headerStr != null) {
                headerStr = headerStr.toUpperCase();
                if (DecisionTableHelper.isValidRetHeader(headerStr) || DecisionTableHelper
                    .isValidCRetHeader(headerStr)) {
                    return;
                }
            }
        }
        throw new OpenLCompilationException("RET or CRET column is mandatory for Lookup table.");
    }

    private void processHorizConditionsHeaders(ILogicalTable originalTable,
            int firstLookupColumn) throws OpenLCompilationException {
        ILogicalTable hcRowTable = originalTable.getRows(DISPLAY_ROW).getRow(0);
        int w = 0;
        int c = 0;
        while (w < firstLookupColumn) {
            w = w + hcRowTable.getColumn(c).getSource().getWidth();
            c++;
        }
        ILogicalTable hcHeaderTable = hcRowTable.getSubtable(c, 0, hcRowTable.getWidth() - c, hcRowTable.getHeight());
        validateHCHeaders(hcHeaderTable);
    }

    private ILogicalTable getLookupValuesTable(ILogicalTable originalTable, int firstLookupColumn) {
        int w = 0;
        int c = 0;
        while (w < firstLookupColumn) {
            w = w + originalTable.getColumn(c).getSource().getWidth();
            c++;
        }
        ILogicalTable hcRowTable = originalTable.getRows(DISPLAY_ROW).getRow(0);
        return originalTable.getSubtable(c,
            DISPLAY_ROW + hcRowTable.getHeight(),
            originalTable.getWidth() - c,
            originalTable.getHeight() - hcRowTable.getHeight() - DISPLAY_ROW);
    }

    private Integer getLookupValuesTableHeight(ILogicalTable originalTable) {
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
        int retTableWidth = retTable.getWidth();
        int lookupTableWidth = lookupValuesTable.getWidth();

        boolean isMultiplier = lookupTableWidth % retTableWidth == 0;
        // lookupTableWidth/retTableWidth*retTableWidth == lookupTableWidth;

        if (!isMultiplier) {
            int w = getWidthWithIgnoredEmptyCells(lookupValuesTable);
            isMultiplier = w % retTableWidth == 0;
            if (!isMultiplier) {
                String message = String.format(
                    "The width of the Lookup table(%d) is not a multiple of the RET width(%d).",
                    lookupTableWidth,
                    retTableWidth);
                throw new OpenLCompilationException(message);
            }
        }
    }

    private void validateHCHeaders(ILogicalTable hcHeaderTable) throws OpenLCompilationException {
        if (hcHeaders.size() != hcHeaderTable.getSource().getHeight()) {
            throw new OpenLCompilationException(
                "The height of the horizontal keys must be equal to the number of the horizontal headers.");
        }
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
        int ncol = headerRow.getSource().getWidth();

        for (int columnIndex = 0; columnIndex < ncol; columnIndex++) {
            String headerStr = headerRow.getSource().getColumn(columnIndex).getCell(0, 0).getStringValue();

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
        throw new OpenLCompilationException("Horizontal condition is mandatory for Lookup table.");
    }

    private void loadHorizConditionsAndReturnColumns(ILogicalTable rowHeader,
            int firstLookupColumn) throws OpenLCompilationException {

        int nCol = rowHeader.getSource().getWidth();
        int d = 0;
        while (d < nCol) {
            IGridTable hTable = rowHeader.getSource().getColumn(d);
            if (d >= firstLookupColumn) {
                String headerStr = hTable.getCell(0, 0).getStringValue();
                if (headerStr != null) {
                    headerStr = headerStr.toUpperCase();

                    if (DecisionTableHelper.isValidHConditionHeader(headerStr)) {
                        if (hTable.getWidth() != 1) {
                            throw new OpenLCompilationException("Column HC must have width = 1.");
                        }
                        hcHeaders.add(hTable);
                    } else if (DecisionTableHelper.isValidRetHeader(headerStr) || DecisionTableHelper
                        .isValidCRetHeader(headerStr)) {
                        if (retTable != null) {
                            throw new OpenLCompilationException("Only one RET column is allowed for Lookup table.");
                        }
                        retTable = hTable;
                    } else {
                        throw new OpenLCompilationException(
                            "Lookup Table allows only HC or RET or CRET columns after vertical conditions: " + headerStr);
                    }
                }
            }
            d = d + hTable.getCell(0, 0).getWidth();
        }
    }

    private void validateLookupSection() throws OpenLCompilationException {
        if (hcHeaders.isEmpty()) {
            String message = String.format("Horizontal Condition (%s1) is mandatory for Lookup table.",
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
