package org.openl.rules.dt;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.types.IOpenClass;

public class DecisionTableHelper {

    /**
     * Check if table is vertical.<br>
     * Vertical table is when conditions are represented from left to right, table is reading from top to bottom.</br> 
     * Example of vertical table:
     * 
     * <table cellspacing="2">
     * <tr>
     * <td align="center" bgcolor="#ccffff"><b>Rule</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C1</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C2</b></td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">paramLocal1==paramInc</td>
     * <td align="center" bgcolor="#ccffff">paramLocal2==paramInc</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">String paramLocal1</td>
     * <td align="center" bgcolor="#ccffff">String paramLocal2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule</td>
     * <td align="center" bgcolor="#ffff99">Local Param 1</td>
     * <td align="center" bgcolor="#ffff99">Local Param 2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule1</td>
     * <td align="center" bgcolor="#ffff99">value11</td>
     * <td align="center" bgcolor="#ffff99">value21</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule2</td>
     * <td align="center" bgcolor="#ffff99">value12</td>
     * <td align="center" bgcolor="#ffff99">value22</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule3</td>
     * <td align="center" bgcolor="#ffff99">value13</td>
     * <td align="center" bgcolor="#ffff99">value23</td>
     * </tr>
     * </table>
     * 
     * @param table checked table
     * @return <code>TRUE</code> if table is vertical.
     */
    public static boolean looksLikeVertical(ILogicalTable table) {

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
    
    public static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith(
            DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && 
            Character.isDigit(headerStr.charAt(2));
    }
    

    public static boolean isValidMergedConditionHeader(String headerStr) {
        return headerStr.startsWith(
                DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey()) && headerStr.length() > 2 && 
                Character.isDigit(headerStr.charAt(2));
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

    public static boolean isActionHeader(String s) {
        return isValidActionHeader(s) || isValidRetHeader(s);
    }

    public static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s) || isValidMergedConditionHeader(s);
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
     * @param table checked table
     * @return true if there is is any horizontal condition header in the table.
     */
    public static boolean hasHConditions(ILogicalTable table) {
    	return countHConditions(table) > 0;
    }
    
    
    /**
     * Creates virtual headers for condition and return columns to load simple
     * Decision Table as an usual Decision Table
     * 
     * @param decisionTable method description for simple Decision Table.
     * @param originalTable The original body of simple Decision Table.
     * @param numberOfHcondition The number of horizontal conditions. In SimpleRules it == 0 in SimpleLookups > 0
     * @return prepared usual Decision Table.
     */
    public static ILogicalTable preprocessSimpleDecisionTable(DecisionTable decisionTable, ILogicalTable originalTable,
            int numberOfHcondition) throws OpenLCompilationException {
        IWritableGrid virtualGrid = createVirtualGrid();
        writeVirtualHeadersForSimpleDecisionTable(virtualGrid, originalTable, decisionTable, numberOfHcondition);

        //If the new table header size bigger than the size of the old table we use the new table size
        int sizeOfVirtualGridTable = virtualGrid.getMaxColumnIndex(0) < originalTable.getSource().getWidth() ?
                originalTable.getSource().getWidth() - 1 : virtualGrid.getMaxColumnIndex(0) - 1;
        GridTable virtualGridTable = 
            new GridTable(0, 0, IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1, 
                    sizeOfVirtualGridTable/*originalTable.getSource().getWidth() - 1*/, virtualGrid);

        IGrid grid = new CompositeGrid(new IGridTable[] { virtualGridTable, originalTable.getSource() }, true);

        //If the new table header size bigger than the size of the old table we use the new table size
        int sizeofGrid = virtualGridTable.getWidth() < originalTable.getSource().getWidth() ?
                originalTable.getSource().getWidth() - 1 : virtualGridTable.getWidth() - 1;
                
        return LogicalTableHelper.logicalTable(new GridTable(0, 0, originalTable.getSource().getHeight()
                + IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1, 
                sizeofGrid /*originalTable.getSource().getWidth() - 1*/, grid));
    }

    private static void writeVirtualHeadersForSimpleDecisionTable(IWritableGrid grid, ILogicalTable originalTable,
            DecisionTable decisionTable, int numberOfHcondition) throws OpenLCompilationException {
        int columnsForConditions;

        // number of physical columns for conditions(including merged)
        //
        columnsForConditions = writeConditions(grid, originalTable, decisionTable, numberOfHcondition);

        // write return
        //
        writeReturn(grid, originalTable, decisionTable, columnsForConditions, numberOfHcondition > 0);
    }

    private static void writeReturn(IWritableGrid grid, ILogicalTable originalTable, DecisionTable decisionTable,
            int columnsForConditions, boolean isLookupTable) throws OpenLCompilationException {
        // write return column
        //
        grid.setCellValue(columnsForConditions, 0, (DecisionTableColumnHeaders.RETURN.getHeaderKey() + "1").intern());
        
        if (!isLookupTable) {
            if (originalTable.getWidth() > getNumberOfConditions(decisionTable)) {
                int mergedColumnsCounts = originalTable.getColumnWidth(getNumberOfConditions(decisionTable));

                if (mergedColumnsCounts > 1) {
                    for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                        grid.addMergedRegion(
                            new GridRegion(row, columnsForConditions, row, columnsForConditions + mergedColumnsCounts - 1));
                    }
                }
            } else {
                // if the physical number of columns for conditions is equals or more than whole width of the table,
                // means there is no return column.
                //
                throw new OpenLCompilationException("Wrong table structure: There is no column for return values");
            }
        }
    }

    private static int writeConditions(IWritableGrid grid, ILogicalTable originalTable, DecisionTable decisionTable,
            int numberOfHcondition) throws OpenLCompilationException {
        int numberOfConditions = getNumberOfConditions(decisionTable);
        int column = 0;
        int vColumnCounter = 0;
        
        for (int i = 0; i < numberOfConditions; i++) {
            if (column > originalTable.getWidth()) {
                String message = "Wrong table structure: Columns count is less than parameters count";
                throw new OpenLCompilationException(message);
            }
            // write headers
            //
            boolean isThatVCondition = i < numberOfConditions - numberOfHcondition;
            boolean lastCondition = i + 1 == numberOfConditions;

            if (isThatVCondition) {
                vColumnCounter++;
                // write simple condition
                //
                if (i == 0 && numberOfHcondition == 0 && numberOfConditions < 2){
                    grid.setCellValue(column, 0, (DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey() + (i + 1)).intern());
                }else{
                    grid.setCellValue(column, 0, (DecisionTableColumnHeaders.CONDITION.getHeaderKey() + (i + 1)).intern());
                }
            } else {
                // write horizontal condition
                //
                grid.setCellValue(column, 0, (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + (i + 1)).intern());
            }

            grid.setCellValue(column, 1, decisionTable.getSignature().getParameterName(i));

            //Set type of condition values(for Ranges and Array)
            grid.setCellValue(column, 2,
                    checkTypeOfValues(originalTable, i, 
                            decisionTable.getSignature().getParameterTypes()[i],
                            isThatVCondition, lastCondition, vColumnCounter) );

            //merge columns
            if (isThatVCondition || lastCondition) {
                int mergedColumnsCounts = isThatVCondition ? originalTable.getColumnWidth(i) : originalTable
                        .getSource().getCell(vColumnCounter, i - vColumnCounter).getWidth();

                if (mergedColumnsCounts > 1) {
                    for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                        grid.addMergedRegion(new GridRegion(row, column, row, column + mergedColumnsCounts - 1));
                    }
                }
    
                column += mergedColumnsCounts;
            } else {
                column++;
            }
        }
        return column;
    }

    

    /**
     * Check type of condition values. If condition values are complex(Range, Array) 
     * then types of complex values will be returned 
     * 
     * @param originalTable The original body of simple Decision Table.
     * @param column The number of a condition 
     * @param type The type of an input parameter
     * @param isThatVCondition If condition is vertical value = true
     * @param vColumnCounter Counter of vertical conditions. Needed for calculating 
     * position of horizontal condition
     * @return type of condition values
     */
    private static String checkTypeOfValues(ILogicalTable originalTable, int column, IOpenClass type,
            boolean isThatVCondition, boolean lastCondition, int vColumnCounter) {
        final List<String> intType = Arrays.asList("byte", "short", "int", "java.lang.Byte",
                "org.openl.meta.ByteValue", "org.openl.meta.ShortValue", "org.openl.meta.IntValue",
                "org.openl.meta.BigIntegerValue", "java.lang.Integer", "org.openl.meta.IntegerValue");
        final List<String> doubleType = Arrays.asList("long","float","double","java.lang.Long","java.lang.Float",
                "java.lang.Double", "org.openl.meta.LongValue","org.openl.meta.FloatValue","org.openl.meta.DoubleValue",
                "org.openl.meta.BigDecimalValue");
        ILogicalTable decisionValues;
        int width;

        if (isThatVCondition) {
            decisionValues = originalTable.getColumn(column);
            width = decisionValues.getHeight();
        } else {
            int numOfHRow = column - vColumnCounter;

            decisionValues = LogicalTableHelper.logicalTable(originalTable.getSource().getRow(numOfHRow));
            width = decisionValues.getWidth();
        }

        if (isThatVCondition || lastCondition) {
            int mergedColumnsCounts = isThatVCondition ? originalTable.getColumnWidth(column) : originalTable
                    .getSource().getCell(vColumnCounter, column - vColumnCounter).getWidth();
            boolean isMerged = mergedColumnsCounts > 1;

            //if the name row is merged then we have Array
            if (isMerged) {
                if (!type.isArray()){
                    return type.getName() + "[]";
                }else{
                    return type.getName();
                }
            }
        }

        for (int valueNum = 1; valueNum < width; valueNum++) {
            ILogicalTable cellValue;
            
            if (isThatVCondition) {
                cellValue = decisionValues.getRow(valueNum);
            } else {
                cellValue = decisionValues.getColumn(valueNum);
            }

            if (cellValue.getSource().getCell(0, 0).getStringValue() == null) {
                continue;
            }

           if (maybeIsRange(cellValue.getSource().getCell(0, 0).getStringValue())) {
                INumberRange range;

                /**try to create range by values**/
                if (intType.contains(type.getName())) {
                    try {
                        range = new IntRange(cellValue.getSource().getCell(0, 0).getStringValue());

                        /**Return name of a class without a package prefix**/
                        return range.getClass().getSimpleName();
                    } catch(Exception e) {
                       continue;
                    }
                } else if (doubleType.contains(type.getName())) {
                    try {
                        range = new DoubleRange(cellValue.getSource().getCell(0, 0).getStringValue());
                        
                        /**Return name of a class without a package prefix**/
                        return range.getClass().getSimpleName();
                    } catch(Exception e) {
                        continue;
                    }
                }
            }
        }
        if (!type.isArray()){
            return type.getName() + "[]";
        }else{
            return type.getName();
        }
    }

    private static boolean maybeIsRange(String cellValue) {
        Pattern p = Pattern.compile(".*(more|less|[;<>\\[\\(+\\.]).*|.*\\d+.*-.*");
        Matcher m = p.matcher(cellValue);
        
        return m.matches();
    }

    private static int getNumberOfConditions(DecisionTable decisionTable) {
        // number of conditions is counted by the number of income parameters
        //
        return decisionTable.getSignature().getNumberOfParameters();        
    }

    public static IWritableGrid createVirtualGrid(String poiSheetName, int numberOfColumns) {
        // Pre-2007 excel sheets had a limitation of 256 columns.
        Workbook workbook = (numberOfColumns > 256) ? new XSSFWorkbook() : new HSSFWorkbook();
        final Sheet sheet = workbook.createSheet(poiSheetName);
        return createVirtualGrid(sheet, "/VIRTUAL_EXCEL_FILE_FOR_DISPATCHER_TABLES.xls");
    }

    public static boolean isSimpleDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType);
    }

    public static boolean isSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_LOOKUP.equals(dtType);
    }

	public static int countHConditions(ILogicalTable table) {
        int width = table.getWidth();
        int cnt = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();

                if (isValidHConditionHeader(value)) {
                    ++cnt;
                }
            }
        }

        return cnt;
	}


	public static int countVConditions(ILogicalTable table) {
        int width = table.getWidth();
        int cnt = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();

                if (isValidConditionHeader(value)) {
                    ++cnt;
                }
            }
        }

        return cnt;
	}

    /**
     * Creates virtual {@link XlsSheetGridModel} with poi source sheet.
     */
    public static XlsSheetGridModel createVirtualGrid() {
        Sheet sheet = new HSSFWorkbook().createSheet();
        return createVirtualGrid(sheet, "/VIRTUAL_EXCEL_FILE.xls");
    }

    /**
     * Creates virtual {@link XlsSheetGridModel} from poi source sheet.
     *
     * @param sheet poi sheet source
     * @param virtualExcelFile file name, if null or blank will be used default name.
     * @return virtual grid that wraps sheet
     */
    private static XlsSheetGridModel createVirtualGrid(Sheet sheet, String virtualExcelFile) {
        final FileSourceCodeModule sourceCodeModule = new FileSourceCodeModule(virtualExcelFile, null);
        final SimpleWorkbookLoader workbookLoader = new SimpleWorkbookLoader(sheet.getWorkbook());
        XlsWorkbookSourceCodeModule mockWorkbookSource = new XlsWorkbookSourceCodeModule(sourceCodeModule, workbookLoader);
        XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet), mockWorkbookSource);

        return new XlsSheetGridModel(mockSheetSource);
    }

}
