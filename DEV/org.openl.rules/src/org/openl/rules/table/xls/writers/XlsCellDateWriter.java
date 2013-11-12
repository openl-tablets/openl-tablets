package org.openl.rules.table.xls.writers;

import java.util.Date;

import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.XlsDateFormatter;

public class XlsCellDateWriter extends AXlsCellWriter{
    
    public XlsCellDateWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    public void writeCellValue(boolean writeMetaInfo) {        
        Date dateValue = (Date) getValueToWrite();
        getCellToWrite().setCellValue(dateValue);

        CellStyle previousStyle = getCellToWrite().getCellStyle();
        getCellToWrite().setCellStyle(getXlsSheetGridModel().getSheetSource().getSheet().getWorkbook().createCellStyle());
        getCellToWrite().getCellStyle().cloneStyleFrom(previousStyle);
        getCellToWrite().getCellStyle().setDataFormat((short) BuiltinFormats
                .getBuiltinFormat(XlsDateFormatter.DEFAULT_XLS_DATE_FORMAT));

        if (writeMetaInfo) {
            setMetaInfo(Date.class);
        }
    }

}
