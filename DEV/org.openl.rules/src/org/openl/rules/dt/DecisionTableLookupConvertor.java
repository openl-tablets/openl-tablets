package org.openl.rules.dt;

import static org.openl.rules.table.GridTableUtils.isSameMergedCell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.table.CoordinatesTransformer;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.TransformedGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.util.ParserUtils;
import org.openl.util.StringUtils;

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
 * <p>
 * Each horizontal condition takes one row of the horizontal keys, or the rows its keys are merged down over. A key
 * merged across several columns applies to each of them.
 * <p>
 * A table that returns an array, rather than collects one, from a one-column <b>RET</b> returns the values under a key
 * as one array: a key merged across several columns returns the values of all of them. Keys of different widths
 * return arrays of different sizes.
 */

@RequiredArgsConstructor
public class DecisionTableLookupConvertor {

    private static final int HEADER_ROW = 0;
    private static final int CODE_ROW = 1;
    private static final int PARAMETER_ROW = 2;
    private static final int DISPLAY_ROW = 3;

    /**
     * Whether the headers are written in the table, so a title cell crossing into the horizontal conditions is a
     * mistake of the user. Generated headers of Smart and Simple tables report a mismatch of titles instead.
     */
    private final boolean explicitHeaders;
    /** Whether the table returns an array: a return written with neither a parameter nor an expression takes it. */
    private final boolean tableReturnsArray;
    private final @NonNull List<@NonNull IGridTable> hcHeaders = new ArrayList<>();
    private @Nullable IGridTable retTable;
    /** The column of the return header. */
    private int retColumn;
    /** Whether the return header collects what the rules return, as a CRET column does. */
    private boolean retCollects;
    @Getter
    private DTScale scale;

    IGridTable convertTable(ILogicalTable table) throws OpenLCompilationException {
        var headerRow = table.getRow(HEADER_ROW);

        var firstLookupColumn = findFirstLookupColumn(headerRow);
        if (explicitHeaders && firstLookupColumn == 0) {
            // The title of the first column tells the rows of the horizontal keys
            throw new OpenLCompilationException("Vertical condition is mandatory for Lookup table.");
        }
        loadHorizConditionsAndReturnColumns(headerRow, firstLookupColumn);
        validateLookupSection();
        var retTableWidth = retTable.getCell(0, 0).getWidth();

        var keys = processHorizConditionsHeaders(table, firstLookupColumn);
        var hConditionRows = findHConditionRows(keys, hcHeaders.size());

        var lookupValuesTable = getLookupValuesTable(table, firstLookupColumn).getSource();

        var lookupValuesTableHeight = getLookupValuesTableHeight(table);

        isMultiplier(lookupValuesTable, retTableWidth);

        var transformer = getTransformer(table,
                headerRow,
                keys,
                hConditionRows,
                lookupValuesTable,
                lookupValuesTableHeight,
                retTableWidth);

        return new TransformedGridTable(table.getSource(), transformer);
    }

    private @NonNull CoordinatesTransformer getTransformer(@NonNull ILogicalTable table,
                                                           @NonNull ILogicalTable headerRow,
                                                           @NonNull IGridTable keys,
                                                           int @NonNull [] hConditionRows,
                                                           @NonNull IGridTable lookupValuesTable,
                                                           @Nullable Integer lookupValuesTableHeight,
                                                           int retTableWidth) throws OpenLCompilationException {
        validateRetColumn(headerRow);

        if (lookupValuesTableHeight == null) {
            lookupValuesTableHeight = lookupValuesTable.getHeight();
        }

        var groupStarts = retTableWidth == 1 && returnsArray(table.getSource())
                ? keyGroupStarts(keys, hConditionRows)
                : fixedGroupStarts(lookupValuesTable.getWidth(), retTableWidth);
        this.scale = new DTScale(lookupValuesTableHeight, groupStarts.length - 1);

        return new LookupHeadersTransformer(table.getSource(),
                lookupValuesTable,
                retTableWidth,
                hConditionRows,
                groupStarts,
                firstVerticalColumn(headerRow),
                buildHorizontalHeaderOffsets(headerRow));
    }

    /**
     * Whether the return takes an array: its parameter is declared as an array, or, with neither a parameter nor an
     * expression written, the table returns one.
     *
     * <p>A return that collects, a CRET column, keeps the columns apart, and so does a return with an expression and no
     * parameter: the type of its value is not known yet.
     */
    private boolean returnsArray(@NonNull IGridTable source) {
        if (retCollects) {
            return false;
        }
        var parameter = source.getCell(retColumn, PARAMETER_ROW).getStringValue();
        if (StringUtils.isNotBlank(parameter)) {
            return declaresArray(parameter);
        }
        return tableReturnsArray && StringUtils.isBlank(source.getCell(retColumn, CODE_ROW).getStringValue());
    }

    /**
     * Whether a parameter declaration, such as {@code Double[] rates}, declares an array.
     *
     * <p>The brackets of the generic arguments of the type, such as in {@code List<Double[]> rates}, do not count.
     */
    private static boolean declaresArray(@NonNull String declaration) {
        var type = declaration.replaceAll("\\s", "");
        return type.substring(type.lastIndexOf('>') + 1).contains("[]");
    }

    /**
     * Splits the lookup values into groups as wide as the return header.
     *
     * @return the first column of each group, and the end of the last group
     */
    private static int @NonNull [] fixedGroupStarts(int width, int retTableWidth) {
        return IntStream.rangeClosed(0, width / retTableWidth)
                .map(group -> group * retTableWidth)
                .toArray();
    }

    /**
     * Splits the lookup values into groups under the keys of the horizontal conditions: a group ends where a key of
     * any horizontal condition ends, so a key merged over several columns makes one group of them.
     *
     * @return the first column of each group, and the end of the last group
     */
    private static int @NonNull [] keyGroupStarts(@NonNull IGridTable keys, int @NonNull [] hConditionRows) {
        var width = keys.getWidth();
        return IntStream.rangeClosed(0, width)
                .filter(column -> column == 0 || column == width || startsKey(keys, hConditionRows, column))
                .toArray();
    }

    /** Whether a key of some horizontal condition starts at the column. */
    private static boolean startsKey(@NonNull IGridTable keys, int @NonNull [] hConditionRows, int column) {
        return IntStream.of(hConditionRows)
                .anyMatch(row -> !isSameMergedCell(keys.getCell(column - 1, row), keys.getCell(column, row)));
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

    /**
     * Finds the horizontal keys: the rows under the headers in the columns of the horizontal conditions.
     *
     * <p>A table with explicit headers is checked for a title cell that spans both vertical and horizontal conditions.
     */
    private @NonNull IGridTable processHorizConditionsHeaders(@NonNull ILogicalTable originalTable,
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
        if (hcHeaderTable == null) {
            throw keysHeightError();
        }
        return hcHeaderTable.getSource();
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

    /** The width of the lookup values without the columns at their end that are empty in every row. */
    private static int getWidthWithIgnoredEmptyCells(IGridTable table) {
        var width = table.getWidth();
        while (width > 0) {
            for (var i = 0; i < table.getHeight(); i++) {
                if (table.getCell(width - 1, i).getStringValue() != null) {
                    return width;
                }
            }
            width--;
        }

        return 0;
    }

    /**
     * Checks that the lookup values fill whole groups as wide as the return header.
     *
     * <p>Columns at the end of the values that are empty in every row are left out.
     *
     * @throws OpenLCompilationException when the last group is narrower than the return header
     */
    private static void isMultiplier(@NonNull IGridTable lookupValuesTable, int retTableWidth)
            throws OpenLCompilationException {
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

    /**
     * Finds the row of the horizontal keys each horizontal condition starts at.
     *
     * <p>A horizontal condition takes one row of the keys, or several rows when every key of it is merged down over
     * them; a column empty in every row of the keys is left aside. A row where a key is not merged with the key above
     * it starts a condition of its own, as without a merge.
     *
     * <p>Keys that take as many rows as there are horizontal conditions give each row to a condition of its own,
     * whatever is merged down, as they did before merged keys took several rows.
     *
     * @return the row each horizontal condition starts at
     * @throws OpenLCompilationException when the keys make more or fewer rows than there are horizontal conditions
     */
    private static int @NonNull [] findHConditionRows(@NonNull IGridTable keys, int hConditions)
            throws OpenLCompilationException {
        var rows = IntStream.range(0, keys.getHeight())
                .filter(row -> row == 0 || !continuesRowAbove(keys, row))
                .toArray();
        if (rows.length == hConditions) {
            return rows;
        } else if (keys.getHeight() == hConditions) {
            return IntStream.range(0, hConditions).toArray();
        }
        throw keysHeightError();
    }

    /**
     * Whether the row of the keys belongs to the horizontal condition above it: every key of the row is merged with
     * the key above it, a column empty in every row of the keys aside.
     */
    private static boolean continuesRowAbove(@NonNull IGridTable keys, int row) {
        return IntStream.range(0, keys.getWidth())
                .allMatch(column -> isSameMergedCell(keys.getCell(column, row - 1), keys.getCell(column, row))
                        || isEmptyColumn(keys, column));
    }

    private static boolean isEmptyColumn(@NonNull IGridTable keys, int column) {
        return IntStream.range(0, keys.getHeight())
                .allMatch(row -> StringUtils.isBlank(keys.getCell(column, row).getStringValue()));
    }

    private static @NonNull OpenLCompilationException keysHeightError() {
        return new OpenLCompilationException(
                "The height of the horizontal keys must be equal to the number of the horizontal headers.");
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
                loadHorizConditionOrReturnColumn(hTable, d);
            }
            d = d + hTable.getCell(0, 0).getWidth();
        }
    }

    private void loadHorizConditionOrReturnColumn(@NonNull IGridTable hTable,
                                                  int column) throws OpenLCompilationException {
        var headerStr = hTable.getCell(0, 0).getStringValue();
        if (headerStr != null) {
            headerStr = headerStr.toUpperCase();

            if (DecisionTableHelper.isValidHConditionHeader(headerStr)) {
                if (hTable.getCell(0, 0).getWidth() != 1) {
                    throw new OpenLCompilationException("Column HC must have width = 1.");
                }
                hcHeaders.add(hTable);
            } else if (DecisionTableHelper.isValidRetHeader(headerStr) || DecisionTableHelper
                    .isValidCRetHeader(headerStr)) {
                if (retTable != null) {
                    throw new OpenLCompilationException("Only one RET column is allowed for Lookup table.");
                }
                retTable = hTable;
                retColumn = column;
                retCollects = DecisionTableHelper.isValidCRetHeader(headerStr);
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
