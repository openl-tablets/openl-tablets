package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellNumberWriter extends AXlsCellWriter{

    public XlsCellNumberWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue(boolean writeMetaInfo) {
        Number numberValue = (Number) getValueToWrite();
        getCellToWrite().setCellValue(numberValue.doubleValue());

        if (writeMetaInfo) {
            // We need to set cell meta info for the cell, to open appropriate editor for it on UI.
            setMetaInfo(numberValue.getClass());
        }
    }

}
