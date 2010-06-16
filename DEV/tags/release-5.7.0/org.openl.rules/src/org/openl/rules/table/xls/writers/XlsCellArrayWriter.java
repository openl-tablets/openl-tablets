package org.openl.rules.table.xls.writers;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;

public class XlsCellArrayWriter extends AXlsCellWriter {

    public XlsCellArrayWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue(boolean writeMetaInfo) {
        Object[] values = (Object[]) getValueToWrite();
        getCellToWrite().setCellValue(StringUtils.join(values, ","));

        if (writeMetaInfo) {
            Class<?> valueClass = getValueToWrite().getClass().getComponentType();
            setMetaInfo(valueClass, true);
        }
    }

}
