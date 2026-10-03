package org.openl.excel.grid;

import java.util.Date;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DateUtil;

import org.openl.excel.parser.TableStyles;
import org.openl.gen.writers.ISO8601DateFormater;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;
import org.openl.rules.table.xls.XlsUtil;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ParsedCell implements ICell {
    private static final Object NOT_DEFINED = new Object();
    @Getter
    private final int row;
    @Getter
    private final int column;
    private final ParsedGrid grid;
    private Object value = NOT_DEFINED;
    private IGridRegion region;

    private TableStyles tableStyles;

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
        var absoluteRegion = getRegion();
        if (absoluteRegion == null) {
            absoluteRegion = new GridRegion(row, column, row, column);
        }
        return absoluteRegion;
    }

    @Override
    public int getWidth() {
        var mergedRegion = getRegion();
        return mergedRegion == null ? 1 : mergedRegion.getRight() - mergedRegion.getLeft() + 1;
    }

    @Override
    public int getHeight() {
        var mergedRegion = getRegion();
        return mergedRegion == null ? 1 : mergedRegion.getBottom() - mergedRegion.getTop() + 1;
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
        var cellValue = getObjectValue();
        if (cellValue == null) {
            return null;
        } else  if (cellValue instanceof Date date) {
            return ISO8601DateFormater.format(date);
        } else {
            return cellValue.toString();
        }
    }

    @Override
    public ICellFont getFont() {
        initializeStyles();
        return tableStyles == null ? null : tableStyles.getFont(row, column);
    }

    @Override
    public List<TextRun> getTextRuns() {
        initializeStyles();
        return tableStyles == null ? List.of() : tableStyles.getTextRuns(row, column);
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

    @Override
    public int getType() {
        var cellValue = getObjectValue();
        if (cellValue == null) {
            return IGrid.CELL_TYPE_BLANK;
        } else if (cellValue instanceof Boolean) {
            return IGrid.CELL_TYPE_BOOLEAN;
        } else if (cellValue instanceof Number || cellValue instanceof Date) {
            return IGrid.CELL_TYPE_NUMERIC;
        } else if (cellValue instanceof String) {
            return IGrid.CELL_TYPE_STRING;
        }
        return IGrid.CELL_TYPE_ERROR;
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
        var cellValue = getObjectValue();

        if (cellValue == null) {
            return 0.0;
        }
        if (cellValue instanceof Number number) {
            return number.doubleValue();
        }

        return Double.NaN;
    }

    @Override
    public boolean getNativeBoolean() {
        var cellValue = getObjectValue();
        return cellValue != null && (Boolean) cellValue;
    }

    @Override
    public Date getNativeDate() {
        var cellValue = getObjectValue();

        if (cellValue == null) {
            return null;
        }
        if (cellValue instanceof Date date) {
            return date;
        }
        if (cellValue instanceof Number number) {
            return DateUtil.getJavaDate(number.doubleValue(), grid.isUse1904Windowing());
        }
        if (cellValue instanceof String string) {
            return DateUtil.getJavaDate(Double.parseDouble(string), grid.isUse1904Windowing());
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
        var mergedRegion = getRegion();
        return mergedRegion == null ? this : grid.getCell(mergedRegion.getLeft(), mergedRegion.getTop());
    }

    private void initializeStyles() {
        if (tableStyles == null) {
            tableStyles = grid.getTableStyles(row, column);
        }
    }
}
