package org.openl.rules.table.xls.builder;

import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.ICell;
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
     * The style carried into this workbook for each style of another one.
     *
     * <p>Styles are held by identity: two workbooks describe a style the same way while the numbers in that
     * description mean different things in each, so a style of this workbook answers equal to one of another.
     */
    private final Map<CellStyle, CellStyle> style2style;

    /**
     * Carries the styles of other workbooks into this one, built when the first such style is written.
     */
    @Nullable
    private CellStyleCarrier styleCarrier;

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
        style2style = new IdentityHashMap<>();
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
        if (region == null || !GridRegionUtils.isValidRegion(region, gridModel.getSpreadsheetConstants())) {
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
        for (var y = currentRow; y < height; y++) {
            for (var x = 0; x < width; x++) {
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
            var workbook = gridModel.getWorkbookToWrite();
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
            var workbook = gridModel.getWorkbookToWrite();
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
        var source = style instanceof XlsCellStyle xlsStyle ? xlsStyle : null;
        var cellStyle = source != null ? source.getXlsStyle() : getDefaultCellStyle();
        x += region.getLeft();
        y += region.getTop();
        var x2 = x + width - 1;
        var y2 = y + height - 1;
        var sheet = gridModel.getSheetToWrite();
        if (width > 1 || height > 1) {
            gridModel.addMergedRegion(new GridRegion(y, x, y2, x2));
        }
        gridModel.setCellValue(x, y, value);
        for (var col = x; col <= x2; col++) {
            for (var row = y; row <= y2; row++) {
                setCellStyle(PoiExcelHelper.getOrCreateCell(col, row, sheet), cellStyle, source);
            }
        }
        if (value instanceof Date) {
            // Excel stores a date as a number, and it is read back as a date only while the cell holding it carries
            // a date format. A merged cell holds its value in the one it opens with.
            var cell = PoiExcelHelper.getOrCreateCell(x, y, sheet);
            var dateStyle = cellStyle == getDefaultCellStyle() ? getDefaultDateCellStyle() : getDateCellStyle(cell);
            setCellStyle(cell, dateStyle, null);
        }
        if (value != null) {
            // The style written over the value decides its apostrophe again.
            gridModel.setApostrophe(x, y);
        }
    }

    /**
     * If the value was set to the cell of type date, we need to create new style for this cell with data format for
     * dates.
     *
     * @param cell Cell with value in it.
     */
    private CellStyle getDateCellStyle(Cell cell) {
        return PoiExcelHelper.setCellDataFormat(cell,
                BuiltinFormats.getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));
    }

    /**
     * Gives the cell the style asked for, carrying it into this workbook when it belongs to another one.
     *
     * <p>A style is written as it stands when this workbook already holds it. A style of another workbook is
     * carried over instead, which keeps the colours it shows: they are held as numbers a palette of its own
     * gives meaning to, and this workbook reads the same numbers as other colours.
     */
    private void setCellStyle(Cell cell, CellStyle cellStyle, @Nullable XlsCellStyle source) {
        var newStyle = style2style.get(cellStyle);
        if (newStyle != null) {
            cellStyle = newStyle;
        }
        try {
            cell.setCellStyle(cellStyle);
        } catch (RuntimeException e) {
            // A style of another workbook is refused, and by which exception depends on the two formats. One
            // that came from this workbook was refused for a reason of its own, which stands.
            if (source == null) {
                throw e;
            }
            var carried = carrier().carry(source);
            style2style.put(cellStyle, carried);
            cell.setCellStyle(carried);
        }
    }

    private CellStyleCarrier carrier() {
        if (styleCarrier == null) {
            styleCarrier = new CellStyleCarrier(gridModel.getWorkbookToWrite());
        }
        return styleCarrier;
    }

    /**
     * Writes table grid.
     *
     * <p>A table of the same workbook, such as one moved on its sheet, is written as it is stored: every cell keeps
     * the notation of its value, its text untrimmed and the fonts of its parts. A table of another workbook is written
     * by its values: an Excel formula stays an Excel formula, and a text starting with {@code =} stays a text, after
     * the apostrophe of Excel. A cell a merge covers below or right of its top-left cell stays empty.
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
        var source = table.getGrid() instanceof XlsSheetGridModel grid && isSameWorkbook(grid) ? grid : null;
        for (var i = 0; i < table.getWidth(); i++) {
            for (var j = 0; j < table.getHeight(); j++) {
                var cell = table.getCell(i, j);
                writeGridCell(source, table.getRegion(), cell, i, j);
            }
        }
        currentRow += table.getHeight();
    }

    private boolean isSameWorkbook(XlsSheetGridModel grid) {
        return grid.getSheetSource().getSheet().getWorkbook() == gridModel.getSheetSource().getSheet().getWorkbook();
    }

    /**
     * Writes a cell of a table grid with its value, style, comment and meta info.
     *
     * @param source the sheet of the cell when it is of the same workbook, so it is copied as it is stored
     * @param table  the region of the table written, in its sheet
     */
    private void writeGridCell(@Nullable XlsSheetGridModel source, IGridRegion table, ICell cell, int i, int j) {
        var cellWidth = cell.getWidth();
        var cellHeight = cell.getHeight();
        var style = cell.getStyle();
        var column = i + region.getLeft();
        var row = currentRow + j + region.getTop();
        var area = cell.getAbsoluteRegion();
        var covered = isCovered(cell, area, table);
        if (source != null) {
            writeCell(i, currentRow + j, cellWidth, cellHeight, null, style);
            if (!covered) {
                // A merge reaching in from outside the table shows the value of its top-left cell here.
                gridModel.copyCellValue(source, area.getLeft(), area.getTop(), column, row);
            }
        } else {
            var formula = covered ? null : cell.getFormula();
            writeCell(i, currentRow + j, cellWidth, cellHeight, covered || formula != null ? null : cell.getObjectValue(),
                    style);
            if (formula != null) {
                writeFormula(column, row, formula, cell);
            }
        }
        var newCell = PoiExcelHelper.getOrCreateCell(column, row, gridModel.getSheetToWrite());
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
        if (metaInfoWriter != null) {
            metaInfoWriter.setMetaInfo(newCell.getRowIndex(),
                    newCell.getColumnIndex(),
                    metaInfoWriter.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn()));
        }
    }

    /**
     * Writes the Excel formula of a cell of another workbook.
     *
     * <p>A formula the workbook written to cannot read or compute, such as one using a defined name it has not, keeps
     * the value it computed in the other workbook instead.
     */
    private void writeFormula(int column, int row, String formula, ICell cell) {
        try {
            gridModel.setCellFormula(column, row, formula);
        } catch (IllegalArgumentException e) {
            gridModel.setCellValue(column, row, cell.getObjectValue());
        }
    }

    /**
     * Whether a merge of the table covers the cell below or right of its top-left cell, where the merge holds no
     * value. A merge reaching into the table from outside it covers nothing written.
     *
     * @param area the region of the sheet the cell covers
     */
    private static boolean isCovered(ICell cell, IGridRegion area, IGridRegion table) {
        var topLeft = area.getLeft() == cell.getAbsoluteColumn() && area.getTop() == cell.getAbsoluteRow();
        return !topLeft && GridRegionUtils.contains(table, area.getLeft(), area.getTop());
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
