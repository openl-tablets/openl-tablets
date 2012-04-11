package org.openl.rules.table.xls;

import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.table.IGridRegion;

class XlsGridRegion implements IGridRegion {

    CellRangeAddress poiXlsRegion;

    XlsGridRegion(CellRangeAddress poiXlsRegion) {
        this.poiXlsRegion = poiXlsRegion;
    }

    public int getBottom() {
        return poiXlsRegion.getLastRow();
    }

    public int getLeft() {
        return poiXlsRegion.getFirstColumn();
    }

    public int getRight() {
        return poiXlsRegion.getLastColumn();
    }

    public int getTop() {
        return poiXlsRegion.getFirstRow();
    }

}