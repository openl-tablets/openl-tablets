package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
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
 * The table should have at least one vertical condition column, it can have
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

    public IGridTable convertTable(ILogicalTable table) throws OpenLCompilationException {

        IGrid grid = table.getGridTable().getGrid();
        ILogicalTable originaltable = LogicalTableHelper.logicalTable(table);

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
        
        
        // check lookupTable width is multiple of  RET column width
        
        if (retTable == null)
        {
            String message = "There must be one RET column in a lookup table";
            throw new OpenLCompilationException(message);
        }    
        
        int retTableWidth = retTable.getGridTable().getGridWidth();
        int lookupTableWidth = lookupValuesTable.getGridWidth();
        
        boolean isMultiplier = lookupTableWidth % retTableWidth == 0; 
//            lookupTableWidth/retTableWidth*retTableWidth == lookupTableWidth;
        
        if (!isMultiplier) {
            String message = String.format("The width of the lookup table(%d) is not a multiple of the RET width(%d)", lookupTableWidth, retTableWidth);
            throw new OpenLCompilationException(message);            
        }    
        

        return new TransformedGridTable(table.getGridTable(), 
            new TwoDimensionDecisionTableTranformer(table.getGridTable(), lookupValuesTable, retTable.getGridTable())).asGridTable();
    }

    private void validateHCHeaders(ILogicalTable hcHeaderTable) throws OpenLCompilationException {

        String message = String.format("The width of the horizontal keys must be equal to the number of the %s headers", 
            DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
        assertEQ(hcHeaders.size(),
            hcHeaderTable.getGridTable().getLogicalHeight(),
            message);
    }

    private void assertEQ(int v1, int v2, String message) throws OpenLCompilationException {

        if (v1 == v2)
            return;

        throw new OpenLCompilationException(message);
    }

    private int parseAndValidateLookupHeaders(ILogicalTable headerRow) throws OpenLCompilationException {

        int ncol = headerRow.getLogicalWidth();

        for (int i = 0; i < ncol; i++) {

            String headerStr = headerRow.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (headerStr == null) {
                continue;
            }

            headerStr = headerStr.toUpperCase();

            if (DecisionTableHelper.isValidRuleHeader(headerStr) || 
                    DecisionTableHelper.isValidConditionHeader(headerStr) || 
                    DecisionTableHelper.isValidCommentHeader(headerStr)) {
                continue;
            }

            loadHorizConditionsAndReturnColumns(headerRow, i);

            return i;
        }

        throw new OpenLCompilationException("Lookup table must have at least one horizontal condition");
    }

    private void loadHorizConditionsAndReturnColumns(ILogicalTable rowHeader, int i) throws OpenLCompilationException {

        int ncol = rowHeader.getLogicalWidth();

        for (; i < ncol; i++) {

            ILogicalTable htable = rowHeader.getLogicalColumn(i);
            String headerStr = htable.getGridTable().getCell(0, 0).getStringValue();

            if (headerStr == null) {
                continue;
            }

            headerStr = headerStr.toUpperCase();

            if (DecisionTableHelper.isValidHConditionHeader(headerStr)) {
                if (retTable != null) {
                    throw new OpenLCompilationException(String.format("%s column must be the last one", 
                        DecisionTableColumnHeaders.RETURN.getHeaderKey()));
                }

                hcHeaders.add(htable);
                assertTableWidth(1, htable, DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey());
                continue;
            }

            if (DecisionTableHelper.isValidRetHeader(headerStr)) {

                if (retTable != null) {
                    throw new OpenLCompilationException(String.format("Lookup Table can have only one %s column", 
                        DecisionTableColumnHeaders.RETURN.getHeaderKey()));
                }

//                assertTableWidth(1, htable, DecisionTableColumnHeaders.RETURN.getHeaderKey());
                retTable = htable;
                continue;
            }

            String message = String.format("Lookup Table allow here only %s or %s columns: %s", 
                DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey(), DecisionTableColumnHeaders.RETURN.getHeaderKey(), headerStr);
            throw new OpenLCompilationException(message);
        }

        if (hcHeaders.size() == 0) {
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

    private void assertTableWidth(int w, ILogicalTable htable, String type) throws OpenLCompilationException {
        if (htable.getGridTable().getGridWidth() == w) {
            return;
        }

        throw new OpenLCompilationException(String.format("Column %s must have width=%s", type, w));
    }

}
