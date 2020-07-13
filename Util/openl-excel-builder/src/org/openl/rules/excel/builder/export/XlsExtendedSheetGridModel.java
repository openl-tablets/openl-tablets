package org.openl.rules.excel.builder.export;

import org.apache.poi.ss.util.CellRangeAddress;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsExtendedSheetGridModel extends XlsSheetGridModel implements IWritableExtendedGrid {

    public XlsExtendedSheetGridModel(XlsSheetSourceCodeModule sheetSource) {
        super(sheetSource);
    }

    @Override
    public void addMergedRegionUnsafe(IGridRegion reg) {
        Object topLeftCellValue = findFirstValueInRegion(reg);
        for (int row = reg.getTop(); row <= reg.getBottom(); row++) {
            for (int column = reg.getLeft(); column <= reg.getRight(); column++) {
                if (column != reg.getLeft() || row != reg.getTop()) {
                    clearCellValue(column, row);
                }
            }
        }
        setCellValue(reg.getLeft(), reg.getTop(), topLeftCellValue);
        getMergedRegionsPool().add(reg);
        getSheet()
                .addMergedRegionUnsafe(new CellRangeAddress(reg.getTop(), reg.getBottom(), reg.getLeft(), reg.getRight()));
    }
}
