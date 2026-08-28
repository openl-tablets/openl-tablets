package org.openl.rules.table.xls.writers;

import java.util.Arrays;

import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

public class XlsCellEnumArrayWriter extends AXlsCellWriter {

    public XlsCellEnumArrayWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);
    }

    @Override
    public void writeCellValue() {
        var enums = (Object[]) getValueToWrite();
        var names = Arrays.stream(enums)
                .map(value -> value == null ? StringUtils.EMPTY : EnumUtils.getName((Enum<?>) value))
                .toArray(String[]::new);
        getCellToWrite().setCellValue(String.join(",", names));
    }

}
