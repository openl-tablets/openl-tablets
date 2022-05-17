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
        /*
         * FIXME if cell has a RichText content like:
         * <xml-fragment t="inlineStr" xmlns:main="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
         *   <main:is>
         *    <main:t>= ""</main:t>
         *  </main:is>
         * </xml-fragment>
         * Apache POI 5.0 doesn't remove it and set new values next to <main:is> element.
         * As a result cell is in invalid state because contains old values and new.
         * Set cell to blank to fix this issue and fully override old values
         */
        cellToWrite.setBlank();
        try {
            String excelFormula = formula.replaceFirst("=", "");
            cellToWrite.setCellFormula(excelFormula);
            PoiExcelHelper.evaluateFormula(cellToWrite);
        } catch (Exception e) {
            // if the setting of Excel formula have been failed then we have
            // OpenL formula
            // TODO make separate writers and editors for OpenL and Excel
            // Formulas
            cellToWrite.setCellValue(formula);
        }
    }

}
