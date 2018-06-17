package org.openl.extension.xmlrules.syntax;

import java.util.Date;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.*;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsUtil;

public class SimpleCell implements ICell {

    private int column;
    private int row;

    private int width = 1;
    private int height = 1;

    private String stringValue;

    private CellMetaInfo metaInfo;

    public SimpleCell(int column, int row, String stringValue) {
        this.row = row;
        this.column = column;
        this.stringValue = stringValue;
    }

    public SimpleCell(int column, int row, int width, int height, String stringValue) {
        this.column = column;
        this.row = row;
        this.width = width;
        this.height = height;
        this.stringValue = stringValue;
    }

    public int getAbsoluteColumn() {
        return getColumn();
    }

    public int getAbsoluteRow() {
        return getRow();
    }

    public IGridRegion getAbsoluteRegion() {
        IGridRegion absoluteRegion = getRegion();
        if (absoluteRegion == null) {
            absoluteRegion = new GridRegion(row, column, row, column);
        }
        return absoluteRegion;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int col) {
        this.column = col;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public ICellStyle getStyle() {
        return null;
    }

    public Object getObjectValue() {
        return getStringValue();
    }

    public String getStringValue() {
        return stringValue;
    }

    public ICellFont getFont() {
        return null;
    }

    public IGridRegion getRegion() {
        return height == 1 && width == 1 ? null : new GridRegion(row, column, row + height - 1, column + width - 1);
    }

    public String getFormula() {
        return getStringValue();
    }

    public int getType() {
        // TODO Add other types support, change getObjectValue() too
        return IGrid.CELL_TYPE_STRING;
    }

    public String getUri() {
        return XlsUtil.xlsCellPresentation(column, row);
    }

    public boolean getNativeBoolean() {
        throw new UnsupportedOperationException();
    }

    public Date getNativeDate() {
        throw new UnsupportedOperationException();
    }
    public double getNativeNumber() {
        throw new UnsupportedOperationException();
    }

    public int getNativeType() {
        // TODO Add other types support, change getObjectValue() too
        return IGrid.CELL_TYPE_STRING;
    }

    public boolean hasNativeType() {
        return false;
    }

    public CellMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(CellMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public ICellComment getComment() {
        return null;
    }

    @Override
	public ICell getTopLeftCellFromRegion() {
		// TODO implementation should be updated once we support native types in SimpleCell
		return this;
	}

}
