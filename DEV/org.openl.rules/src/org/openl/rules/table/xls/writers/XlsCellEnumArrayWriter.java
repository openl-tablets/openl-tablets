package org.openl.rules.table.xls.writers;

import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

public class XlsCellEnumArrayWriter extends AXlsCellWriter {

    public XlsCellEnumArrayWriter(XlsSheetGridModel xlsSheetGridModel) {
        super(xlsSheetGridModel);       
    }

    @Override
    public void writeCellValue(boolean writeMetaInfo) {
        Object[] enums = (Object[]) getValueToWrite();
        String[] names = EnumUtils.getNames(enums);
        getCellToWrite().setCellValue(StringUtils.join(names, ","));

        if (writeMetaInfo) {
            // We have an array of Enums. we need to set as meta info information that domain class is Enum, so we 
            // need to take a component class and multiValue to true as it is an array.
            Class<?> valueClass = getValueToWrite().getClass().getComponentType();
            setMetaInfo(valueClass, true);
        }
    }

}
