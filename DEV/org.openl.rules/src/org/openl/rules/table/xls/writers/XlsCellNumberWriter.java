package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.DateUtil;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.FormatConstants;

/**
 * Writes a number into a cell.
 *
 * <p>A number a {@code double} holds exactly is written as a number, and anything else as a text that reads back as
 * the same number, as {@link PoiExcelHelper#setNumber(org.apache.poi.ss.usermodel.Cell, Number)} writes it.
 */
public class XlsCellNumberWriter extends AXlsCellWriter {

    public XlsCellNumberWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        var cellToWrite = getCellToWrite();
        if (PoiExcelHelper.setNumber(cellToWrite, (Number) getValueToWrite())
                && DateUtil.isCellDateFormatted(cellToWrite)) {
            // Previously the cell was formatted as a date. Change format to number.
            PoiExcelHelper.setCellDataFormat(cellToWrite,
                    BuiltinFormats.getBuiltinFormat(FormatConstants.GENERAL_FORMAT));
        }
    }

}
