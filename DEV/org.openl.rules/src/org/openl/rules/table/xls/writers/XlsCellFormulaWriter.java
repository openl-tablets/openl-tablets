package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.Cell;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsCellFormulaWriter extends AXlsCellWriter {

    private final Logger log = LoggerFactory.getLogger(XlsCellFormulaWriter.class);

    public XlsCellFormulaWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue(boolean writeMetaInfo) {
        String formula = getStringValue();
        try {
            writeExcelFormula(formula.replaceFirst("=", ""));
        } catch (Exception e) {
            // if the setting of Excel formula have been failed then we have
            // OpenL formula
            //TODO make separate writers and editors for OpenL and Excel Formulas
            writeOpenLFormula(formula);
        }
        if (writeMetaInfo) {
            setMetaInfo(String.class);
        }
    }

    private void writeExcelFormula(String formula) {
        getCellToWrite().setCellFormula(formula);

        try {
            PoiExcelHelper.evaluateFormula((getCellToWrite()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void writeOpenLFormula(String formula) {
        getCellToWrite().setCellType(Cell.CELL_TYPE_STRING);
        getCellToWrite().setCellValue(formula);
    }
}
