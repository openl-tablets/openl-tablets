package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.Cell;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellStringWriter extends AXlsCellWriter {

    public XlsCellStringWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);        
    }

    @Override
    public void writeCellValue() {
        getCellToWrite().setCellType(Cell.CELL_TYPE_BLANK);
        getCellToWrite().setCellValue(getStringValue());
    }

}
