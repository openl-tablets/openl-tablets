package org.openl.rules.table.xls.builder;

import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.util.StringUtils;

/**
 * The class is responsible for creating Data tables.
 * 
 * @author NSamatov
 */
public class DataTableBuilder extends TableBuilder {
    public static final int MIN_WIDTH = 1; // "this" field
    private static final int NOT_INITIALIZED = -1;

    private List<? extends DataTableField> fields;
    private int fieldNameRow = NOT_INITIALIZED;
    private int foreignKeyRow = NOT_INITIALIZED;
    private int businessNameRow = NOT_INITIALIZED;

    /**
     * Creates new instance.
     * 
     * @param gridModel represents interface for operations with excel sheets
     */
    public DataTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }

    /**
     * Write a header of a data table
     * 
     * @param typeName type name
     * @param variableName technical variable name
     * @param style cell style (can be null)
     */
    public void writeHeader(String typeName, String variableName, ICellStyle style) {
        String header = IXlsTableNames.DATA_TABLE;

        if (StringUtils.isNotBlank(typeName)) {
            header += " " + typeName;
        }

        if (StringUtils.isNotBlank(variableName)) {
            header += " " + variableName;
        }

        super.writeHeader(header, style);
    }

    /**
     * Write a header of a data table
     * 
     * @param typeName type name
     * @param variableName technical variable name
     */
    public void writeHeader(String typeName, String variableName) {
        writeHeader(typeName, variableName, null);
    }

    /**
     * Write a field's description for a given data table (field technical and
     * business names and their foreign keys).
     * 
     * @param fields a fields list
     */
    public void writeFieldNames(List<? extends DataTableField> fields) {
        if (getTableRegion() == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }

        this.fields = fields;

        boolean hasForeignKeys = hasForeignKeys(fields);

        fieldNameRow = getCurrentRow();
        foreignKeyRow = hasForeignKeys ? fieldNameRow + 1 : NOT_INITIALIZED;
        businessNameRow = hasForeignKeys ? fieldNameRow + 2 : fieldNameRow + 1;

        writeFields("", fields, 0);

        incCurrentRow(hasForeignKeys ? 3 : 2);
    }

    /**
     * Write a field values
     * 
     * @param fieldValues a map containing values where a key is fully qualified
     *            field name
     * @param style cell style (can be null)
     */
    public void writeFieldValues(Map<String, String> fieldValues, ICellStyle cellStyle) {
        writeFieldValues(fields, fieldValues, cellStyle);
        incCurrentRow();
    }

    private boolean hasForeignKeys(List<? extends DataTableField> fields) {
        for (DataTableField field : fields) {
            boolean hasForeignKey;

            if (field.isFillChildren()) {
                hasForeignKey = hasForeignKeys(field.getAggregatedFields());
            } else {
                hasForeignKey = StringUtils.isNotBlank(field.getForeignKey());
            }

            if (hasForeignKey) {
                return true;
            }
        }

        return false;
    }

    private int writeFields(String prefix, List<? extends DataTableField> fields, int column) {
        int col = column;

        for (DataTableField field : fields) {
            if (StringUtils.isBlank(field.getName())) {
                throw new IllegalArgumentException("Field name must be not empty");
            }

            if (field.isFillChildren()) {
                col = writeFields(prefix + field.getName() + ".", field.getAggregatedFields(), col);
            } else {
                writeCell(col, fieldNameRow, 1, 1, prefix + field.getName());
                if (foreignKeyRow != NOT_INITIALIZED) {
                    writeCell(col, foreignKeyRow, 1, 1, parseForeignKey(field.getForeignKey()));
                }
                writeCell(col, businessNameRow, 1, 1, field.getBusinessName());
                col++;
            }
        }

        return col;
    }

    private String parseForeignKey(String foreignKey) {
        if (StringUtils.isNotBlank(foreignKey)) {
            if (foreignKey.startsWith(">"))
                return foreignKey;

            int dotPos = foreignKey.indexOf('.');
            String tableName = dotPos > 0 ? foreignKey.substring(0, dotPos) : foreignKey;
            String columnName = dotPos > 0 ? " " + foreignKey.substring(dotPos + 1) : "";
            return ">" + tableName + columnName;
        }

        return "";
    }

    private void writeFieldValues(List<? extends DataTableField> fields, Map<String, String> fieldValues, ICellStyle cellStyle) {
        for (int i = 0; i < fields.size(); i++) {
            DataTableField field = fields.get(i);
            if (field.isFillChildren()) {
                writeFieldValues(field.getAggregatedFields(), fieldValues, cellStyle);
            } else {
                String fieldName = field.getName();

                writeCell(i, getCurrentRow(), 1, 1, fieldValues.get(fieldName), cellStyle);
            }
        }
    }
}
