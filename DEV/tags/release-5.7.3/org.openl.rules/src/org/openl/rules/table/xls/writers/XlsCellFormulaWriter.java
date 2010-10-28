package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellFormulaWriter extends AXlsCellWriter {

    public XlsCellFormulaWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);        
    }

    @Override
    public void writeCellValue(boolean writeMetaInfo) {
        getCellToWrite().setCellFormula(getStringValue().replaceFirst("=", ""));

        try {
            PoiExcelHelper.evaluateFormula((getCellToWrite()));
        } catch (Exception e) {
        }

        if (writeMetaInfo) {
            setMetaInfo(String.class);
        }
    }

}
