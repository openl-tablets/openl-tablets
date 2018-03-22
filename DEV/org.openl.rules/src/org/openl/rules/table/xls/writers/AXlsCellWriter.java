package org.openl.rules.table.xls.writers;

import org.apache.poi.ss.usermodel.Cell;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.types.java.JavaOpenClass;

public abstract class AXlsCellWriter {
    
    public static final String ARRAY_WRITER = "Array Writer";
    public static final String BOOLEAN_WRITER = "Boolean Writer";
    public static final String DATE_WRITER = "Date Writer";
    
    
    public static final String ENUM_ARRAY_WRITER = "Enum Array Writer";
    public static final String ENUM_WRITER = "Enum Writer";
    public static final String FORMULA_WRITER = "Formula Writer";
    public static final String NUMBER_WRITER = "Number Writer";
    public static final String STRING_WRITER = "String Writer";    
    
    private XlsSheetGridModel xlsSheetGridModel;
    private Cell cellToWrite;
    private Object valueToWrite;
    private String strValue;
    
    public AXlsCellWriter(XlsSheetGridModel xlsSheetGridModel) {        
        this.xlsSheetGridModel = xlsSheetGridModel;
    }

    public XlsSheetGridModel getXlsSheetGridModel() {
        return xlsSheetGridModel;
    }

    public void setXlsSheetGridModel(XlsSheetGridModel xlsSheetGridModel) {
        this.xlsSheetGridModel = xlsSheetGridModel;
    }

    public Cell getCellToWrite() {
        return cellToWrite;
    }

    public void setCellToWrite(Cell cellToWrite) {
        this.cellToWrite = cellToWrite;
    }

    public Object getValueToWrite() {
        return valueToWrite;
    }

    public void setValueToWrite(Object valueToWrite) {
        this.valueToWrite = valueToWrite;
        this.strValue = String.valueOf(valueToWrite);
    }

    protected String getStringValue() {
        return strValue;
    }

    public abstract void writeCellValue(boolean writeMetaInfo);

    protected void setMetaInfo(Class<?> valueClass, boolean multiValue) {
        // We need to set cell meta info for the cell, to open appropriate editor for it on UI.
        CellMetaInfo cellMeta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, strValue,
                JavaOpenClass.getOpenClass(valueClass), multiValue);
        xlsSheetGridModel.getCell(cellToWrite.getColumnIndex(), cellToWrite.getRowIndex()).setMetaInfo(cellMeta);
    }

    protected void setMetaInfo(Class<?> valueClass) {
        setMetaInfo(valueClass, false);
    }

}
