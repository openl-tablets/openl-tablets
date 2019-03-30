package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.CellType;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellStringWriter extends AXlsCellWriter {

    public XlsCellStringWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);        
    }

    @Override
    public void writeCellValue() {
        getCellToWrite().setCellType(CellType.BLANK);
        getCellToWrite().setCellValue(getStringValue());
    }

}
