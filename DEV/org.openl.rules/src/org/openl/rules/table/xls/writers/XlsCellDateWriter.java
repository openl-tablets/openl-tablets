package org.openl.rules.table.xls.writers;

import java.util.Date;

import org.apache.poi.ss.usermodel.BuiltinFormats;

import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.formatters.FormatConstants;

public class XlsCellDateWriter extends AXlsCellWriter {

    public XlsCellDateWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        var dateValue = (Date) getValueToWrite();
        PoiExcelHelper.setDate(getCellToWrite(), dateValue);

        PoiExcelHelper.setCellDataFormat(getCellToWrite(),
                BuiltinFormats.getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT));
    }

}
