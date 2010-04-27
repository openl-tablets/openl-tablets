package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.TransformedGridTable;

/**
 * Lookup table is a decision table that is created by transforming lookup
 * tables to create a single-column return value.
 * 
 * The lookup values could appear either left of the lookup table or on top of
 * it.
 * 
 * The values on the left will be called "vertical" and values on top will be
 * called "horizontal".
 * 
 * 
 * The table should have at least one vertical condition column, it can not have
 * the Rule column, it (in theory) might have vertical Actions which will be
 * processed the same way as vertical conditions, it must have one or more
 * Horizontal Conditions, and exactly one (optional in the future release) RET
 * column
 * 
 * The Horizontal Conditions will be marked HC1, HC2 etc. The first HC column
 * will mark the starting column of the lookup matrix
 */

public class DecisionTableLookupConvertor {

    public static final int HEADER_ROW = 0;
    public static final int EXPR_ROW = 1;
    public static final int PARAM_ROW = 2;
    public static final int DISPLAY_ROW = 3;

    private List<ILogicalTable> hcHeaders = new ArrayList<ILogicalTable>();
    private ILogicalTable retTable;

    public IGridTable convertTable(ILogicalTable table) throws Exception {

        IGrid grid = table.getGridTable().getGrid();
        ILogicalTable originaltable = LogicalTable.logicalTable(table);

        ILogicalTable headerRow = originaltable.getLogicalRow(HEADER_ROW);

        int firstLookupColumn = parseAndValidateLookupHeaders(headerRow);
        int firstLookupGridColumn = headerRow.getLogicalColumn(firstLookupColumn).getGridTable().getGridColumn(0, 0);

        // Find and validate horizontal condition keys
        //
        ILogicalTable tableWithDisplay = originaltable.rows(DISPLAY_ROW);

        ILogicalTable displayRow = tableWithDisplay.getLogicalRow(0);
        IGridRegion displayRowRegion = displayRow.getGridTable().getRegion();
        IGridRegion hcHeadersRegion = new GridRegion(displayRowRegion, IGridRegion.LEFT, firstLookupGridColumn);
        ILogicalTable hcHeaderTable = new GridTable(hcHeadersRegion, grid);

        validateHCHeaders(hcHeaderTable);

        // 2) lookup values
        //
        ILogicalTable valueTable = originaltable.rows(DISPLAY_ROW + 1);

        IGridRegion lookupValuesRegion = new GridRegion(valueTable.getGridTable().getRegion(),
            IGridRegion.LEFT,
            firstLookupGridColumn);

        IGridTable lookupValuesTable = new GridTable(lookupValuesRegion, grid);


        return new TransformedGridTable(table.getGridTable(), new TwoDimensionDecisionTableTranformer(table
                .getGridTable(), lookupValuesTable)).asGridTable();
    }

    private void validateHCHeaders(ILogicalTable hcHeaderTable) throws Exception {

        assertEQ(hcHeaders.size(),
            hcHeaderTable.getGridTable().getLogicalHeight(),
            "The width of the horizontal keys must be equal to the number of the HC headers");
    }

    private void assertEQ(int v1, int v2, String message) throws Exception {

        if (v1 == v2)
            return;

        throw new Exception(message);
    }

    private int parseAndValidateLookupHeaders(ILogicalTable headerRow) throws Exception {

        int ncol = headerRow.getLogicalWidth();

        for (int i = 0; i < ncol; i++) {

            String headerStr = headerRow.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (headerStr == null) {
                continue;
            }

            headerStr = headerStr.toUpperCase();

            if (DecisionTableHelper.isValidConditionHeader(headerStr) || DecisionTableHelper.isValidCommentHeader(headerStr)) {
                continue;
            }

            loadHCandRet(headerRow, i);

            return i;
        }

        throw new Exception("Lookup table must have at least one horizontal condition");
    }

    private void loadHCandRet(ILogicalTable rowHeader, int i) throws Exception {

        int ncol = rowHeader.getLogicalWidth();

        for (; i < ncol; i++) {

            ILogicalTable htable = rowHeader.getLogicalColumn(i);
            String headerStr = htable.getGridTable().getCell(0, 0).getStringValue();

            if (headerStr == null) {
                continue;
            }

            headerStr = headerStr.toUpperCase();

            if (isValidHConditionHeader(headerStr)) {
                if (retTable != null) {
                    throw new Exception("RET column must be the last one");
                }

                hcHeaders.add(htable);
                assertTableWidth(1, htable, "HC");
                continue;
            }

            if (DecisionTableHelper.isValidRetHeader(headerStr)) {

                if (retTable != null) {
                    throw new Exception("Lookup Table can have only one RET column");
                }

                assertTableWidth(1, htable, "RET");
                retTable = htable;
                continue;
            }

            throw new Exception("Lookup Table allow here only HC or RET columns: " + headerStr);
        }

        if (hcHeaders.size() == 0) {
            throw new Exception("Lookup Table must have at least one Horizontal Condition (HC1)");
        }

        if (retTable == null) {
            throw new Exception("Lookup Table must have RET column");
        }

    }

    private void assertTableWidth(int w, ILogicalTable htable, String type) throws Exception {
        if (htable.getGridTable().getGridWidth() == w) {
            return;
        }

        throw new Exception("Column " + type + " must have width=" + w);
    }

    public static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith("HC") && headerStr.length() > 2 && Character.isDigit(headerStr.charAt(2));
    }

}
