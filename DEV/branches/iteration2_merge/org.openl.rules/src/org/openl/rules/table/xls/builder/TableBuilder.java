package org.openl.rules.table.xls.builder;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.io.IOException;

import org.openl.rules.table.xls.XlsCellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Class that allows creating tables in specified excel sheet.
 * 
 * @author Aliaksandr Antonik
 * @author Andrei Astrouski
 */
public class TableBuilder {

    public static final String TABLE_PROPERTIES = "properties";
    public static final String TABLE_PROPERTIES_NAME = "name";

    /** The sheet to write tables to. */
    private final XlsSheetGridModel gridModel;
    /** Current table region in excel sheet. */
    private IGridRegion region;
    /** Table width. */
    private int width;
    /** Table height. */
    private int height;
    /** Current table row to write. */
    private int currentRow;
    /**Default cell style. */
    private CellStyle defaultCellStyle;

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets
     */
    public TableBuilder(XlsSheetGridModel gridModel) {
        if (gridModel == null)
            throw new IllegalArgumentException("gridModel is null");
        this.gridModel = gridModel;
    }

    protected XlsSheetGridModel getGridModel() {
        return gridModel;
    }

    protected IGridRegion getTableRegion() {
        return region;
    }

    protected int getWidth() {
        return width;
    }

    protected int getHeight() {
        return height;
    }

    protected int getCurrentRow() {
        return currentRow;
    }

    protected void incCurrentRow() {
        incCurrentRow(1);
    }

    protected void incCurrentRow(int increment) {
        currentRow += increment;
    }

    /**
     * Begins writing a table.
     * 
     * @param width table width in cells
     * @param height table height in cells
     * 
     * @throws CreateTableException if unable to create table
     * @throws IllegalStateException if <code>beginTable()</code> has already been called without subsequent
     * <code>endTable()</code>
     */
    public void beginTable(int width, int height) throws CreateTableException {
        if (region != null) {
            throw new IllegalStateException(
                    "beginTable() has already been called");
        }

        this.width = width;
        this.height = height;
        region = gridModel.findEmptyRect(width, height);
        if (region == null) {
            throw new CreateTableException(
                    "could not find appropriate region for writing");
        }

        currentRow = 0;
    }

    /**
     * Initializes default cell style.
     * 
     * @return cell style
     */
    protected CellStyle getDefaultCellStyle() {
        if (defaultCellStyle == null) {
            Workbook workbook = gridModel.getSheetSource()
                    .getWorkbookSource().getWorkbook();
            CellStyle cellStyle = workbook.createCellStyle();

            cellStyle.setBorderBottom(ICellStyle.BORDER_THIN);
            cellStyle.setBorderTop(ICellStyle.BORDER_THIN);
            cellStyle.setBorderLeft(ICellStyle.BORDER_THIN);
            cellStyle.setBorderRight(ICellStyle.BORDER_THIN);

            defaultCellStyle = cellStyle;
        }
        return defaultCellStyle;
    }

    /**
     * Writes table header.
     * 
     * @param header header text for the table
     * @param style header style
     * 
     * @throws IllegalStateException if method is called without
     * prior <code>beginTable()</code> call
     */
    public void writeHeader(String header, ICellStyle style) {
        if (region == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }
        writeCell(0, currentRow++, width, 1, header, style);
    }

    /**
     * Writes table properties.
     * 
     * @param properties table properties
     * @param style properties style
     * 
     * @throws IllegalArgumentException if properties is null
     * @throws IllegalStateException if method is called without
     * prior <code>beginTable()</code> call
     */
    public void writeProperties(Map<String, String> properties,
            ICellStyle style) {
        if (properties == null) {
            throw new IllegalArgumentException ("properties must be not null");
        }
        if (region == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }
        writeCell(0, currentRow, 1, properties.size(), TABLE_PROPERTIES, style);
        Set<String> keys = properties.keySet();
        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
            String key = iterator.next();
            writeCell(1, currentRow, 1, 1, key, style);
            String value = properties.get(key);
            writeCell(2, currentRow, 1, 1, value, style);
            currentRow++;
        }
    }

    /**
     * Writes table grid.
     * 
     * @param table table grid
     * 
     * @throws IllegalArgumentException if table is null
     * @throws IllegalStateException if method is called without
     * prior <code>beginTable()</code> call
     */
    public void writeGridTable(IGridTable table) {
        if (table == null) {
            throw new IllegalArgumentException ("table must be not null");
        }
        if (region == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }
        for (int i = 0; i < table.getGridWidth(); i++) {
            for (int j = 0; j < table.getGridHeight(); j++) {
                int cellWidth = table.getCellWidth(i, j);
                int cellHeight = table.getCellHeight(i, j);
                Object cellValue = table.getObjectValue(i, j);
                ICellStyle style = table.getCellStyle(i, j);
                writeCell(i, currentRow + j, cellWidth, cellHeight,
                        cellValue, style);
            }
        }
        currentRow += table.getGridHeight();
    }

    /**
     * Writes cell.
     * 
     * @param x cell x coordinate
     * @param y cell y coordinate
     * @param value cell value
     */
    protected void writeCell(int x, int y, Object value) {
        writeCell(x, y, 1, 1, value, null);
    }

    /**
     * Writes cell.
     * 
     * @param x cell x coordinate
     * @param y cell y coordinate
     * @param width cell width
     * @param height cell height
     * @param value cell value
     */
    protected void writeCell(int x, int y, int width, int height, Object value) {
        writeCell(x, y, width, height, value, null);
    }

    /**
     * Writes cell.
     * 
     * @param x cell x coordinate
     * @param y cell y coordinate
     * @param width cell width
     * @param height cell height
     * @param value cell value
     * @param style cell style
     */
    protected void writeCell(int x, int y, int width, int height, Object value,
            ICellStyle style) {
        CellStyle cellStyle = null;
        if (style != null) {
            cellStyle = ((XlsCellStyle) style).getXlsStyle();
        } else {
            cellStyle = getDefaultCellStyle();
        }
        x += region.getLeft();
        y += region.getTop();
        if (width == 1 && height == 1) {
            Cell cell = gridModel.createNewCell(x, y);
            gridModel.setCellValue(x, y, value);
            cell.setCellStyle(cellStyle);
        } else {
            int x2 = x + width - 1;
            int y2 = y + height - 1;
            gridModel.addMergedRegion(new GridRegion(y, x, y2, x2));
            gridModel.setCellValue(x, y, value);
            for (int col = x; col <= x2; col++) {
                for (int row = y; row <= y2; row++) {
                    gridModel.createNewCell(col, row).setCellStyle(cellStyle);
                }
            }
        }
    }

    /**
     * Finishes writing a table. Saves the changes to excel sheet.
     *
     * @throws IllegalStateException if method is called without
     * prior <code>beginTable()</code> call
     * @throws CreateTableException if an exception occurred when saving
     */
    public void endTable() throws CreateTableException {
        if (region == null) {
            throw new IllegalStateException(
                    "endTable() call without prior beginTable() call");
        }
        for (int y = currentRow; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                writeCell(x, y, 1, 1, "");
            }
        }
        try {
            gridModel.getSheetSource().getWorkbookSource().save();
        } catch (IOException e) {
            throw new CreateTableException ("could not save table");
        }
        region = null;
    }

}
