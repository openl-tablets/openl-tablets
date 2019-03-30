package org.openl.rules.table.xls;

import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.table.IGridRegion;

/* internal */ class XlsGridRegion implements IGridRegion {

    private CellRangeAddress poiXlsRegion;

    
    /* internal */ XlsGridRegion(CellRangeAddress poiXlsRegion) {
        this.poiXlsRegion = poiXlsRegion;
    }

    public CellRangeAddress getPoiXlsRegion() {
        return poiXlsRegion;
    }
    
    @Override
    public int getBottom() {
        return poiXlsRegion.getLastRow();
    }

    @Override
    public int getLeft() {
        return poiXlsRegion.getFirstColumn();
    }

    @Override
    public int getRight() {
        return poiXlsRegion.getLastColumn();
    }

    @Override
    public int getTop() {
        return poiXlsRegion.getFirstRow();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getBottom();
        result = prime * result + getLeft();
        result = prime * result + getRight();
        result = prime * result + getTop();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XlsGridRegion other = (XlsGridRegion) obj;
        if (getBottom() != other.getBottom())
            return false;
        if (getLeft() != other.getLeft())
            return false;
        if (getRight() != other.getRight())
            return false;
        if (getTop() != other.getTop())
            return false;
        return true;
    }

}