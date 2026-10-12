package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.TransformedGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.util.ParserUtils;

/**
 * Lookup table is a decision table that is created by transforming lookup tables to create a single-column return
 * value.<br>
 * <br>
 * <p>
 * The lookup values could appear either left of the lookup table or on top of it.<br>
 * <br>
 * <p>
 * The values on the left will be called <b>"vertical"</b> and values on top will be called <b>"horizontal"</b>.<br>
 * <br>
 * <p>
 * The table should have at least one vertical condition column, it can have the Rule column, it (in theory) might have
 * vertical Actions which will be processed the same way as vertical conditions, it must have one or more Horizontal
 * Conditions, and exactly one (optional in the future release) <b>RET</b> or <b>CRET</b> column<br>
 * . <br>
 * <b>RET</b> or <b>CRET</b> section can be placed in any place of lookup headers row, after vertical conditions (for
 * users convenience).
 * <p>
 * The Horizontal Conditions will be marked <b>HC1</b>, <b>HC2</b> etc. The first HC column or RET column will mark the
 * starting column of the lookup matrix
 */

@RequiredArgsConstructor
public class DecisionTableLookupConvertor {

    private static final int HEADER_ROW = 0;
    private static final int DISPLAY_ROW = 3;

    /**
     * Whether the headers are written in the table, so a title cell crossing into the horizontal conditions is a
     * mistake of the user. Generated headers of Smart and Simple tables report a mismatch of titles instead.
     */
    private final boolean explicitHeaders;
    private final List<IGridTable> hcHeaders = new ArrayList<>();
    private IGridTable retTable;
    @Getter
    private DTScale scale;

    IGridTable convertTable(ILogicalTable table) throws OpenLCompilationException {
        var headerRow = table.getRow(HEADER_ROW);

        var firstLookupColumn = findFirstLookupColumn(headerRow);
        loadHorizConditionsAndReturnColumns(headerRow, firstLookupColumn);
        validateLookupSection();

        processHorizConditionsHeaders(table, firstLookupColumn);

        var lookupValuesTable = getLookupValuesTable(table, firstLookupColumn).getSource();

        var lookupValuesTableHeight = getLookupValuesTableHeight(table);

        isMultiplier(lookupValuesTable);

        var transformer = getTransformer(table,
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
        var retTableWidth = retTable.getCell(0, 0).getWidth();

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
        for (var columnIndex = 0; columnIndex < headerRow.getSource().getWidth(); columnIndex++) {
            var headerStr = headerRow.getSource().getCell(columnIndex, 0).getStringValue();
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
        var hcOffsets = new ArrayList<Integer>();
        var retOffsets = new ArrayList<Integer>();
        var columnIndex = 0;
        while (columnIndex < headerRow.getSource().getWidth()) {
            var cell = headerRow.getSource().getCell(columnIndex, 0);
            var headerStr = cell.getStringValue();
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
        var ncol = headerRow.getSource().getWidth();
        for (var columnIndex = 0; columnIndex < ncol; columnIndex++) {
            var headerStr = headerRow.getSource().getCell(columnIndex, 0).getStringValue();
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
        var hcRowTable = originalTable.getRows(DISPLAY_ROW).getRow(0);
        if (explicitHeaders) {
            validateTitleCells(originalTable.getRow(HEADER_ROW).getSource(), hcRowTable.getSource(), firstLookupColumn);
        }
        var w = 0;
        var c = 0;
        while (w < firstLookupColumn) {
            w = w + hcRowTable.getColumn(c).getSource().getWidth();
            c++;
        }
        var hcHeaderTable = hcRowTable.getSubtable(c, 0, hcRowTable.getWidth() - c, hcRowTable.getHeight());
        validateHCHeaders(hcHeaderTable);
    }

    /**
     * Rejects a merged cell of the title rows that starts in a vertical condition column and goes into the horizontal
     * conditions.
     *
     * <p>The error points to that cell and names the two headers it spans.
     */
    private static void validateTitleCells(IGridTable headers,
                                           IGridTable titles,
                                           int firstLookupColumn) throws SyntaxNodeException {
        for (var row = 0; firstLookupColumn > 0 && row < titles.getHeight(); row++) {
            if (isSameMergedCell(titles.getCell(firstLookupColumn - 1, row), titles.getCell(firstLookupColumn, row))) {
                throw crossingCellError(headers, titles, firstLookupColumn, row);
            }
        }
    }

    private static SyntaxNodeException crossingCellError(IGridTable headers,
                                                         IGridTable titles,
                                                         int firstLookupColumn,
                                                         int row) {
        var start = firstLookupColumn - 1;
        while (start > 0 && isSameMergedCell(titles.getCell(start - 1, row), titles.getCell(start, row))) {
            start--;
        }
        var end = firstLookupColumn;
        while (end + 1 < titles.getWidth()
                && isSameMergedCell(titles.getCell(end, row), titles.getCell(end + 1, row))) {
            end++;
        }
        var message = ("The cell spans the condition column '%s' and the horizontal condition column '%s'. "
                + "A title cell must not span both vertical and horizontal conditions.").formatted(
                headerTitle(headers, start),
                headerTitle(headers, firstLookupColumn));
        var cell = titles.getSubtable(start, row, end - start + 1, 1);
        return SyntaxNodeExceptionUtils.createError(message, new GridCellSourceCodeModule(cell));
    }

    private static String headerTitle(IGridTable headers, int column) {
        var cell = headers.getCell(column, 0);
        var topLeft = cell.getTopLeftCellFromRegion();
        return (topLeft == null ? cell : topLeft).getStringValue();
    }

    private static boolean isSameMergedCell(ICell left, ICell right) {
        var leftRegion = left.getRegion();
        var rightRegion = right.getRegion();
        return leftRegion != null && rightRegion != null
                && leftRegion.getTop() == rightRegion.getTop()
                && leftRegion.getLeft() == rightRegion.getLeft();
    }

    private ILogicalTable getLookupValuesTable(ILogicalTable originalTable, int firstLookupColumn) {
        var w = 0;
        var c = 0;
        while (w < firstLookupColumn) {
            w = w + originalTable.getColumn(c).getSource().getWidth();
            c++;
        }
        var hcRowTable = originalTable.getRows(DISPLAY_ROW).getRow(0);
        return originalTable.getSubtable(c,
                DISPLAY_ROW + hcRowTable.getHeight(),
                originalTable.getWidth() - c,
                originalTable.getHeight() - hcRowTable.getHeight() - DISPLAY_ROW);
    }

    private Integer getLookupValuesTableHeight(ILogicalTable originalTable) {
        var stringValue = originalTable.getCell(0, 0).getStringValue();
        if (stringValue == null) {
            stringValue = "";
        }
        stringValue = stringValue.toUpperCase();
        var valueTable = originalTable.getRows(DISPLAY_ROW + 1);
        if (DecisionTableHelper.isValidRuleHeader(stringValue) || DecisionTableHelper
                .isValidMergedConditionHeader(stringValue)) {
            return valueTable.getHeight();
        } else {
            return null;
        }
    }

    private int getWidthWithIgnoredEmptyCells(IGridTable table) {
        var width = table.getWidth();
        while (width > 0) {
            for (var i = 0; i < table.getHeight(); i++) {
                if (table.getCell(width - 1, 0).getStringValue() != null) {
                    return width;
                }
            }
            width--;
        }

        return 0;
    }

    private void isMultiplier(IGridTable lookupValuesTable) throws OpenLCompilationException {
        var retTableWidth = retTable.getWidth();
        var lookupTableWidth = lookupValuesTable.getWidth();

        var isMultiplier = lookupTableWidth % retTableWidth == 0;

        if (!isMultiplier) {
            var w = getWidthWithIgnoredEmptyCells(lookupValuesTable);
            isMultiplier = w % retTableWidth == 0;
            if (!isMultiplier) {
                var message = "The width of the Lookup table(%d) is not a multiple of the RET width(%d).".formatted(
                        lookupTableWidth,
                        retTableWidth);
                throw new OpenLCompilationException(message);
            }
        }
    }

    private void validateHCHeaders(ILogicalTable hcHeaderTable) throws OpenLCompilationException {
        if (hcHeaderTable == null || hcHeaders.size() != hcHeaderTable.getSource().getHeight()) {
            throw new OpenLCompilationException(
                    "The height of the horizontal keys must be equal to the number of the horizontal headers.");
        }
    }

    /**
     * @param headerRow row with lookup table headers. For example:
     *                  <table cellspacing="2">
     *                  <tr>
     *                  <td align="center" bgcolor="#8FCB52"><b>C1</b></td>
     *                  <td align="center" bgcolor="#8FCB52"><b>C2</b></td>
     *                  <td align="center" bgcolor="#8FCB52"><b>HC1</b></td>
     *                  <td align="center" bgcolor="#8FCB52"><b>HC2</b></td>
     *                  <td align="center" bgcolor="#8FCB52"><b>RET1</b></td>
     *                  </tr>
     *                  </table>
     *                  In this case the return will be <code>2</code>.
     * @return NOTE!!! it returns an index of logical column!
     * @throws OpenLCompilationException when there is no lookup headers.
     */
    private int findFirstLookupColumn(ILogicalTable headerRow) throws OpenLCompilationException {
        var ncol = headerRow.getSource().getWidth();

        for (var columnIndex = 0; columnIndex < ncol; columnIndex++) {
            var headerStr = headerRow.getSource().getColumn(columnIndex).getCell(0, 0).getStringValue();

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

        var nCol = rowHeader.getSource().getWidth();
        var d = 0;
        while (d < nCol) {
            var hTable = rowHeader.getSource().getColumn(d);
            if (d >= firstLookupColumn) {
                loadHorizConditionOrReturnColumn(hTable);
            }
            d = d + hTable.getCell(0, 0).getWidth();
        }
    }

    private void loadHorizConditionOrReturnColumn(IGridTable hTable) throws OpenLCompilationException {
        var headerStr = hTable.getCell(0, 0).getStringValue();
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

    private void validateLookupSection() throws OpenLCompilationException {
        if (hcHeaders.isEmpty()) {
            var message = "Horizontal Condition (%s1) is mandatory for Lookup table.".formatted(
                    DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
            throw new OpenLCompilationException(message);
        }

        if (retTable == null) {
            var message = "Lookup Table must have %s column".formatted(
                    DecisionTableColumnHeaders.RETURN.getHeaderKey());
            throw new OpenLCompilationException(message);
        }

    }

}
