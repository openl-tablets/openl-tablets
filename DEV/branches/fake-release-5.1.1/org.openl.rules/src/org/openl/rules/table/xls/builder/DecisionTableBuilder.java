package org.openl.rules.table.xls.builder;

import java.io.IOException;

import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.util.HSSFColor;

/**
 * Helper class that allows creating new decision tables in specified excel sheet.
 *
 * @author Aliaksandr Antonik.
 */
public class DecisionTableBuilder {
    /**
     * Number of rows that usually are used for table header. That is the header cell identifying type of a table,
     * actions & conditions title, logic, parameter declarations, paramater business names.   
     */
    public static final int HEADER_HEIGHT = 5;
    /**
     * The sheet to write tables to.
     */
    private final XlsSheetGridModel gridModel;
    /**
     * Current table region in excel sheet.
     */
    private IGridRegion region;
    private HSSFCellStyle style;
    private int elementColumn;
    private int width, height;

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets.
     */
    public DecisionTableBuilder(XlsSheetGridModel gridModel) {
        if (gridModel == null)
            throw new IllegalArgumentException("gridModel is null");
        this.gridModel = gridModel;
    }

    /**
     * Begins writing a table.
     * 
     * @param width table width in cells
     * @param height tablel height in cells
     * 
     * @throws CreateTableException if unable to create table
     * @throws IllegalStateException if <code>beginTable()</code> has already been called without subsequent
     * <code>endTable()</code>.
     */
    public void beginTable(int width, int height) throws CreateTableException {
        if (region != null) {
            throw new IllegalStateException("beginTable has already been called");
        }

        this.width = width; 
        this.height = height; 
        region = gridModel.findEmptyRect(width, height);
        if (region == null) {
            throw new CreateTableException("could not find appropriate region for writing");
        }

        elementColumn = 0;
        initStyle();
    }

    /**
     * Finishes writing a table. Saves the changes to excel sheet.
     *
     * @throws IOException if an exception occurred when saving.
     */
    public void endTable() throws IOException {
        if (region == null) {
            throw new IllegalStateException("endTable() call without prior beginTable() call");
        }

        for (int y = HEADER_HEIGHT; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                writeCell(x, y, 1, "");
            }
        }

        region = null;
        gridModel.getSheetSource().getWorkbookSource().save();
    }

    /**
     * Writes decision table header. Requires the header signature, e.g. <br/> <code><i>void hello1(int hour)</i></code>.
     * 
     * @param signature method signature for the table.
     */
    public void writeHeader(String signature) {
        if (region == null) {
            throw new IllegalStateException("beginTable has to be called");
        }

        String headerText = IXlsTableNames.DECISION_TABLE2 + " " + signature;
        writeCell(0, 0, width, headerText);
    }

    private void writeCell(int x, int y, int width, String value) {
        x += region.getLeft();
        y += region.getTop();
        if (width == 1) {
            HSSFCell cell = gridModel.createNewCell(x, y);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        } else {
            int x2 = x + width - 1; 
            gridModel.addMergedRegion(new GridRegion(y, x, y, x2));
            gridModel.setCellValue(x, y, value);
            for (int col = x; col <= x2; ++col) {
                gridModel.createNewCell(col, region.getTop()).setCellStyle(style);
            }
        }
    }

    /**
     * Writes an element, which is an action, a condition or return block.
     * As an example look at a part of <i>driverPremium</i> table in OpenL Tutorial 4:
     * <table cellspacing="2">
     * <tr bgcolor="#ccffff"><td align="center" colspan="2"><b>C2</b></td></tr>
     * <tr bgcolor="#ccffff"><td colspan="2">located == "in" &amp;&amp; statelist.indexOf( di.driver.state) &gt;= 0</td></tr>
     * <tr bgcolor="#ccffff"><td align="center">String located</td><td align="center">String statelist</td></tr>
     * <tr bgcolor="#ffff99"><td align="center"><b>Located</b></td><td align="center"><b>State</b></td></tr>
     * </table>
     *
     * Here element's <code>title</code> is <i>C2</i>, <code>logic</code> is
     * <i>located == "in" &amp;&amp; statelist.indexOf( di.driver.state) &gt;= 0</i>,
     * <code>parameterNames</code> are <i>Located</i> and <i>State</i> and finally <code>parameterSignatures</code> are
     * <i>String located</i> and <i>String statelist</i>.
     *
     * <br/> <br/>
     * The lengths of <code>parameterNames</code> and <code>parameterSignatures</code> must be equal and positive.
     *
     * @param title element title
     * @param logic element logic
     * @param parameterNames array of element parameter names
     * @param parameterSignatures array of element parameter signatures
     */
    public void writeElement(String title, String logic, String[] parameterNames, String[] parameterSignatures) {
        if (parameterNames == null || parameterNames.length == 0)
            throw new IllegalArgumentException("parameterNames must be not null array of positive length");
        if (parameterSignatures == null || parameterSignatures.length == 0)
            throw new IllegalArgumentException("parameterSignatures must be not null array of positive length");
        if (parameterSignatures.length != parameterNames.length)
            throw new IllegalArgumentException("numbers of parameter names and parameter signatures must be equal");

        int elementWidth = parameterNames.length;
        if (elementColumn + elementWidth > height) {
            throw new IllegalStateException("total elements width is too big, expected height = " + height);
        }

        writeCell(elementColumn, 1, elementWidth, title);
        writeCell(elementColumn, 2, elementWidth, logic);
        for (int i = 0; i < elementWidth; ++i) {
            writeCell(elementColumn + i, 3, 1, parameterSignatures[i]);
            writeCell(elementColumn + i, 4, 1, parameterNames[i]);
        }

        elementColumn += elementWidth;
    }

    private HSSFCellStyle initStyle() {
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
    
}
