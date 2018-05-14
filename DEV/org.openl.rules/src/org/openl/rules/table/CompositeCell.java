package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

class CompositeCell implements ICell {

    private int column;
    private int row;
    private IGridRegion region;
    private ICell delegate;
    
    public ICell getTopLeftCellFromRegion() {
		return delegate.getTopLeftCellFromRegion();
	}

	/**
     * parameters column and row are different from inner column and row in cell delegate. 
     */
    public CompositeCell(int column, int row, IGridRegion region, ICell delegate) {
        this.column = column;
        this.row = row;
        this.region = region;
        this.delegate = delegate;  
    }

    public int getAbsoluteColumn() {
        return delegate.getAbsoluteColumn();
    }

    public int getAbsoluteRow() {
        return delegate.getAbsoluteRow();
    }

    public IGridRegion getAbsoluteRegion() {
        //return delegate.getAbsoluteRegion();
        return new GridRegion(delegate.getRow(), delegate.getColumn(), delegate.getRow(), delegate.getColumn());
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public IGridRegion getRegion() {
        return region;
    }

    public ICellStyle getStyle() {
        return delegate.getStyle();
    }

    public ICellFont getFont() {
        return delegate.getFont();
    }

    public int getHeight() {
        if (region == null){
            return delegate.getHeight();
        }
        return region.getBottom() - region.getTop() + 1;
    }
    
    public Object getObjectValue() {
        return delegate.getObjectValue();
    }
    
    public String getStringValue() {
        return delegate.getStringValue();
    }

    public int getWidth() {
        if (region == null){
            return delegate.getWidth();
        }
        return region.getRight() - region.getLeft() + 1;
    }

    public String getFormula() {
        return delegate.getFormula();
    }

    public int getType() {
        return delegate.getType();
    }

    public String getUri() {
        return delegate.getUri();
    }

    public boolean getNativeBoolean() {
        return delegate.getNativeBoolean();
    }

    public double getNativeNumber() {
        return delegate.getNativeNumber();
    }

    public int getNativeType() {
        return delegate.getNativeType();
    }

    public boolean hasNativeType() {
        return delegate.hasNativeType();
    }

    public Date getNativeDate() {
        return delegate.getNativeDate();
    }

    public ICellComment getComment() {
        return delegate.getComment();
    }

}
