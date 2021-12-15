package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

public class EmptyCell implements ICell {

    private final ICell cell;

    public EmptyCell(ICell cell) {
        this.cell = cell;
    }

    @Override
    public int getRow() {
        return cell.getRow();
    }

    @Override
    public int getColumn() {
        return cell.getColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return cell.getAbsoluteRow();
    }

    @Override
    public int getAbsoluteColumn() {
        return cell.getAbsoluteColumn();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        return cell.getAbsoluteRegion();
    }

    @Override
    public int getWidth() {
        return cell.getWidth();
    }

    @Override
    public int getHeight() {
        return cell.getHeight();
    }

    @Override
    public ICellStyle getStyle() {
        return cell.getStyle();
    }

    @Override
    public Object getObjectValue() {
        return null;
    }

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public ICellFont getFont() {
        return cell.getFont();
    }

    @Override
    public IGridRegion getRegion() {
        return cell.getRegion();
    }

    @Override
    public String getFormula() {
        return null;
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
    public boolean hasNativeType() {
        return cell.hasNativeType();
    }

    @Override
    public int getNativeType() {
        return cell.getNativeType();
    }

    @Override
    public double getNativeNumber() {
        return cell.getNativeNumber();
    }

    @Override
    public boolean getNativeBoolean() {
        return false;
    }

    @Override
    public Date getNativeDate() {
        return null;
    }

    @Override
    public ICellComment getComment() {
        return null;
    }

    @Override
    public ICell getTopLeftCellFromRegion() {
        return cell.getTopLeftCellFromRegion();
    }
}
