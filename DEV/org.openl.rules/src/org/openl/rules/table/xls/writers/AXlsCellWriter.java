package org.openl.rules.table.xls.writers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;

import org.openl.rules.table.xls.XlsSheetGridModel;

@RequiredArgsConstructor
public abstract class AXlsCellWriter {

    public static final String ARRAY_WRITER = "Array Writer";
    public static final String BOOLEAN_WRITER = "Boolean Writer";
    public static final String DATE_WRITER = "Date Writer";

    public static final String ENUM_ARRAY_WRITER = "Enum Array Writer";
    public static final String ENUM_WRITER = "Enum Writer";
    public static final String FORMULA_WRITER = "Formula Writer";
    public static final String NUMBER_WRITER = "Number Writer";
    public static final String STRING_WRITER = "String Writer";

    @Getter
    private final XlsSheetGridModel xlsSheetGridModel;
    @Getter
    @Setter
    private Cell cellToWrite;
    @Getter
    private Object valueToWrite;
    private String strValue;

    public void setValueToWrite(Object valueToWrite) {
        this.valueToWrite = valueToWrite;
        this.strValue = String.valueOf(valueToWrite);
    }

    protected String getStringValue() {
        return strValue;
    }

    public abstract void writeCellValue();

}
