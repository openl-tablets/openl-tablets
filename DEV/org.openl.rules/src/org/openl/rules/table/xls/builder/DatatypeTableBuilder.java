package org.openl.rules.table.xls.builder;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.StringUtils;

/**
 * The class is responsible for creating Datatype tables.
 *
 * @author Andrei Astrouski
 */
public class DatatypeTableBuilder extends TableBuilder {

    public static final int MIN_WIDTH = 2; // Param type + Param name

    public void beginTable(int height) throws CreateTableException {
        super.beginTable(MIN_WIDTH, height);
    }

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets
     */
    public DatatypeTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }

    @Override
    public void writeHeader(String tableName, ICellStyle style) {
        writeHeader(tableName, null, style);
    }

    public void writeHeader(String tableName, String parentType, ICellStyle style) {
        String header = IXlsTableNames.DATATYPE_TABLE;
        if (StringUtils.isNotBlank(tableName)) {
            header += " " + tableName;
        }
        if (StringUtils.isNotBlank(parentType)) {
            header += " extends " + parentType;
        }
        super.writeHeader(header, style);
    }

    public void writeHeader(String tableName, String parentType) {
        writeHeader(tableName, parentType, null);
    }

    public void writeParameter(String paramType, String paramName, ICellStyle cellStyle) {
        if (StringUtils.isBlank(paramType) || StringUtils.isBlank(paramName)) {
            throw new IllegalArgumentException("Parameter type and name must be not empty");
        }
        if (getTableRegion() == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }

        writeCell(0, getCurrentRow(), 1, 1, paramType, cellStyle);
        writeCell(1, getCurrentRow(), 1, 1, paramName, cellStyle);

        incCurrentRow();
    }

    public void writeParameter(String paramType, String paramName) {
        writeParameter(paramType, paramName, null);
    }

}
