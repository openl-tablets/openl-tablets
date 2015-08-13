package org.openl.extension.xmlrules.model.single;

import org.openl.extension.xmlrules.model.XlsRegion;

public class XlsRegionImpl implements XlsRegion {
    private int column;
    private int row;
    private Integer width;
    private Integer height;

    @Override
    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @Override
    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
