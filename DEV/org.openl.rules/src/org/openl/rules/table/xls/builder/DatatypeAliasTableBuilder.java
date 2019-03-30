package org.openl.rules.table.xls.builder;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.StringUtils;

/**
 * The class is responsible for creating Datatype Alias tables.
 *
 * @author Andrei Astrouski
 */
public class DatatypeAliasTableBuilder extends TableBuilder {

    public static final int MIN_WIDTH = 1;

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets
     */
    public DatatypeAliasTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }

    public void beginTable(int height) throws CreateTableException {
        super.beginTable(MIN_WIDTH, height);
    }

    public void writeHeader(String tableName, String aliasType, ICellStyle style) {
        String header = IXlsTableNames.DATATYPE_TABLE;
        if (StringUtils.isNotBlank(tableName)) {
            header += (" " + tableName);
        }
        if (StringUtils.isNotBlank(aliasType)) {
            header += (" <" + aliasType + ">");
        }
        super.writeHeader(header, style);
    }

    public void writeHeader(String tableName, String aliasType) {
        writeHeader(tableName, aliasType, null);
    }

    @Override
    public void writeHeader(String header, ICellStyle style) {
        writeHeader(header, null, style);
    }

    public void writeValue(String value, ICellStyle cellStyle) {
        if (getTableRegion() == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }

        writeCell(0, getCurrentRow(), 1, 1, value, cellStyle);

        incCurrentRow();
    }

    public void writeValue(String value) {
        writeValue(value, null);
    }

}
