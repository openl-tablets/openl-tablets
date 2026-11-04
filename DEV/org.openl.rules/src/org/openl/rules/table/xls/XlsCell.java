package org.openl.rules.table.xls;

import java.util.Date;
import java.util.List;

import lombok.Getter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.load.CellLoader;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;
import org.openl.util.StringPool;

public class XlsCell implements ICell {

    @Getter
    private final int column;
    @Getter
    private final int row;
    @Getter
    private final IGridRegion region;
    private final CellLoader cellLoader;

    @Getter
    private int width = 1;
    @Getter
    private int height = 1;

    private final XlsSheetGridModel gridModel;

    /**
     * Usually there is a parameter duplication: the same column and row exist in cell object. But sometimes cell is
     * null, so we will have just the coordinates of the cell.
     */
    public XlsCell(int column, int row, XlsSheetGridModel gridModel) {
        this.column = column;
        this.row = row;
        this.region = gridModel.getRegionContaining(column, row);
        this.cellLoader = gridModel.getSheetSource().getSheetLoader().getCellLoader(column, row);

        if (region != null && region.getLeft() == column && region.getTop() == row) {
            this.width = region.getRight() - region.getLeft() + 1;
            this.height = region.getBottom() - region.getTop() + 1;
        }
        this.gridModel = gridModel;
    }

    @Override
    public @Nullable ICellStyle getStyle() {
        var cell = getCell();
        if (cell == null) {
            return null;
        }
        var style = cell.getCellStyle();
        if (style == null) {
            return null;
        }
        var workbook = gridModel.getSheetSource().getSheet().getWorkbook();
        return new XlsCellStyle(style, workbook);
    }

    @Override
    public int getAbsoluteColumn() {
        return getColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return getRow();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        var absoluteRegion = getRegion();
        if (absoluteRegion == null) {
            absoluteRegion = new GridRegion(row, column, row, column);
        }
        return absoluteRegion;
    }

    @Override
    public @Nullable ICellFont getFont() {
        var cell = getCell();
        if (cell == null) {
            return null;
        }
        var font = gridModel.getSheetSource()
                .getSheet()
                .getWorkbook()
                .getFontAt(cell.getCellStyle().getFontIndex());
        return new XlsCellFont(font, gridModel.getSheetSource().getSheet().getWorkbook());
    }

    @Override
    public List<TextRun> getTextRuns() {
        if (region != null && !isCurrentCellATopLeftCellInRegion()) {
            return getTopLeftCellFromRegion().getTextRuns();
        }
        var cell = getCell();
        if (cell == null || cell.getCellType() != CellType.STRING) {
            return List.of();
        }
        return PoiExcelHelper.getTextRuns(cell.getRichStringCellValue(),
                gridModel.getSheetSource().getSheet().getWorkbook(),
                null);
    }

    @Override
    public Object getObjectValue() {
        if (region == null || isCurrentCellATopLeftCellInRegion()) {
            // If cell belongs to some merged region, we try to get merged value from it.
            // If the top left cell is the current cell instance, we just extract it`s value.
            // In other case get string value of top left cell of the region.
            return extractCellValue();
        }
        var topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getObjectValue();
    }

    @Override
    public @Nullable String getStringValue() {
        var res = getObjectValue();
        return res == null ? null : String.valueOf(res);
    }

    @Override
    public ICell getTopLeftCellFromRegion() {
        // Gets the top left cell in this region
        var topRow = region.getTop();
        var col = region.getLeft();
        return gridModel.getCell(col, topRow);
    }

    private boolean isCurrentCellATopLeftCellInRegion() {
        var topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getColumn() == this.column && topLeftCell.getRow() == this.row;
    }

    private Object extractCellValue() {
        var value = PoiExcelHelper.getCellValue(getCell());
        // A text is interned: the cells of a workbook repeat the same texts many times
        return value instanceof String text ? StringPool.intern(text) : value;
    }

    @Override
    public @Nullable String getFormula() {
        if (region == null || isCurrentCellATopLeftCellInRegion()) {
            // A merge may start at a cell the sheet holds nothing for, as may any cell
            var cell = getCell();
            return cell != null && cell.getCellType() == CellType.FORMULA ? cell.getCellFormula() : null;
        }
        var topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getType() == IGrid.CELL_TYPE_FORMULA ? topLeftCell.getFormula() : null;
    }

    @Override
    public int getType() {
        if (region == null || isCurrentCellATopLeftCellInRegion()) {
            var cell = getCell();
            return cell == null ? IGrid.CELL_TYPE_BLANK : getIGridCellType(cell.getCellType());
        }
        var topLeftCell = getTopLeftCellFromRegion();
        return topLeftCell.getType();
    }

    @Override
    public String getUri() {
        return XlsUtil.xlsCellPresentation(column, row);
    }

    @Override
    public boolean getNativeBoolean() {
        return true;
    }

    @Override
    public double getNativeNumber() {
        var cell = getCell();
        if (cell == null) {
            return 0;
        }
        return cell.getNumericCellValue();
    }

    @Override
    public int getNativeType() {
        var cell = getCell();
        if (cell == null) {
            return IGrid.CELL_TYPE_BLANK;
        }

        var type = cell.getCellType();
        if (type == CellType.FORMULA) {
            return getIGridCellType(cell.getCachedFormulaResultType());
        }
        return getIGridCellType(type);
    }

    @Override
    public boolean hasNativeType() {
        return true;
    }

    /**
     * @return date value if cell is of type {@link IGrid#CELL_TYPE_NUMERIC} and is formatted in excel as date.<br>
     * null is cell is of type {@link IGrid#CELL_TYPE_NUMERIC} and is not formatted in excel as date.<br>
     */
    @Override
    public @Nullable Date getNativeDate() {
        var cell = getCell();
        if (cell == null) {
            return null;
        }
        if (CellType.STRING == cell.getCellType()) {
            double doubleValue;
            try {
                doubleValue = Double.parseDouble(cell.getStringCellValue());
            } catch (NumberFormatException npe) {
                throw new IllegalStateException("Cannot parse the value as a date : " + cell.getNumericCellValue());
            }
            var workbook = cell.getSheet().getWorkbook();
            var date1904 = false;
            if (workbook instanceof XSSFWorkbook fWorkbook1) {
                date1904 = fWorkbook1.isDate1904();
            } else if (workbook instanceof HSSFWorkbook fWorkbook) {
                date1904 = fWorkbook.getInternalWorkbook().isUsing1904DateWindowing();
            }
            return DateUtil.getJavaDate(doubleValue, date1904);
        }
        try {
            return cell.getDateCellValue();
        } catch (NullPointerException npe) {
            throw new IllegalStateException("Cannot parse the value as a date : " + cell.getNumericCellValue());
        }
    }

    @Override
    public @Nullable ICellComment getComment() {
        var cell = getCell();
        if (cell != null) {
            var comment = cell.getCellComment();
            if (comment != null) {
                return new XlsCellComment(comment);
            } else if (region != null && !isCurrentCellATopLeftCellInRegion()) {
                var topLeftCell = getTopLeftCellFromRegion();
                return topLeftCell.getComment();
            }
        }
        return null;
    }

    private Cell getCell() {
        return cellLoader.getCell();
    }

    private int getIGridCellType(CellType cellType) {
        switch (cellType) {
            case NUMERIC:
                return IGrid.CELL_TYPE_NUMERIC;
            case STRING:
                return IGrid.CELL_TYPE_STRING;
            case FORMULA:
                return IGrid.CELL_TYPE_FORMULA;
            case BLANK:
                return IGrid.CELL_TYPE_BLANK;
            case BOOLEAN:
                return IGrid.CELL_TYPE_BOOLEAN;
            case ERROR:
                return IGrid.CELL_TYPE_ERROR;
            default:
                throw new IllegalArgumentException("Unsupported cell type " + cellType);
        }
    }
}
