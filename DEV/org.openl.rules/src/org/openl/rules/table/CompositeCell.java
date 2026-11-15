package org.openl.rules.table;

import java.util.Date;
import java.util.List;

import lombok.Getter;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.TextRun;

public class CompositeCell implements ICell {

    @Getter
    private final int column;
    @Getter
    private final int row;
    @Getter
    private final IGridRegion region;
    private final ICell delegate;
    private final IGridTable gridTable;

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
    public ICellStyle getStyle() {
        return delegate.getStyle();
    }

    @Override
    public ICellFont getFont() {
        return delegate.getFont();
    }

    @Override
    public List<TextRun> getTextRuns() {
        return delegate.getTextRuns();
    }

    /**
     * The height of the cell in this grid. A merged region of this grid is laid out as this grid shows it already;
     * a cell of the sheet of a transposed table is as high as it is wide on its sheet.
     */
    @Override
    public int getHeight() {
        if (region != null) {
            return GridRegionUtils.height(region);
        }
        return gridTable.isNormalOrientation() ? delegate.getHeight() : delegate.getWidth();
    }

    /**
     * The width of the cell in this grid. A merged region of this grid is laid out as this grid shows it already;
     * a cell of the sheet of a transposed table is as wide as it is high on its sheet.
     */
    @Override
    public int getWidth() {
        if (region != null) {
            return GridRegionUtils.width(region);
        }
        return gridTable.isNormalOrientation() ? delegate.getWidth() : delegate.getHeight();
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
