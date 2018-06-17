package org.openl.rules.table.xls.writers;

import java.util.Date;

import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.FormatConstants;

public class XlsCellDateWriter extends AXlsCellWriter{
    
    public XlsCellDateWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    public void writeCellValue() {
        Date dateValue = (Date) getValueToWrite();
        getCellToWrite().setCellValue(dateValue);

        CellStyle previousStyle = getCellToWrite().getCellStyle();
        getCellToWrite().setCellStyle(PoiExcelHelper.createCellStyle(getXlsSheetGridModel().getSheetSource().getSheet().getWorkbook()));
        getCellToWrite().getCellStyle().cloneStyleFrom(previousStyle);
        getCellToWrite().getCellStyle().setDataFormat((short) BuiltinFormats
                .getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));
    }

}
