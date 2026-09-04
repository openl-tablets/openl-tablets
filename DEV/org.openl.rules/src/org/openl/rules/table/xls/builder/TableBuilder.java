package org.openl.rules.table.xls.builder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsCellComment;
import org.openl.rules.table.xls.XlsCellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.FormatConstants;

/**
 * Class that allows creating tables in specified excel sheet.
 *
 * @author Aliaksandr Antonik
 * @author Andrei Astrouski
 */
public class TableBuilder {

    private static final String TABLE_PROPERTIES = "properties";

    public static final int HEADER_HEIGHT = 1;
    public static final int PROPERTIES_MIN_WIDTH = 3;

    /**
     * The sheet to write tables to.
     */
    private final XlsSheetGridModel gridModel;
    /**
     * Current table region in excel sheet.
     */
    private IGridRegion region;
    /**
     * Table width.
     */
    private int width;
    /**
     * Table height.
     */
    private int height;
    /**
     * Current table row to write.
     */
    private int currentRow;
    /**
     * Default cell style.
     */
    private CellStyle defaultCellStyle;
    /**
     * Default data cell style
     */
    private CellStyle defaultDateCellStyle;

    /**
     * Mapping for style to style transformation.
     */
    private final Map<CellStyle, CellStyle> style2style;

    private final MetaInfoWriter metaInfoWriter;

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets
     */
    public TableBuilder(XlsSheetGridModel gridModel) {
        this(gridModel, null);
    }

    public TableBuilder(XlsSheetGridModel gridModel, MetaInfoWriter metaInfoWriter) {
        this.gridModel = Objects.requireNonNull(gridModel, "gridModel cannot be null");
        style2style = new HashMap<>();
        this.metaInfoWriter = metaInfoWriter;
    }

    /**
     * Begins writing a table.
     *
     * @param width  table width in cells
     * @param height table height in cells
     * @throws CreateTableException  if unable to create table
     * @throws IllegalStateException if <code>beginTable()</code> has already been called without subsequent
     *                               <code>endTable()</code>
     */
    public void beginTable(int width, int height) throws CreateTableException {
        if (region != null) {
            throw new IllegalStateException("beginTable() has already been called");
        }

        this.width = width;
        this.height = height;
        region = gridModel.findEmptyRect(width, height);
        if (region == null) {
            throw new CreateTableException("Could not find appropriate region for writing");
        }

        currentRow = 0;
        style2style.clear();
    }

    /**
     * Begins writing a table within the specified region.
     *
     * @param regionToWrite region to write table.
     * @throws CreateTableException  if unable to create table
     * @throws IllegalStateException if <code>beginTable()</code> has already been called without subsequent
     *                               <code>endTable()</code>
     */
    public void beginTable(IGridRegion regionToWrite) throws CreateTableException {
        if (region != null) {
            throw new IllegalStateException("beginTable() has already been called");
        }
        region = regionToWrite;
        if (region == null || !IGridRegion.Tool.isValidRegion(region, gridModel.getSpreadsheetConstants())) {
            throw new CreateTableException("Could not find appropriate region for writing");
        }
        currentRow = 0;
        style2style.clear();
    }

    /**
     * Finishes writing a table. Saves the changes to excel sheet.
     *
     * @throws IllegalStateException if method is called without prior <code>beginTable()</code> call
     * @throws CreateTableException  if an exception occurred when saving
     */
    public void endTable() throws CreateTableException {
        if (region == null) {
            throw new IllegalStateException("endTable() call without prior beginTable() call");
        }
        for (var y = currentRow; y < height; ++y) {
            for (var x = 0; x < width; ++x) {
                writeCell(x, y, 1, 1, "");
            }
        }
        region = null;
        style2style.clear();
    }

    /**
     * Initializes default cell style.
     *
     * @return cell style
     */
    private CellStyle getDefaultCellStyle() {
        if (defaultCellStyle == null) {
            var workbook = gridModel.getSheetSource().getWorkbookSource().getWorkbook();
            CellStyle cellStyle = PoiExcelHelper.createCellStyle(workbook);

            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);

            defaultCellStyle = cellStyle;
        }
        return defaultCellStyle;
    }

    private CellStyle getDefaultDateCellStyle() {
        if (defaultDateCellStyle == null) {
            var workbook = gridModel.getSheetSource().getWorkbookSource().getWorkbook();
            CellStyle cellStyle = PoiExcelHelper.createCellStyle(workbook);

            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);

            cellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));

            defaultDateCellStyle = cellStyle;
        }
        return defaultDateCellStyle;
    }

    public IGridRegion getTableRegion() {
        return region;
    }

    /**
     * Writes cell.
     *
     * @param x      cell x coordinate
     * @param y      cell y coordinate
     * @param width  cell width
     * @param height cell height
     * @param value  cell value
     */
    private void writeCell(int x, int y, int width, int height, Object value) {
        writeCell(x, y, width, height, value, null);
    }

    /**
     * Writes cell.
     *
     * @param x      cell x coordinate
     * @param y      cell y coordinate
     * @param width  cell width
     * @param height cell height
     * @param value  cell value
     * @param style  cell style
     */
    private void writeCell(int x, int y, int width, int height, Object value, ICellStyle style) {
        var cellStyle = analyseCellStyle(style);
        x += region.getLeft();
        y += region.getTop();
        var x2 = x + width - 1;
        var y2 = y + height - 1;
        var sheet = gridModel.getSheetSource().getSheet();
        if (width > 1 || height > 1) {
            gridModel.addMergedRegion(new GridRegion(y, x, y2, x2));
        }
        gridModel.setCellValue(x, y, value);
        for (var col = x; col <= x2; col++) {
            for (var row = y; row <= y2; row++) {
                setCellStyle(PoiExcelHelper.getOrCreateCell(col, row, sheet), cellStyle);
            }
        }
        if (value instanceof Date) {
            // Excel stores a date as a number, and it is read back as a date only while the cell holding it carries
            // a date format. A merged cell holds its value in the one it opens with.
            var cell = PoiExcelHelper.getOrCreateCell(x, y, sheet);
            setCellStyle(cell, cellStyle == getDefaultCellStyle() ? getDefaultDateCellStyle() : getDateCellStyle(cell));
        }
    }

    /**
     * Analyse the type of cell style.
     *
     * @param style Incoming cell style.
     * @return CellStyle according to its type. If income value was <code>NULL</code> returns {@link #defaultCellStyle}.
     * @author DLiauchuk
     */
    private CellStyle analyseCellStyle(ICellStyle style) {
        CellStyle returnStyle;
        if (style instanceof XlsCellStyle cellStyle) {
            returnStyle = cellStyle.getXlsStyle();
        } else {
            returnStyle = getDefaultCellStyle();
        }
        return returnStyle;
    }

    /**
     * If the value was set to the cell of type date, we need to create new style for this cell with data format for
     * dates.
     *
     * @param cell Cell with value in it.
     */
    private CellStyle getDateCellStyle(Cell cell) {
        var previousStyle = cell.getCellStyle();
        cell.setCellStyle(PoiExcelHelper.createCellStyle(cell.getSheet().getWorkbook()));
        cell.getCellStyle().cloneStyleFrom(previousStyle);
        cell.getCellStyle()
                .setDataFormat((short) BuiltinFormats.getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));
        return cell.getCellStyle();
    }

    private void setCellStyle(Cell cell, CellStyle cellStyle) {
        var newStyle = style2style.get(cellStyle);
        if (newStyle != null) {
            cellStyle = newStyle;
        }
        try {
            cell.setCellStyle(cellStyle);
        } catch (Exception e) {
            var style = findWorkbookCellStyle(cellStyle);
            if (style != null) {
                style2style.put(cellStyle, style);
            } else {
                var workbook = gridModel.getSheetSource().getWorkbookSource().getWorkbook();
                style = PoiExcelHelper.createCellStyle(workbook);
                try {
                    style.cloneStyleFrom(cellStyle);
                } catch (IllegalArgumentException ex) {
                    // FIXME: remove try.. catch
                }
                style2style.put(cellStyle, style);
            }
            cell.setCellStyle(style);
        }
    }

    private CellStyle findWorkbookCellStyle(CellStyle cellStyle) {
        var workbook = gridModel.getSheetSource().getWorkbookSource().getWorkbook();
        var numCellStyles = workbook.getNumCellStyles();
        for (var i = 0; i < numCellStyles; i++) {
            var cellStyleAt = workbook.getCellStyleAt((short) i);
            if (equalsStyle(cellStyleAt, cellStyle)) {
                return cellStyleAt;
            }
        }
        return null;
    }

    private boolean equalsStyle(CellStyle cs1, CellStyle cs2) {
        return cs1.getAlignment() == cs2.getAlignment() && cs1.getHidden() == cs2.getHidden() && cs1.getLocked() == cs2
                .getLocked() && cs1.getWrapText() == cs2.getWrapText() && cs1
                .getBorderBottom() == cs2.getBorderBottom() && cs1.getBorderLeft() == cs2.getBorderLeft() && cs1
                .getBorderRight() == cs2.getBorderRight() && cs1.getBorderTop() == cs2.getBorderTop() && cs1
                .getBottomBorderColor() == cs2.getBottomBorderColor() && cs1
                .getFillBackgroundColor() == cs2.getFillBackgroundColor() && cs1
                .getFillForegroundColor() == cs2.getFillForegroundColor() && cs1
                .getFillPattern() == cs2.getFillPattern() && cs1
                .getIndention() == cs2.getIndention() && cs1
                .getLeftBorderColor() == cs2.getLeftBorderColor() && cs1
                .getRightBorderColor() == cs2.getRightBorderColor() && cs1
                .getRotation() == cs2.getRotation() && cs1
                .getTopBorderColor() == cs2.getTopBorderColor() && cs1
                .getVerticalAlignment() == cs2.getVerticalAlignment() && cs1
                .getDataFormat() == cs2.getDataFormat();
    }

    /**
     * Writes table grid.
     *
     * @param table table grid
     * @throws IllegalArgumentException if table is null
     * @throws IllegalStateException    if method is called without prior <code>beginTable()</code> call
     */
    public void writeGridTable(IGridTable table) {
        Objects.requireNonNull(table, "table cannot be null");
        if (region == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }
        for (var i = 0; i < table.getWidth(); i++) {
            for (var j = 0; j < table.getHeight(); j++) {
                var cell = table.getCell(i, j);
                var cellWidth = cell.getWidth();
                var cellHeight = cell.getHeight();
                Object cellValue;
                if (cell.getFormula() != null) {
                    cellValue = "=" + cell.getFormula();
                } else {
                    cellValue = cell.getObjectValue();
                }
                var style = cell.getStyle();
                writeCell(i, currentRow + j, cellWidth, cellHeight, cellValue, style);
                Cell newCell = PoiExcelHelper.getCell(i + region.getLeft(),
                        currentRow + j + region.getTop(),
                        gridModel.getSheetSource().getSheet());
                if (cell.getType() != IGrid.CELL_TYPE_FORMULA && newCell.getCellType() == CellType.FORMULA) {
                    newCell.setCellValue(cellValue.toString());
                }
                var iCellComment = cell.getComment();
                if (iCellComment != null) {
                    var xlxComment = ((XlsCellComment) iCellComment).getXlxComment();
                    var sheet = newCell.getSheet();
                    var anchor = sheet.getWorkbook().getCreationHelper().createClientAnchor();
                    anchor.setCol1(newCell.getColumnIndex());
                    anchor.setCol2(newCell.getColumnIndex() + 1);
                    anchor.setRow1(newCell.getRow().getRowNum());
                    anchor.setRow2(newCell.getRow().getRowNum() + 3);
                    var comment = sheet.createDrawingPatriarch().createCellComment(anchor);
                    comment.setAuthor(xlxComment.getAuthor());
                    comment.setString(xlxComment.getString());
                    newCell.setCellComment(comment);
                }
                if (metaInfoWriter != null && newCell != null) {
                    metaInfoWriter.setMetaInfo(newCell.getRowIndex(),
                            newCell.getColumnIndex(),
                            metaInfoWriter.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn()));
                }
            }
        }
        currentRow += table.getHeight();
    }

    /**
     * Writes table header.
     *
     * @param header header text for the table
     * @param style  header style
     * @throws IllegalStateException if method is called without prior <code>beginTable()</code> call
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
     * @param style      properties style
     * @throws IllegalArgumentException if properties is null
     * @throws IllegalStateException    if method is called without prior <code>beginTable()</code> call
     */
    public void writeProperties(Map<String, Object> properties, ICellStyle style) {
        Objects.requireNonNull(properties, "properties cannot be null");
        if (region == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }

        if (!properties.isEmpty()) {
            writeCell(0, currentRow, 1, properties.size(), TABLE_PROPERTIES, style);
            for (var property : properties.entrySet()) {
                writeCell(1, currentRow, 1, 1, property.getKey(), style);
                // The value reaches the table's right edge, the way a properties section is written by hand: left in
                // the third column, the columns beside it would read as cells of the section's own.
                writeCell(2, currentRow, width - 2, 1, property.getValue(), style);
                currentRow++;
            }
        }
    }

}
