package org.openl.rules.dt;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.xls.XlsSheetGridHelper;

public class DecisionTableHelper {

    public static boolean looksLikeTransposed(ILogicalTable table) {

        if (table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return true;
        }

        if (table.getHeight() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return false;
        }

        int cnt1 = countConditionsAndActions(table);
        int cnt2 = countConditionsAndActions(table.transpose());

        if (cnt1 != cnt2) {
            return cnt1 > cnt2;
        }

        return table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
    }
    
    public static boolean isValidConditionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.CONDITION.getHeaderKey().charAt(0) 
            && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidActionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.ACTION.getHeaderKey().charAt(0) 
            && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidRetHeader(String s) {
        return s.length() >= 3 && s.startsWith(DecisionTableColumnHeaders.RETURN.getHeaderKey())
            && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    public static boolean isValidRuleHeader(String s) {
        return s.equals(DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    public static boolean isValidCommentHeader(String s) {
        return s.startsWith("//");
    }

    public static boolean isActionHeader(String s) {
        return isValidActionHeader(s) || isValidRetHeader(s);
    }

    public static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s);
    }

    public static int countConditionsAndActions(ILogicalTable table) {

        int width = table.getWidth();
        int count = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();
                count += isValidConditionHeader(value) || isActionHeader(value) ? 1 : 0;
            }
        }

        return count;
    }
    
    /**
     * Checks if given table contain any horizontal condition header.
     * 
     * @param table
     * @return true if there is is any horizontal condition header in the table.
     */
    public static boolean hasHConditions(ILogicalTable table) {

        int width = table.getWidth();

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();

                if (isValidHConditionHeader(value)) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith(DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character.isDigit(headerStr.charAt(2));
    }


    private static int SIMPLE_DT_HEADERS_HEIGHT = 3;
    /**
     * Creates virtual headers for condition and return columns to load simple
     * lookup as usual Desicion Table
     * 
     * @param decisionTable method description fo simple lookup.
     * @param original The original body of simple lookup.
     * @param numberOfHcondition
     * @return prepared simple lookup table.
     */
    public static ILogicalTable preprocessSimpleLoolup(DecisionTable decisionTable, ILogicalTable original,
            int numberOfHcondition) {
        IWritableGrid virtualGrid = createVirtualGrid();
        writeConditionAndReturnHeadersForSimpleLookup(virtualGrid, original, decisionTable, numberOfHcondition);
        GridTable virtualGridTable = new GridTable(0, 0, SIMPLE_DT_HEADERS_HEIGHT - 1, original.getSource().getWidth() - 1, virtualGrid);
        IGrid grid = new CompositeGrid(new IGridTable[] { virtualGridTable, original.getSource() }, true);
        return LogicalTableHelper.logicalTable(new GridTable(0, 0, original.getSource().getHeight()
                + SIMPLE_DT_HEADERS_HEIGHT - 1, original.getSource().getWidth() - 1, grid));
    }

    private static void writeConditionAndReturnHeadersForSimpleLookup(IWritableGrid grid, ILogicalTable original, DecisionTable decisionTable,
            int numberOfHcondition) {
        int numberOfConditions = decisionTable.getSignature().getNumberOfParameters();
        int column = 0;
        for (int i = 0; i < numberOfConditions; i++) {
            //write headers
            if (i < numberOfConditions - numberOfHcondition) {
                grid.setCellValue(column, 0, "C" + (i + 1));
            } else {
                grid.setCellValue(column, 0, "HC" + (i + 1));
            }
            grid.setCellValue(column, 1, decisionTable.getSignature().getParameterName(i));
            
            //merge columns
            int mergedColumnsCounts = original.getColumnWidth(i);
            if (mergedColumnsCounts > 1) {
                for (int row = 0; row < SIMPLE_DT_HEADERS_HEIGHT; row++) {
                    grid.addMergedRegion(new GridRegion(row, column, row, column + mergedColumnsCounts - 1));
                }
            }
            column += mergedColumnsCounts;
        }
        grid.setCellValue(column, 0, "RET1");
        int mergedColumnsCounts = original.getColumnWidth(numberOfConditions);
        if (mergedColumnsCounts > 1) {
            for (int row = 0; row < SIMPLE_DT_HEADERS_HEIGHT; row++) {
                grid.addMergedRegion(new GridRegion(row, column, row, column + mergedColumnsCounts - 1));
            }
        }
    }
    /**
     * Creates not-existing virtual grid.
     * 
     * @return virtual {@link IWritableGrid}.
     */
    public static IWritableGrid createVirtualGrid() {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();
        return XlsSheetGridHelper.createVirtualGrid(sheet);
    }

    public static boolean isSimpleDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        if (IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        if (IXlsTableNames.SIMPLE_DECISION_LOOKUP.equals(dtType)) {
            return true;
        } else {
            return false;
        }
    }
}
