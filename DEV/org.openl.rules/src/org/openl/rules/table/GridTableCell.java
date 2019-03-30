package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

class GridTableCell implements ICell {

    private int column;
    private int row;
    private IGridTable table;
    private ICell cell;

    @Override
    public ICell getTopLeftCellFromRegion() {
        return cell.getTopLeftCellFromRegion();
    }

    public GridTableCell(int column, int row, IGridTable table) {
        this.column = column;
        this.row = row;
        this.table = table;
        int gridColumn = table.getGridColumn(column, row);
        int gridRow = table.getGridRow(column, row);
        this.cell = table.getGrid().getCell(gridColumn, gridRow);
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getAbsoluteColumn() {
        return cell.getAbsoluteColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return cell.getAbsoluteRow();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        return cell.getAbsoluteRegion();
    }

    @Override
    public IGridRegion getRegion() {
        return cell.getRegion();
    }

    @Override
    public ICellStyle getStyle() {
        return cell.getStyle();
    }

    @Override
    public ICellFont getFont() {
        return cell.getFont();
    }

    @Override
    public int getHeight() {
        return table.isNormalOrientation() ? cell.getHeight() : cell.getWidth();
    }

    @Override
    public Object getObjectValue() {
        return cell.getObjectValue();
    }

    @Override
    public String getStringValue() {
        return cell.getStringValue();
    }

    @Override
    public int getWidth() {
        return table.isNormalOrientation() ? cell.getWidth() : cell.getHeight();
    }

    @Override
    public String getFormula() {
        return cell.getFormula();
    }

    @Override
    public int getType() {
        return cell.getType();
    }

    @Override
    public String getUri() {
        return cell.getUri();
    }

    @Override
    public boolean getNativeBoolean() {
        return cell.getNativeBoolean();
    }

    @Override
    public double getNativeNumber() {
        return cell.getNativeNumber();
    }

    @Override
    public int getNativeType() {
        return cell.getNativeType();
    }

    @Override
    public boolean hasNativeType() {
        return cell.hasNativeType();
    }

    @Override
    public Date getNativeDate() {
        return cell.getNativeDate();
    }

    @Override
    public ICellComment getComment() {
        return cell.getComment();
    }

}
