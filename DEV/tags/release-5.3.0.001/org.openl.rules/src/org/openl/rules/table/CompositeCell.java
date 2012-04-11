package org.openl.rules.table;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

class CompositeCell implements ICell {

    private int column;
    private int row;
    private IGridRegion region;
    private ICell delegate;

    public CompositeCell(int column, int row, IGridRegion region, ICell delegate) {
        this.column = column;
        this.row = row;
        this.region = region;
        this.delegate = delegate;  
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
        return delegate.getHeight();
    }
    
    public Object getObjectValue() {
        return delegate.getObjectValue();
    }
    
    public String getStringValue() {
        return delegate.getStringValue();
    }

    public int getWidth() {
        return delegate.getWidth();
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

}
