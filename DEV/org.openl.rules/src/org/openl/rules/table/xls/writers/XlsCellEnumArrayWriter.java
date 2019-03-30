package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

public class XlsCellEnumArrayWriter extends AXlsCellWriter {

    public XlsCellEnumArrayWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        Object[] enums = (Object[]) getValueToWrite();
        String[] names = EnumUtils.getNames(enums);
        getCellToWrite().setCellValue(StringUtils.join(names, ","));
    }

}
