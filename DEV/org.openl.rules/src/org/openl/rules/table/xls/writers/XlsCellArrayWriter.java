package org.openl.rules.table.xls.writers;

import java.util.Arrays;

import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.StringUtils;

public class XlsCellArrayWriter extends AXlsCellWriter {

    public XlsCellArrayWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        Object[] values = (Object[]) getValueToWrite();
        getCellToWrite().setCellValue(
            StringUtils.join(Arrays.stream(values).map(e -> e == null ? StringUtils.EMPTY : e).toArray(), ","));
    }

}
