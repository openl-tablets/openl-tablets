package org.openl.rules.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultField;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.TableProperties;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.DatatypeArrayElementField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

public class DataTableBindHelper {

    private static final char INDEX_ROW_REFERENCE_START_SYMBOL = '>';

    private static final String FPK = "_PK_";

    /** Indicates that field is a constructor.<br> */
    // Protected to make javadoc reference.
    protected static final String CONSTRUCTOR_FIELD = "this";

    private static final String CODE_DELIMETERS = ". \n\r";
    private static final String INDEX_ROW_REFERENCE_DELIMITER = " >\n\r";
    private static final String LINK_DELIMETERS = ".";

    // patter for field like addressArry[0]
    public static final String ARRAY_ACCESS_PATTERN = ".+\\[[0-9]+\\]$";
    public static final String PRECISION_PATTERN = "^\\(\\-?[0-9]+\\)$";
    public static final String SPREADSHEETRESULTFIELD_PATTERN = "^\\$.+\\$.+$";
    
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

        ILogicalTable potentialForeignKeysRow = dataTable.getRows(1, 1);

        int columnsCount = potentialForeignKeysRow.getWidth();

        for (int i = 0; i < columnsCount; i++) {

            ILogicalTable cell = potentialForeignKeysRow.getColumn(i);
            String value = cell.getSource().getCell(0, 0).getStringValue();

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

        return tsn.getTable().getRows(startRow);
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
        if (dataTableBody.getHeight() != 1) {
            if (TableProperties.class.isAssignableFrom(tableType.getInstanceClass())) {
                // Properties are always vertical
                return false;
            }
            int fieldsCount1 = countChangeableFields(dataTableBody, tableType);
            int fieldsCount2 = countChangeableFields(dataTableBody.transpose(), tableType);

            return fieldsCount1 >= fieldsCount2;
        }

        return false;
    }

    /**
     * Goes through the data table columns from left to right, and count number
     * of changeable <code>{@link IOpenField}</code>.
     * 
     * @param dataTable
     * @param tableType
     * @return Number of <code>{@link IOpenField}</code> found in the data
     *         table.
     */
    private static int countChangeableFields(ILogicalTable dataTable, IOpenClass tableType) {

        int count = 0;
        int width = dataTable.getWidth();

        for (int i = 0; i < width; ++i) {

            String fieldName = dataTable.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (fieldName == null) {
                continue;
            }

            // Remove extra spaces.
            //
            fieldName = StringUtils.trim(fieldName);

            // if it is field chain get first token
            if (fieldName.indexOf(".") > 0) {
                fieldName = fieldName.substring(0, fieldName.indexOf("."));
            }
            // if it is array field correct field name
            if (fieldName.indexOf("[") > 0) {
                fieldName = fieldName.substring(0, fieldName.indexOf("["));
            }

            IOpenField field = findField(fieldName, null, tableType);

            if (field != null && !field.isConst() && field.isWritable()) {
                count += 1;
            }
        }

        return count;
    }

    public static IOpenField findField(String fieldName, ITable table, IOpenClass tableType) {

        if (FPK.equals(fieldName)) {
            // TODO: Remove it ASAP. USE _id_ instead
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

        if (tableBody != null) {
            if (isHorizontalTable(tableBody, tableType)) {
                resultTable = tableBody;
            } else {
                resultTable = tableBody.transpose();
            }
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
    public static ILogicalTable getHorizontalDataWithTitle(ILogicalTable horizDataTableBody) {
        int startIndex = getStartIndexForDataWithTitlesSection(horizDataTableBody);

        return horizDataTableBody.getRows(startIndex);
    }

    /**
     * Gets the sub table for displaying on business view.<br>
     * 
     * @param tableBody data table body.
     * @param tableType
     * @return Data_With_Titles section for current data table body.
     */
    public static ILogicalTable getSubTableForBusinessView(ILogicalTable tableBody, IOpenClass tableType) {
        if (isHorizontalTable(tableBody, tableType)) {
            return getHorizontalDataWithTitle(tableBody);
        } else {
            return getVerticalDataWithTitle(tableBody);
        }
    }

    /**
     * Gets the Data_With_Titles columns from the data table body.
     * Data_With_Titles start column consider to be the next column after
     * descriptor section of the table and till the end of the table.
     * 
     * @param verticalTableBody Vertical representation of data table body.
     * @return Data_With_Titles columns for current data table body.
     */
    private static ILogicalTable getVerticalDataWithTitle(ILogicalTable verticalTableBody) {
        ILogicalTable horizDataTableBody = verticalTableBody.transpose();
        int startIndex = getStartIndexForDataWithTitlesSection(horizDataTableBody);
        return verticalTableBody.getColumns(startIndex);
    }

    /**
     * Gets the start index of the Data_With_Titles section of the data table
     * body.<br>
     * It depends on whether table has or no the foreign key row.<br>
     * <br>
     * Works with horizontal representation of data table.
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Number of the start row for the Data_With_Titles section.
     */
    private static int getStartIndexForDataWithTitlesSection(ILogicalTable horizDataTableBody) {

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

        return horizDataTableBody.getRows(0, endRow);
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
     * @param bindingContext is used for optimization
     *            {@link GridCellSourceCodeModule} in execution mode. Can be
     *            <code>null</code>.
     * @param column Number of column in data table.
     * @param hasColumnTitleRow Flag shows if data table has column tytle row.
     * @return Column title (aka Display name).
     */
    public static StringValue makeColumnTitle(IBindingContext bindingContext,
            ILogicalTable dataWithTitleRows,
            int column,
            boolean hasColumnTitleRow) {

        String value = StringUtils.EMPTY;

        if (hasColumnTitleRow) {

            ILogicalTable titleCell = dataWithTitleRows.getSubtable(column, 0, 1, 1);
            value = titleCell.getSource().getCell(0, 0).getStringValue();

            // remove extra spaces
            value = StringUtils.trimToEmpty(value);

            return new StringValue(value, value, value, new GridCellSourceCodeModule(titleCell.getSource(),
                bindingContext));
        }

        return new StringValue(value, value, value, null);
    }

    /**
     * 
     * @param bindingContext is used for optimization
     *            {@link GridCellSourceCodeModule} in execution mode. Can be
     *            <code>null</code>.
     * @param table
     * @param type
     * @param openl
     * @param descriptorRows
     * @param dataWithTitleRows
     * @param hasForeignKeysRow
     * @param hasColumnTytleRow
     * @return
     * @throws Exception
     */
    public static ColumnDescriptor[] makeDescriptors(IBindingContext bindingContext,
            ITable table,
            IOpenClass type,
            OpenL openl,
            ILogicalTable descriptorRows,
            ILogicalTable dataWithTitleRows,
            boolean hasForeignKeysRow,
            boolean hasColumnTytleRow,
            boolean supportConstructorFields) throws Exception {

        int width = descriptorRows.getWidth();
        ColumnDescriptor[] columnDescriptors = new ColumnDescriptor[width];

        List<IdentifierNode[]> columnIdentifiers = getColumnIdentifiers(bindingContext, table, descriptorRows);

        for (int columnNum = 0; columnNum < columnIdentifiers.size(); columnNum++) {
            IdentifierNode[] fieldAccessorChainTokens = columnIdentifiers.get(columnNum);
            if (fieldAccessorChainTokens != null) {

                IOpenField descriptorField = null;

                // indicates if field is a constructor.
                boolean constructorField = false;

                IdentifierNode foreignKeyTable = null;
                IdentifierNode foreignKey = null;
                IdentifierNode[] accessorChainTokens = null;
                ICell foreignKeyCell = null;

                if (fieldAccessorChainTokens.length == 1 && !hasForeignKeysRow) {
                    // process single field in chain, e.g. driver;
                    IdentifierNode fieldNameNode = fieldAccessorChainTokens[0];

                    if (supportConstructorFields && CONSTRUCTOR_FIELD.equals(fieldNameNode.getIdentifier())) {
                        constructorField = true;
                    } else if (fieldNameNode.getIdentifier().matches(ARRAY_ACCESS_PATTERN)) {
                        descriptorField = getWritableArrayElement(fieldNameNode, table, type);
                    } else {
                        descriptorField = getWritableField(fieldNameNode, table, type);
                    }
                } else {
                    // process the chain of fields, e.g.
                    // driver.homeAdress.street;
                    descriptorField = processFieldsChain(table, type, fieldAccessorChainTokens);
                }
                if (hasForeignKeysRow) {
                    IdentifierNode[] foreignKeyTokens = getForeignKeyTokens(bindingContext, descriptorRows, columnNum);
                    foreignKeyTable = foreignKeyTokens.length > 0 ? foreignKeyTokens[0] : null;
                    foreignKey = foreignKeyTokens.length > 1 ? foreignKeyTokens[1] : null;
                    foreignKeyCell = descriptorRows.getSubtable(columnNum, 1, 1, 1).getSource().getCell(0, 0);

                    if (foreignKeyTable != null) {
                        accessorChainTokens = Tokenizer.tokenize(foreignKeyTable.getModule(),
                            LINK_DELIMETERS,
                            foreignKeyTable.getLocation());

                        if (!ArrayUtils.isEmpty(accessorChainTokens)) {
                            foreignKeyTable = accessorChainTokens.length > 0 ? accessorChainTokens[0] : null;
                        }
                    }
                }

                StringValue header = DataTableBindHelper.makeColumnTitle(bindingContext,
                    dataWithTitleRows,
                    columnNum,
                    hasColumnTytleRow);

                ColumnDescriptor currentColumnDescriptor = getColumnDescriptor(openl,
                    descriptorField,
                    constructorField,
                    foreignKeyTable,
                    foreignKey,
                    accessorChainTokens,
                    foreignKeyCell,
                    header,
                    fieldAccessorChainTokens);

                columnDescriptors[columnNum] = currentColumnDescriptor;
            }
        }
        return columnDescriptors;
    }

    /**
     * 
     * @param bindingContext is used for optimization
     *            {@link GridCellSourceCodeModule} in execution mode. Can be
     *            <code>null</code>.
     * @param table is needed only for error processing. Can be
     *            <code>null</code>.
     * @param descriptorRows
     * @return
     * @throws OpenLCompilationException
     */
    public static List<IdentifierNode[]> getColumnIdentifiers(IBindingContext bindingContext,
            ITable table,
            ILogicalTable descriptorRows) {
        int width = descriptorRows.getWidth();
        List<IdentifierNode[]> identifiers = new ArrayList<IdentifierNode[]>();
        for (int columnNum = 0; columnNum < width; columnNum++) {

            GridCellSourceCodeModule cellSourceModule = getCellSourceModule(descriptorRows, columnNum);
            cellSourceModule.update(bindingContext);

            String code = cellSourceModule.getCode();

            if (code.length() != 0) {

                IdentifierNode[] fieldAccessorChainTokens = null;
                try {
                    // fields names nodes
                    fieldAccessorChainTokens = Tokenizer.tokenize(cellSourceModule, CODE_DELIMETERS);
                } catch (OpenLCompilationException e) {
                    String message = String.format("Cannot parse field source \"%s\"", code);
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                    processError(table, error);
                }

                if (contains(identifiers, fieldAccessorChainTokens)) {
                    String message = String.format("Found duplicate of field \"%s\"", code);
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                    processError(table, error);
                } else {
                    identifiers.add(fieldAccessorChainTokens);
                }
            } else {
                identifiers.add(null);
            }
        }
        return identifiers;
    }

    private static GridCellSourceCodeModule getCellSourceModule(ILogicalTable descriptorRows, int columnNum) {
        IGridTable gridTable = descriptorRows.getColumn(columnNum).getSource();
        GridCellSourceCodeModule cellSourceModule = new GridCellSourceCodeModule(gridTable);
        return cellSourceModule;
    }

    private static ColumnDescriptor getColumnDescriptor(OpenL openl,
            IOpenField descriptorField,
            boolean constructorField,
            IdentifierNode foreignKeyTable,
            IdentifierNode foreignKey,
            IdentifierNode[] foreignKeyTableAccessorChainTokens,
            ICell foreignKeyCell,
            StringValue header,
            IdentifierNode[] fieldChainTokens) {
        ColumnDescriptor currentColumnDescriptor;

        if (foreignKeyTable != null) {
            currentColumnDescriptor = new ForeignKeyColumnDescriptor(descriptorField,
                foreignKeyTable,
                foreignKey,
                foreignKeyTableAccessorChainTokens,
                foreignKeyCell,
                header,
                openl,
                constructorField,
                fieldChainTokens);
        } else {
            currentColumnDescriptor = new ColumnDescriptor(descriptorField,
                header,
                openl,
                constructorField,
                fieldChainTokens);
        }
        return currentColumnDescriptor;
    }

    /**
     * Process the chain of fields, e.g. driver.homeAdress.street;
     *
     * @return {@link IOpenField} for fields chain.
     */
    private static IOpenField processFieldsChain(ITable table, IOpenClass type, 
            IdentifierNode[] fieldAccessorChainTokens) {
        IOpenField chainField = null;
        IOpenClass loadedFieldType = type;

        // the chain of fields to access the target field, e.g. for
        // driver.name it will be array consisting of two fields:
        // 1st for driver, 2nd for name     
        IOpenField[] fieldAccessorChain = new IOpenField[fieldAccessorChainTokens.length];
        boolean hasAccessByArrayId = false;

        for (int fieldIndex = 0; fieldIndex < fieldAccessorChain.length; fieldIndex++) {
            IdentifierNode fieldNameNode = fieldAccessorChainTokens[fieldIndex];
            IOpenField fieldInChain;
            boolean arrayAccess = fieldNameNode.getIdentifier().matches(ARRAY_ACCESS_PATTERN);

            if(fieldNameNode.getIdentifier().matches(PRECISION_PATTERN)) {
                fieldAccessorChain = ArrayUtils.remove(fieldAccessorChain, fieldIndex);
                fieldAccessorChainTokens = ArrayUtils.remove(fieldAccessorChainTokens, fieldIndex);
                //Skip creation of IOpenField
                continue;
            }
            
            if (arrayAccess) {
                hasAccessByArrayId = arrayAccess;
                fieldInChain = getWritableArrayElement(fieldNameNode, table, loadedFieldType);
            } else {
                fieldInChain = getWritableField(fieldNameNode, table, loadedFieldType);
            }
            
            if (fieldIndex > 0 && (fieldAccessorChain[fieldIndex - 1] instanceof DatatypeArrayElementField || fieldAccessorChain[fieldIndex - 1] instanceof SpreadsheetResultField) && fieldAccessorChain[fieldIndex - 1].getType().getOpenClass().equals(JavaOpenClass.OBJECT)){
                if (fieldNameNode.getIdentifier().matches(SPREADSHEETRESULTFIELD_PATTERN)){
                    AOpenField aOpenField = (AOpenField) fieldAccessorChain[fieldIndex - 1];
                    aOpenField.setType(new SpreadsheetResultOpenClass(SpreadsheetResult.class));
                }
            }
            
            if (fieldInChain == null) {
                // in this case current field and all the followings in fieldAccessorChain will be nulls.
                //
                break;
            }
            
            if (fieldInChain.getType() != null && fieldInChain.getType().isArray() && arrayAccess) {
                loadedFieldType = fieldInChain.getType().getComponentClass();
            } else {
                loadedFieldType = fieldInChain.getType();
            }
            
            fieldAccessorChain[fieldIndex] = fieldInChain;
        }
        if (!CollectionUtils.hasNull(fieldAccessorChain)) { // check successful loading of all
                                                                // fields in fieldAccessorChain.
            chainField = new FieldChain(type, fieldAccessorChain, fieldAccessorChainTokens, hasAccessByArrayId);
        }
        return chainField;
    }

    public static Integer getPrecisionValue(IdentifierNode fieldNameNode) {
        try {
            String fieldName = fieldNameNode.getIdentifier();
            String txtIndex = fieldName.substring(fieldName.indexOf("(") + 1, fieldName.indexOf(")"));

            return Integer.parseInt(txtIndex);
        } catch (Exception e) {
            return null;
        }
    }

    private static int getArrayIndex(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        String txtIndex = fieldName.substring(fieldName.indexOf("[") + 1, fieldName.indexOf("]"));

        return Integer.parseInt(txtIndex);
    }

    private static String getArrayName(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        return fieldName.substring(0, fieldName.indexOf("["));
    }

    private static void processError(ITable table, SyntaxNodeException error) {
        if (table != null) {
            if (table.getTableSyntaxNode() != null) {
                table.getTableSyntaxNode().addError(error);
            }
            BindHelper.processError(error);
        }
    }

    /**
     * Returns foreign_key_tokens from the current column.
     * 
     * @param bindingContext is used for optimization
     *            {@link GridCellSourceCodeModule} in execution mode. Can be
     *            <code>null</code>.
     * @param descriptorRows
     * @param columnNum
     * @return
     * @throws OpenLCompilationException
     * 
     * @see {@link #hasForeignKeysRow(ILogicalTable)}.
     */
    private static IdentifierNode[] getForeignKeyTokens(IBindingContext bindingContext,
            ILogicalTable descriptorRows,
            int columnNum) throws OpenLCompilationException {

        ILogicalTable logicalRegion = descriptorRows.getSubtable(columnNum, 1, 1, 1);
        GridCellSourceCodeModule indexRowSourceModule = new GridCellSourceCodeModule(logicalRegion.getSource(),
            bindingContext);

        // Should be in format
        // "> reference_table_name [reference_table_key_column]"
        return Tokenizer.tokenize(indexRowSourceModule, INDEX_ROW_REFERENCE_DELIMITER);
    }

    /**
     * Gets the field, and if it is not <code>null</code> and isWritable,
     * returns it. In other case processes errors and return <code>null</code>.
     * 
     * @param currentFieldNameNode
     * @param table
     * @param loadedFieldType
     * @return
     */
    private static IOpenField getWritableField(IdentifierNode currentFieldNameNode,
            ITable table,
            IOpenClass loadedFieldType) {

        String fieldName = currentFieldNameNode.getIdentifier();
        IOpenField field = DataTableBindHelper.findField(fieldName, table, loadedFieldType);
        // Try use object type as SpreadsheetResult
        if (field == null && loadedFieldType.equals(JavaOpenClass.OBJECT)) {
            field = DataTableBindHelper.findField(fieldName,
                table,
                new org.openl.rules.calc.SpreadsheetResultOpenClass(org.openl.rules.calc.SpreadsheetResult.class));
        }
        if (field == null) {
            String errorMessage = String.format("Field \"%s\" is not found in %s", fieldName, loadedFieldType.getName());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(errorMessage, currentFieldNameNode);
            processError(table, error);
            return null;
        }

        if (!field.isWritable()) {
            String message = String.format("Field '%s' is not writable in %s", fieldName, loadedFieldType.getName());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(table, error);
            return null;
        }

        return field;
    }

    private static IOpenField getWritableArrayElement(IdentifierNode currentFieldNameNode,
            ITable table,
            IOpenClass loadedFieldType) {
        String arrayName = getArrayName(currentFieldNameNode);
        int arrayIndex = getArrayIndex(currentFieldNameNode);
        IOpenField field = DataTableBindHelper.findField(arrayName, table, loadedFieldType);
        //Try find field in SpreadsheetResult type
        if (field == null && loadedFieldType.equals(JavaOpenClass.OBJECT)) {
            field = DataTableBindHelper.findField(arrayName,
                table,
                new org.openl.rules.calc.SpreadsheetResultOpenClass(org.openl.rules.calc.SpreadsheetResult.class));
        }
        
        if (field == null){
            String message = String.format("Field '%s' is not found!", arrayName);
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(table, error);
            return null;
        }
        
        if (!field.getType().isArray() && !field.getType().getOpenClass().getInstanceClass().equals(Object.class)) {
            String message = String.format("Field '%s' isn't array! The field type is '%s'", arrayName, field.getType()
                .toString());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(table, error);
            return null;
        }
        IOpenField arrayAccessField = null;
        if (!field.getType().isArray() && field.getType().getOpenClass().getInstanceClass().equals(Object.class)) {
            arrayAccessField = new DatatypeArrayElementField(field, arrayIndex, JavaOpenClass.OBJECT);
        } else {
            arrayAccessField = new DatatypeArrayElementField(field, arrayIndex);
        }
        if (!arrayAccessField.isWritable()) {
            String message = String.format("Field '%s' is not writable in %s", arrayName, loadedFieldType.getName());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(table, error);
            return null;
        }

        return arrayAccessField;
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
