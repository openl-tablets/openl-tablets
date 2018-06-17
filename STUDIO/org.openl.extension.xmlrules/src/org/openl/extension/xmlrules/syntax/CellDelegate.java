package org.openl.extension.xmlrules.syntax;

import java.util.Date;

import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

public class CellDelegate implements ICell {
    private final ICell delegate;

    public ICell getTopLeftCellFromRegion() {
        return delegate.getTopLeftCellFromRegion();
    }

    public CellDelegate(ICell delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getRow() {
        return delegate.getRow();
    }

    @Override
    public int getColumn() {
        return delegate.getColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return delegate.getAbsoluteRow();
    }

    @Override
    public int getAbsoluteColumn() {
        return delegate.getAbsoluteColumn();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        return delegate.getAbsoluteRegion();
    }

    @Override
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public ICellStyle getStyle() {
        return delegate.getStyle();
    }

    @Override
    public Object getObjectValue() {
        return delegate.getObjectValue();
    }

    @Override
    public String getStringValue() {
        return delegate.getStringValue();
    }

    @Override
    public ICellFont getFont() {
        return delegate.getFont();
    }

    @Override
    public IGridRegion getRegion() {
        return delegate.getRegion();
    }

    @Override
    public String getFormula() {
        return delegate.getFormula();
    }

    @Override
    public int getType() {
        return delegate.getType();
    }

    @Override
    public String getUri() {
        return delegate.getUri();
    }

    @Override
    public boolean hasNativeType() {
        return delegate.hasNativeType();
    }

    @Override
    public int getNativeType() {
        return delegate.getNativeType();
    }

    @Override
    public double getNativeNumber() {
        return delegate.getNativeNumber();
    }

    @Override
    public boolean getNativeBoolean() {
        return delegate.getNativeBoolean();
    }

    @Override
    public Date getNativeDate() {
        return delegate.getNativeDate();
    }

    @Override
    public ICellComment getComment() {
        return delegate.getComment();
    }

}
