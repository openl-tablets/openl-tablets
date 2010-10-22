package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellFormulaWriter extends AXlsCellWriter {

    public XlsCellFormulaWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);        
    }

    @Override
    public void writeCellValue(boolean writeMetaInfo) {
        getCellToWrite().setCellFormula(getStringValue().replaceFirst("=", ""));

        // Evaluate formula to get new cell value.
        try {
            FormulaEvaluator formulaEvaluator = getXlsSheetGridModel().getSheetSource().getSheet().getWorkbook()
                .getCreationHelper().createFormulaEvaluator();
            formulaEvaluator.evaluateFormulaCell(getCellToWrite());
        } catch (Exception e) {
        }

        if (writeMetaInfo) {
            setMetaInfo(String.class);
        }
    }

}
