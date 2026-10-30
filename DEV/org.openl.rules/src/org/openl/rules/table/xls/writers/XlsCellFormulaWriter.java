package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * Writes an Excel formula, such as {@code =B2*C2}, into a cell.
 *
 * <p>The cell keeps the formula and the value it computes. A formula Excel cannot read, or one Apache POI cannot
 * compute, is refused, so it is never written as a text in its place.
 */
public class XlsCellFormulaWriter extends XlsCellStringWriter {

    public XlsCellFormulaWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    /**
     * Writes the formula into the cell.
     *
     * @throws IllegalArgumentException when Excel cannot read the formula or Apache POI cannot compute it
     */
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
        PoiExcelHelper.setFormula(cellToWrite, formula);
    }

}
