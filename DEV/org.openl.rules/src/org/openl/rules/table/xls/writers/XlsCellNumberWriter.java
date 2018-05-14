package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.FormatConstants;

public class XlsCellNumberWriter extends AXlsCellWriter {

    public XlsCellNumberWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        Number numberValue = (Number) getValueToWrite();
        Cell cellToWrite = getCellToWrite();
        cellToWrite.setCellValue(numberValue.doubleValue());

        if (DateUtil.isCellDateFormatted(cellToWrite)) {
            // Previously the cell was formatted as a date. Change format to number.
            CellStyle previousStyle = cellToWrite.getCellStyle();
            CellStyle newStyle = PoiExcelHelper.createCellStyle(getXlsSheetGridModel()
                    .getSheetSource()
                    .getSheet()
                    .getWorkbook());
            cellToWrite.setCellStyle(newStyle);
            newStyle.cloneStyleFrom(previousStyle);
            newStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat(FormatConstants.GENERAL_FORMAT));
        }
    }

}
