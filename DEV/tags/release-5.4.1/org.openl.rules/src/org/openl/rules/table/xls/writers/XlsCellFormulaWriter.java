package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellFormulaWriter extends AXlsCellWriter {

    public XlsCellFormulaWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);        
    }

    @Override
    public void writeCellValue() {
        getCellToWrite().setCellFormula(getStringValue().replaceFirst("=", ""));
        
        setMetaInfo(String.class);
    }

}
