package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellStringWriter extends AXlsCellWriter {

    public XlsCellStringWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        removeFormulaIfPresent();
        getCellToWrite().setCellValue(getStringValue());
    }

}
