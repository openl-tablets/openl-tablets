package org.openl.rules.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class DataTableBindHelper {

    private static final char INDEX_ROW_REFERENCE_START_SYMBOL = '>';

    private static final String FPK = "_PK_";
    private static final String CONSTRUCTOR_FIELD = "this";

    private static final String CODE_DELIMETERS = ". \n\r";
    private static final String INDEX_ROW_REFERENCE_DELIMITER = " >\n\r";

    /**
     * Foreign keys row is optional for data table. It consists reference for
     * field value to other table. Foreign keys always starts from
     * {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
     * 
     * @param dataTable
     * @return <code>TRUE</code> if second row in data table body (next to the
     *         field row) consists even one value, in any column, starts with
     *         {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
     */
    public static boolean hasForeignKeysRow(ILogicalTable dataTable) {

        ILogicalTable potentialForeignKeysRow = dataTable.rows(1, 1);

        int columnsCount = potentialForeignKeysRow.getLogicalWidth();

        for (int i = 0; i < columnsCount; i++) {

            ILogicalTable cell = potentialForeignKeysRow.getLogicalColumn(i);
            String value = cell.getGridTable().getCell(0, 0).getStringValue();

            if (value == null || value.trim().length() == 0) {
                continue;
            }

            return value.charAt(0) == INDEX_ROW_REFERENCE_START_SYMBOL;
        }

        return false;
    }

    /**
     * Gets the table body, by skipping the table header and properties
     * sections.
     * 
     * @param tsn
     * @return Table body without table header and properties section.
     */
    public static ILogicalTable getTableBody(TableSyntaxNode tsn) {

        int startRow = 0;

        if (!tsn.hasPropertiesDefinedInTable()) {
            startRow = 1;
        } else {
            startRow = 2;
        }

        return tsn.getTable().rows(startRow);
    }

    /**
     * Checks if table representation is horizontal. Horizontal is data table
     * where parameters are listed from left to right.</br> Example:
     * 
     * <table cellspacing="2">
     * <tr bgcolor="#ccffff">
     * <td align="center">param1</td>
     * <td align="center">param2</td>
     * <td align="center">param3</td>
     * </tr>
     * <tr bgcolor="#ffff99">
     * <td align="center"><b>param1 value</b></td>
     * <td align="center"><b>param2 value</b></td>
     * <td align="center"><b>param3 value</b></td>
     * </tr>
     * </table>
     * 
     * @param dataTableBody
     * @param tableType
     * @return <code>TRUE</code> if table is horizontal.
     */
    public static boolean isHorizontalTable(ILogicalTable dataTableBody, IOpenClass tableType) {

        // If data table body contains only one row, we consider it is vertical.
        //
        if (dataTableBody.getLogicalHeight() != 1) {
            int fieldsCount1 = countFields(dataTableBody, tableType);
            int fieldsCount2 = countFields(dataTableBody.transpose(), tableType);

            return fieldsCount1 >= fieldsCount2;
        }

        return false;
    }

    /**
     * Goes through the data table columns from left to right, and count number
     * of <code>{@link IOpenField}</code>.
     * 
     * @param dataTable
     * @param tableType
     * @return Number of <code>{@link IOpenField}</code> found in the data
     *         table.
     */
    private static int countFields(ILogicalTable dataTable, IOpenClass tableType) {

        int count = 0;
        int width = dataTable.getLogicalWidth();

        for (int i = 0; i < width; ++i) {

            String fieldName = dataTable.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();

            if (fieldName == null) {
                continue;
            }

            // Remove extra spaces.
            //
            fieldName = StringUtils.trim(fieldName);
            IOpenField field = findField(fieldName, null, tableType);

            if (field != null) {
                count += 1;
            }
        }

        return count;
    }

    public static IOpenField findField(String fieldName, ITable table, IOpenClass tableType) {

        if (FPK.equals(fieldName)) {
            return new PrimaryKeyField(FPK, table);
        }

        return tableType.getField(fieldName, true);
    }

    /**
     * Gets the horizontal table representation from current table. If it was
     * vertical it will be transposed.
     * 
     * @param tableBody
     * @param tableType
     * @return Horizontal representation of table.
     */
    public static ILogicalTable getHorizontalTable(ILogicalTable tableBody, IOpenClass tableType) {

        ILogicalTable resultTable = null;

        if (isHorizontalTable(tableBody, tableType)) {
            resultTable = tableBody;
        } else {
            resultTable = tableBody.transpose();
        }

        return resultTable;
    }

    /**
     * Gets the Data_With_Titles rows from the data table body. Data_With_Titles
     * start row consider to be the next row after descriptor section of the
     * table and till the end of the table.
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Data_With_Titles rows for current data table body.
     */
    public static ILogicalTable getDataWithTitleRows(ILogicalTable horizDataTableBody) {
        int startRow = getStartRowForDataWithTitlesSection(horizDataTableBody);

        return horizDataTableBody.rows(startRow);
    }

    /**
     * Gets the number of the start row for Data_With_Titles section of the data
     * table body. It depends on whether table has or no the foreign key row.
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Number of the start row for the Data_With_Titles section.
     */
    private static int getStartRowForDataWithTitlesSection(ILogicalTable horizDataTableBody) {

        boolean hasForeignKeysRow = hasForeignKeysRow(horizDataTableBody);

        if (hasForeignKeysRow) {
            // Data_With_Titles will starts from this row.
            //
            return 2;
        }

        // Data_With_Titles will starts from this row.
        //
        return 1;
    }

    /**
     * Gets the descriptor rows from the data table body. Descriptor rows are
     * obligatory parameter row and optional foreign key row if it exists in the
     * table.
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Descriptor rows for current data table body.
     */
    public static ILogicalTable getDescriptorRows(ILogicalTable horizDataTableBody) {

        int endRow = getEndRowForDescriptorSection(horizDataTableBody);

        return horizDataTableBody.rows(0, endRow);
    }

    /**
     * Gets the number of end row for descriptor section of the data table body.
     * It depends on whether table has or no the foreign key row.
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Number of end row for descriptor section.
     */
    private static int getEndRowForDescriptorSection(ILogicalTable horizDataTableBody) {

        boolean hasForeignKeysRow = hasForeignKeysRow(horizDataTableBody);

        if (hasForeignKeysRow) {

            // descriptorRows will consist fieldRow + iforeignKeyRow.
            //
            return 1;
        }

        // descriptorRows will consist only fieldRow.
        //
        return 0;
    }

    /**
     * Gets title for column if required or returns blank value.
     * 
     * @param dataWithTitleRows Logical part of the data table. Consider to
     *            include all rows from base table after header section
     *            (consists from header row + property section) and descriptor
     *            section (consists from JavaBean name obligatory + optional
     *            index row, see {@link #hasForeignKeysRow(ILogicalTable)}).<br>
     *            This part of table may consists from optional first title row
     *            and followed data rows.
     * @param column Number of column in data table.
     * @param hasColumnTitleRow Flag shows if data table has column tytle row.
     * @return Column title (aka Display name).
     */
    public static StringValue makeColumnTitle(IBindingContext bindingContext, ILogicalTable dataWithTitleRows,
            int column, boolean hasColumnTitleRow) {

        String value = StringUtils.EMPTY;

        if (hasColumnTitleRow) {

            ILogicalTable titleCell = dataWithTitleRows.getLogicalRegion(column, 0, 1, 1);
            value = titleCell.getGridTable().getCell(0, 0).getStringValue();

            // remove extra spaces
            value = StringUtils.trimToEmpty(value);

            return new StringValue(value, value, value, new GridCellSourceCodeModule(titleCell.getGridTable(), bindingContext));
        }

        return new StringValue(value, value, value, null);
    }

    public static ColumnDescriptor[] makeDescriptors(IBindingContext bindingContext, ITable table, IOpenClass type,
            OpenL openl, ILogicalTable descriptorRows, ILogicalTable dataWithTitleRows, boolean hasForeignKeysRow,
            boolean hasColumnTytleRow) throws Exception {

        int width = descriptorRows.getLogicalWidth();
        ColumnDescriptor[] columnDescriptors = new ColumnDescriptor[width];

        List<IdentifierNode[]> identifiers = new ArrayList<IdentifierNode[]>(width);

        for (int columnNum = 0; columnNum < width; columnNum++) {

            IGridTable gridTable = descriptorRows.getLogicalColumn(columnNum).getGridTable();
            GridCellSourceCodeModule cellSourceModule = new GridCellSourceCodeModule(gridTable, bindingContext);

            String code = cellSourceModule.getCode();

            if (code.length() != 0) {

                IdentifierNode[] fieldAccessorChainTokens = Tokenizer.tokenize(cellSourceModule, CODE_DELIMETERS);

                if (contains(identifiers, fieldAccessorChainTokens)) {
                    
                    String message = String.format("Found duplicate of field \"%s\"", code);
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                    if(table.getTableSyntaxNode() != null){
                        table.getTableSyntaxNode().addError(error);
                    }
                    BindHelper.processError(error);
                } else {
                    identifiers.add(fieldAccessorChainTokens);
                }

                // the chain of fields to access the target field, e.g. for
                // driver.name it will be array consisting of two fields:
                // 1st for driver, 2nd for name
                IOpenField[] fieldAccessorChain = new IOpenField[fieldAccessorChainTokens.length];

                // the first field can be found in type itself
                IOpenClass loadedFieldType = type;

                for (int currentFieldNumInChain = 0; currentFieldNumInChain < fieldAccessorChain.length; currentFieldNumInChain++) {

                    IdentifierNode currentFieldNameNode = fieldAccessorChainTokens[currentFieldNumInChain];
                    String fieldName = currentFieldNameNode.getIdentifier();

                    if (CONSTRUCTOR_FIELD.equals(fieldName) && fieldAccessorChain.length == 1) {
                        // targetType = loadedFieldType
                        break;
                    }

                    IOpenField field = getWritableField(currentFieldNameNode, table, loadedFieldType);
                    loadedFieldType = field.getType();
                    fieldAccessorChain[currentFieldNumInChain] = field;
                }

                // the target type is the last field type, e.g. in driver.name
                // the target type will be for name
                // IOpenClass targetType = loadedFieldType != null ?
                // loadedFieldType : type;

                // FIXME: If field is CONSTRUCTOR_FIELD then this variable will
                // be null and it's intended behavior. It should be rewritten as
                // it's very error prone.
                IOpenField field = fieldAccessorChain.length == 1 ? fieldAccessorChain[0] : new FieldChain(type,
                    fieldAccessorChain);

                IdentifierNode foreignKeyTable = null;
                IdentifierNode foreignKey = null;

                if (hasForeignKeysRow) {
                    IdentifierNode[] foreignKeyTokens = getForeignKeyTokens(bindingContext, descriptorRows, columnNum);

                    foreignKeyTable = foreignKeyTokens.length > 0 ? foreignKeyTokens[0] : null;
                    foreignKey = foreignKeyTokens.length > 1 ? foreignKeyTokens[1] : null;
                }

                StringValue header = DataTableBindHelper.makeColumnTitle(bindingContext, dataWithTitleRows, columnNum,
                        hasColumnTytleRow);

                ColumnDescriptor currentColumnDescriptor;

                if (foreignKeyTable != null) {
                    currentColumnDescriptor = new ForeignKeyColumnDescriptor(field,
                        foreignKeyTable,
                        foreignKey,
                        header,
                        openl);
                } else {
                    currentColumnDescriptor = new ColumnDescriptor(field, header, openl);
                }

                columnDescriptors[columnNum] = currentColumnDescriptor;
            }
        }

        return columnDescriptors;
    }

    /**
     * Returns foreign_key_tokens from the current column.
     * 
     * @param descriptorRows
     * @param columnNum
     * @return
     * @throws OpenLCompilationException
     * 
     * @see {@link #hasForeignKeysRow(ILogicalTable)}.
     */
    private static IdentifierNode[] getForeignKeyTokens(IBindingContext bindingContext, ILogicalTable descriptorRows, int columnNum) throws OpenLCompilationException {

        ILogicalTable logicalRegion = descriptorRows.getLogicalRegion(columnNum, 1, 1, 1);
        GridCellSourceCodeModule indexRowSourceModule = new GridCellSourceCodeModule(logicalRegion.getGridTable(), bindingContext);

        // Should be in format
        // "> reference_table_name [reference_table_key_column]"
        return Tokenizer.tokenize(indexRowSourceModule, INDEX_ROW_REFERENCE_DELIMITER);
    }

    /**
     * Gets the field, and if it is not <code>null</code> and isWritable,
     * returns it. In other cases throws an exception.
     * 
     * @param currentFieldNameNode
     * @param table
     * @param loadedFieldType
     * @return
     * @throws BoundError
     */
    private static IOpenField getWritableField(IdentifierNode currentFieldNameNode,
            ITable table,
            IOpenClass loadedFieldType) throws SyntaxNodeException {

        String fieldName = currentFieldNameNode.getIdentifier();
        IOpenField field = DataTableBindHelper.findField(fieldName, table, loadedFieldType);

        if (field == null) {
            String errorMessage = String.format("Field \"%s\" not found in %s", fieldName, loadedFieldType.getName());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, currentFieldNameNode);
        }

        if (!field.isWritable()) {
            String message = String.format("Field '%s' is not writable in %s", fieldName, loadedFieldType.getName());
            throw SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
        }

        return field;
    }

    private static boolean contains(List<IdentifierNode[]> identifiers, IdentifierNode[] identifier) {

        for (IdentifierNode[] existIdentifier : identifiers) {
            if (isEqualsIdentifier(existIdentifier, identifier)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEqualsIdentifier(IdentifierNode[] identifier1, IdentifierNode[] identifier2) {

        if (identifier1 == null || identifier2 == null) {
            return false;
        }

        if (identifier1.length != identifier2.length) {
            return false;
        }

        for (int i = 0; i < identifier1.length; i++) {
            if (!identifier1[i].getIdentifier().equals(identifier2[i].getIdentifier())) {
                return false;
            }
        }

        return true;
    }
}
