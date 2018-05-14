package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellEnumWriter extends AXlsCellWriter {

    public XlsCellEnumWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        getCellToWrite().setCellValue(((Enum<?>) getValueToWrite()).name());
    }

}
