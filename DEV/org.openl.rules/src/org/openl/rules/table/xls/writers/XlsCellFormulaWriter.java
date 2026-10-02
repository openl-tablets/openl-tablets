package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellFormulaWriter extends XlsCellStringWriter {

    public XlsCellFormulaWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        var formula = getStringValue();
        var cellToWrite = getCellToWrite();
        /*
         * The cell is blanked first, so that a formula fully overrides an inline rich text value like:
         * <xml-fragment t="inlineStr" xmlns:main="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
         *   <main:is>
         *    <main:t>= ""</main:t>
         *  </main:is>
         * </xml-fragment>
         * Apache POI 5.0 keeps such a value and writes the new one next to the <main:is> element, which leaves the cell
         * holding both.
         */
        cellToWrite.setBlank();
        try {
            var excelFormula = formula.replaceFirst("=", "");
            cellToWrite.setCellFormula(excelFormula);
            PoiExcelHelper.evaluateFormula(cellToWrite);
        } catch (Exception e) {
            // A formula Excel cannot read is an OpenL formula, which is written as text.
            super.writeCellValue();
        }
    }

}
