package org.openl.rules.table.xls.builder;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMember;

/**
 * The class is responsible for creating test method tables in excel sheets.
 * Given all necessary data (parameter names and titles, result column description, table being tested and testmethod
 * name) it just creates a new table in the given sheet.
 *
 * @author Aliaksandr Antonik.
 */
public class TestTableBuilder {
    private final XlsSheetGridModel gridModel;
    private HSSFCellStyle style, headerStyle;
    private String tableName;
    private String testMethodName;
    private List<String> paramName = new ArrayList<String>();
    private List<String> paramTitle = new ArrayList<String>();
    private String resultParamTitle = "Result";
    private int emptyRows = 1;

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets.
     */
    public TestTableBuilder(XlsSheetGridModel gridModel) {
        this.gridModel = gridModel;
    }

    /**
     * Returns name of the table the test method is created for.
     *
     * @return table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets name of the table the test method is created for.
     *
     * @param tableName table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns name of the test method table.
     *
     * @return test method table name
     */
    public String getTestMethodName() {
        return testMethodName;
    }

    /**
     * Sets name of the test method table.
     *
     * @param testMethodName test method table name
     */
    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }

    /**
     * Adds parameter to the test method table.
     *
     * @param name  parameter name
     * @param title parameter title
     */
    public void addParameter(String name, String title) {
        paramName.add(name);
        paramTitle.add(title);
    }

    /**
     * Sets title for result parameter of the test method table.
     *
     * @param title result title
     */
    public void setResultTitle(String title) {
        resultParamTitle = title;
    }

    /**
     * Returns number of empty rows in newly created test method table.
     *
     * @return number of rows
     */
    public int getEmptyRows() {
        return emptyRows;
    }

    /**
     * Sets number of empty rows in newly created test method table.
     *
     * @param emptyRows number of rows
     */
    public void setEmptyRows(int emptyRows) {
        this.emptyRows = emptyRows;
    }

    /**
     * Creates test method table in a excel sheet.
     *
     * @throws CreateTableException if unable to create table
     */
    public void create() throws CreateTableException {
        int width = paramName.size() + 1;
        int height = 3 + emptyRows;

        IGridRegion region = gridModel.findEmptyRect(width, height);
        if (region == null) {
            throw new CreateTableException("could not find appropriate region for writing");
        }

        gridModel
                .addMergedRegion(new GridRegion(region.getTop(), region.getLeft(), region.getTop(), region.getRight()));
        writeHeader(region, IXlsTableNames.TEST_METHOD_TABLE + " " + tableName + " " + testMethodName);

        int row = region.getTop() + 1;
        for (int col = region.getLeft(), index = 0; col < region.getRight(); ++col, ++index) {
            writeCell(col, row, paramName.get(index));
            writeCell(col, row + 1, paramTitle.get(index));
        }
        writeCell(region.getRight(), row, TestMethodHelper.EXPECTED_RESULT_NAME);
        writeCell(region.getRight(), row + 1, resultParamTitle);

        for (row += 2; row <= region.getBottom(); ++row) {
            for (int col = region.getLeft(); col <= region.getRight(); ++col) {
                writeCell(col, row, "");
            }
        }
    }

    private void writeHeader(IGridRegion region, String text) {
        gridModel.setCellValue(region.getLeft(), region.getTop(), text);
        HSSFCellStyle hstyle = getHeaderStyle();

        for (int col = region.getLeft(); col <= region.getRight(); ++col) {
            gridModel.createNewCell(col, region.getTop()).setCellStyle(hstyle);
        }
    }

    private void writeCell(int col, int row, String value) {
        HSSFCell cell = gridModel.createNewCell(col, row);
        cell.setCellValue(value);
        cell.setCellStyle(getStyle());
    }

    private HSSFCellStyle getHeaderStyle() {
        if (headerStyle == null) {
            HSSFWorkbook workbook = gridModel.getSheetSource().getWorkbookSource().getWorkbook();
            HSSFCellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(ICellStyle.BORDER_THIN);
            cellStyle.setBorderTop(ICellStyle.BORDER_THIN);
            cellStyle.setBorderLeft(ICellStyle.BORDER_THIN);
            cellStyle.setBorderRight(ICellStyle.BORDER_THIN);

            cellStyle.setFillForegroundColor(HSSFColor.BLACK.index);
            cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

            HSSFFont font = workbook.createFont();
            font.setColor(HSSFColor.WHITE.index);
            font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            cellStyle.setFont(font);

            headerStyle = cellStyle;
        }

        return headerStyle;
    }

    private HSSFCellStyle getStyle() {
        if (style == null) {
            HSSFWorkbook workbook = gridModel.getSheetSource().getWorkbookSource().getWorkbook();
            HSSFCellStyle cellStyle = workbook.createCellStyle();

            cellStyle.setBorderBottom(ICellStyle.BORDER_THIN);
            cellStyle.setBorderTop(ICellStyle.BORDER_THIN);
            cellStyle.setBorderLeft(ICellStyle.BORDER_THIN);
            cellStyle.setBorderRight(ICellStyle.BORDER_THIN);

            style = cellStyle;
        }

        return style;
    }

    /**
     * Creates instance of <code>TestTableBuilder</code> test method based on a decision table.
     *
     * @param gridModel represents interface for operations with excel sheets
     * @param node      decision table node
     * @return <code>TestTableBuilder</code> with attributes populated from decision table.
     */
    public static TestTableBuilder fromDecisionTableNode(XlsSheetGridModel gridModel, TableSyntaxNode node) {
        IOpenMember member = node.getMember();
        if (!(member instanceof DecisionTable)) {
            throw new IllegalArgumentException("syntax node is not a decision table node");
        }

        DecisionTable decisionTable = (DecisionTable) member;
        TestTableBuilder result = new TestTableBuilder(gridModel);

        result.setTableName(decisionTable.getName());
        result.setTestMethodName(decisionTable.getName() + "Test");
        result.setEmptyRows(3);

        IMethodSignature tableHeaderSignature = decisionTable.getHeader().getSignature();
        for (int i = 0; i < tableHeaderSignature.getNumberOfArguments(); ++i) {
            String paramName = tableHeaderSignature.getParameterName(i);
            result.addParameter(paramName, id2title(paramName));
        }

        return result;
    }

    private static String id2title(String id) {
        StringBuilder sb = new StringBuilder();
        boolean space = true;
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (Character.isWhitespace(c) || c == '_') {
                if (!space) {
                    space = true;
                    sb.append(" ");
                }
            } else if (space || Character.isUpperCase(c)) {
                space = false;
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        } else {
            return id;
        }
        return sb.toString().trim();
    }
}
