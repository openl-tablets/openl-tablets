package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.Cell;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellFormulaWriter extends AXlsCellWriter {

    public XlsCellFormulaWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        String formula = getStringValue();
        Cell cellToWrite = getCellToWrite();
        try {
            String excelFormula = formula.replaceFirst("=", "");
            cellToWrite.setCellFormula(excelFormula);
            PoiExcelHelper.evaluateFormula(cellToWrite);
        } catch (Exception e) {
            // if the setting of Excel formula have been failed then we have
            // OpenL formula
            // TODO make separate writers and editors for OpenL and Excel
            // Formulas
            cellToWrite.setCellType(Cell.CELL_TYPE_STRING);
            cellToWrite.setCellValue(formula);
        }
    }

}
