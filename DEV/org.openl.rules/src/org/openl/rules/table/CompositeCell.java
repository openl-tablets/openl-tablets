package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

class CompositeCell implements ICell {

    private int column;
    private int row;
    private IGridRegion region;
    private ICell delegate;
    private IGridTable gridTable;

    @Override
    public ICell getTopLeftCellFromRegion() {
        return delegate.getTopLeftCellFromRegion();
    }

    /**
     * parameters column and row are different from inner column and row in cell delegate.
     */
    public CompositeCell(int column, int row, IGridRegion region, ICell delegate, IGridTable gridTable) {
        this.column = column;
        this.row = row;
        this.region = region;
        this.delegate = delegate;
        this.gridTable = gridTable;
    }

    @Override
    public int getAbsoluteColumn() {
        return delegate.getAbsoluteColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return delegate.getAbsoluteRow();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        return new GridRegion(delegate.getRow(), delegate.getColumn(), delegate.getRow(), delegate.getColumn());
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
    public IGridRegion getRegion() {
        return region;
    }

    @Override
    public ICellStyle getStyle() {
        return delegate.getStyle();
    }

    @Override
    public ICellFont getFont() {
        return delegate.getFont();
    }

    @Override
    public int getHeight() {
        if (!gridTable.isNormalOrientation()) {
            return getCellWidth();
        }
        return getCellHeight();
    }

    private int getCellHeight() {
        if (region == null) {
            return delegate.getHeight();
        }
        return region.getBottom() - region.getTop() + 1;
    }


    @Override
    public int getWidth() {
        if (!gridTable.isNormalOrientation()) {
            return getCellHeight();
        }
        return getCellWidth();
    }

    private int getCellWidth() {
        if (region == null) {
            return delegate.getWidth();
        }
        return region.getRight() - region.getLeft() + 1;
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
    public boolean getNativeBoolean() {
        return delegate.getNativeBoolean();
    }

    @Override
    public double getNativeNumber() {
        return delegate.getNativeNumber();
    }

    @Override
    public int getNativeType() {
        return delegate.getNativeType();
    }

    @Override
    public boolean hasNativeType() {
        return delegate.hasNativeType();
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
