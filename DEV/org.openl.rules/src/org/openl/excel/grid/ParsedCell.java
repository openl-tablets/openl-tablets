package org.openl.excel.grid;

import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.openl.excel.parser.TableStyles;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsUtil;

public class ParsedCell implements ICell {
    private final static Object NOT_DEFINED = new Object();
    private final int row;
    private final int column;
    private final ParsedGrid grid;
    private Object value = NOT_DEFINED;
    private IGridRegion region;

    private transient TableStyles tableStyles;

    ParsedCell(int row, int column, ParsedGrid grid) {
        this.row = row;
        this.column = column;
        this.grid = grid;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public int getAbsoluteRow() {
        return getRow();
    }

    @Override
    public int getAbsoluteColumn() {
        return getColumn();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        IGridRegion absoluteRegion = getRegion();
        if (absoluteRegion == null) {
            absoluteRegion = new GridRegion(row, column, row, column);
        }
        return absoluteRegion;
    }

    @Override
    public int getWidth() {
        IGridRegion region = getRegion();
        return region == null ? 1 : region.getRight() - region.getLeft() + 1;
    }

    @Override
    public int getHeight() {
        IGridRegion region = getRegion();
        return region == null ? 1 : region.getBottom() - region.getTop() + 1;
    }

    @Override
    public ICellStyle getStyle() {
        return grid.getCellStyle(row, column);
    }

    @Override
    public Object getObjectValue() {
        if (value == NOT_DEFINED) {
            value = grid.getCellValue(row, column);
        }
        return value;
    }

    @Override
    public String getStringValue() {
        Object value = getObjectValue();
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public ICellFont getFont() {
        initializeStyles();
        return tableStyles == null ? null : tableStyles.getFont(row, column);
    }

    @Override
    public IGridRegion getRegion() {
        if (region == null) {
            region = grid.getRegion(row, column);
        }
        return region;
    }

    @Override
    public String getFormula() {
        initializeStyles();
        return tableStyles == null ? null : tableStyles.getFormula(row, column);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getType() {
        Object value = getObjectValue();
        if (value == null) {
            return Cell.CELL_TYPE_BLANK;
        } else if (value instanceof Boolean) {
            return Cell.CELL_TYPE_BOOLEAN;
        } else if (value instanceof Number || value instanceof Date) {
            return Cell.CELL_TYPE_NUMERIC;
        } else if (value instanceof String) {
            return Cell.CELL_TYPE_STRING;
        }
        return Cell.CELL_TYPE_ERROR;
    }

    @Override
    public String getUri() {
        return XlsUtil.xlsCellPresentation(column, row);
    }

    @Override
    public boolean hasNativeType() {
        return true;
    }

    @Override
    public int getNativeType() {
        return getType();
    }

    @Override
    public double getNativeNumber() {
        Object value = getObjectValue();

        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Date) {
            return DateUtil.getExcelDate((Date) value, grid.isUse1904Windowing());
        }

        return Double.NaN;
    }

    @Override
    public boolean getNativeBoolean() {
        Object value = getObjectValue();
        return value == null ? false : (Boolean) value;
    }

    @Override
    public Date getNativeDate() {
        Object value = getObjectValue();

        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return DateUtil.getJavaDate(((Number) value).doubleValue(), grid.isUse1904Windowing());
        }

        return null;
    }

    @Override
    public ICellComment getComment() {
        initializeStyles();
        return tableStyles == null ? null : tableStyles.getComment(row, column);
    }

    @Override
    public ICell getTopLeftCellFromRegion() {
        IGridRegion region = getRegion();
        return region == null ? this : grid.getCell(region.getLeft(), region.getTop());
    }

    private void initializeStyles() {
        if (tableStyles == null) {
            tableStyles = grid.getTableStyles(row, column);
        }
    }
}
