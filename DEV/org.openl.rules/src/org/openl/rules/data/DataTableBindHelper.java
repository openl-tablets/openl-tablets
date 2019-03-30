package org.openl.rules.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultField;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.rules.testmethod.TestMethodOpenClass;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.CollectionElementField;
import org.openl.types.impl.CollectionType;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

public class DataTableBindHelper {

    private static final char INDEX_ROW_REFERENCE_START_SYMBOL = '>';

    private static final String FPK = "_PK_";

    /**
     * Indicates that field is a constructor.<br>
     */
    // Protected to make javadoc reference.
    static final String CONSTRUCTOR_FIELD = "this";

    private static final String CODE_DELIMETERS = ".\n\r";
    private static final String INDEX_ROW_REFERENCE_DELIMITER = " >\n\r";
    private static final String LINK_DELIMETERS = ".";

    // patter for field like addressArry[0]
    public static final Pattern COLLECTION_ACCESS_BY_INDEX_PATTERN = Pattern
        .compile("\\s*[^\\:\\s]+\\s*\\[\\s*[0-9]+\\s*\\]\\s*(\\:\\s*[^\\:\\s]+|)\\s*$");
    public static final Pattern COLLECTION_ACCESS_BY_KEY_PATTERN = Pattern
        .compile("\\s*[^\\:\\s]+\\s*\\[\\s*(\\\".*\\\"|[0-9]+)\\s*\\]\\s*(\\:\\s*[^\\:\\s]+|)\\s*$");

    static final Pattern THIS_ARRAY_ACCESS_PATTERN = Pattern.compile("\\s*\\[\\s*[0-9]+\\s*\\]\\s*$");
    static final Pattern THIS_LIST_ACCESS_PATTERN = Pattern.compile("\\s*\\[\\s*[0-9]+\\s*\\]\\s*(\\:\\s*[^\\:]+|)$");
    static final Pattern THIS_MAP_ACCESS_PATTERN = Pattern
        .compile("\\s*\\[\\s*(\\\".*\\\"|[0-9]+)\\s*\\]\\s*(\\:\\s*[^\\:\\s]+|)\\s*$");
    public static final Pattern PRECISION_PATTERN = Pattern.compile("^\\(\\-?[0-9]+\\)$");
    public static final Pattern SPREADSHEETRESULTFIELD_PATTERN = Pattern.compile("^\\$.+\\$.+$");
    private static final Pattern FIELD_WITH_PRECISION_PATTERN = Pattern.compile("^(.*\\S)\\s*(\\(-?[0-9]+\\))$");
    private static final Pattern QUOTED = Pattern.compile("\\\".*\\\"");

    /**
     * Foreign keys row is optional for data table. It consists reference for field value to other table. Foreign keys
     * always starts from {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
     *
     * @param dataTable data table to check
     * @return <code>TRUE</code> if second row in data table body (next to the field row) consists even one value, in
     *         any column, starts with {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
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
     * Gets the table body, by skipping the table header and properties sections.
     *
     * @param tsn inspecting table
     * @return Table body without table header and properties section.
     */
    public static ILogicalTable getTableBody(TableSyntaxNode tsn) {

        int startRow;

        if (!tsn.hasPropertiesDefinedInTable()) {
            startRow = 1;
        } else {
            startRow = 2;
        }

        return tsn.getTable().getRows(startRow);
    }

    /**
     * Checks if table representation is horizontal. Horizontal is data table where parameters are listed from left to
     * right.</br>
     * Example:
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
     * @param dataTableBody the body of a table to check
     * @param tableType the type of data table
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
     * Goes through the data table columns from left to right, and count number of changeable
     * <code>{@link IOpenField}</code>.
     *
     * @param dataTable the body of a table to check
     * @param tableType the type of data table
     * @return Number of <code>{@link IOpenField}</code> found in the data table.
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
            int dotIndex = fieldName.indexOf('.');
            if (dotIndex > 0) {
                fieldName = fieldName.substring(0, dotIndex);
            }
            // if it is array field correct field name
            int brIndex = fieldName.indexOf('[');
            if (brIndex > 0) {
                fieldName = fieldName.substring(0, brIndex);
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
     * Gets the horizontal table representation from current table. If it was vertical it will be transposed.
     *
     * @param tableBody the body of a table to check
     * @param tableType the type of data table
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
     * Gets the Data_With_Titles rows from the data table body. Data_With_Titles start row consider to be the next row
     * after descriptor section of the table and till the end of the table.
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
     * @param tableType the type of a table
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
     * Gets the Data_With_Titles columns from the data table body. Data_With_Titles start column consider to be the next
     * column after descriptor section of the table and till the end of the table.
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
     * Gets the start index of the Data_With_Titles section of the data table body.<br>
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
     * Gets the descriptor rows from the data table body. Descriptor rows are obligatory parameter row and optional
     * foreign key row if it exists in the table.
     *
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Descriptor rows for current data table body.
     */
    public static ILogicalTable getDescriptorRows(ILogicalTable horizDataTableBody) {

        int endRow = getEndRowForDescriptorSection(horizDataTableBody);

        return horizDataTableBody.getRows(0, endRow);
    }

    /**
     * Gets the number of end row for descriptor section of the data table body. It depends on whether table has or no
     * the foreign key row.
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
     * @param dataWithTitleRows Logical part of the data table. Consider to include all rows from base table after
     *            header section (consists from header row + property section) and descriptor section (consists from
     *            JavaBean name obligatory + optional index row, see {@link #hasForeignKeysRow(ILogicalTable)}).<br>
     *            This part of table may consists from optional first title row and followed data rows.
     * @param bindingContext is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
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

            return new StringValue(value,
                value,
                value,
                new GridCellSourceCodeModule(titleCell.getSource(), bindingContext));
        }

        return new StringValue(value, value, value, null);
    }

    /**
     *
     * @param bindingContext is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
     *            <code>null</code>.
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
                    IdentifierNode fieldNameNode = fieldAccessorChainTokens[0];
                    if (supportConstructorFields && CONSTRUCTOR_FIELD.equals(fieldNameNode.getIdentifier())) {
                        constructorField = true;
                    }
                }
                if (!constructorField && (!(fieldAccessorChainTokens.length == 1 && hasForeignKeysRow && CONSTRUCTOR_FIELD
                    .equals(fieldAccessorChainTokens[0].getIdentifier())))) {
                    descriptorField = processFieldsChain(bindingContext, table, type, fieldAccessorChainTokens);
                }

                if (hasForeignKeysRow) {
                    IdentifierNode[] foreignKeyTokens = getForeignKeyTokens(bindingContext, descriptorRows, columnNum);
                    foreignKeyTable = foreignKeyTokens.length > 0 ? foreignKeyTokens[0] : null;
                    foreignKey = foreignKeyTokens.length > 1 ? foreignKeyTokens[1] : null;
                    foreignKeyCell = descriptorRows.getSubtable(columnNum, 1, 1, 1).getSource().getCell(0, 0);

                    if (foreignKeyTable != null) {
                        accessorChainTokens = Tokenizer
                            .tokenize(foreignKeyTable.getModule(), LINK_DELIMETERS, foreignKeyTable.getLocation());

                        if (!ArrayUtils.isEmpty(accessorChainTokens)) {
                            foreignKeyTable = accessorChainTokens.length > 0 ? accessorChainTokens[0] : null;
                        }
                    }
                }

                StringValue header = DataTableBindHelper
                    .makeColumnTitle(bindingContext, dataWithTitleRows, columnNum, hasColumnTytleRow);

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

        boolean hasSupportMultirowsAfter = false;

        for (int columnNum = columnIdentifiers.size() - 1; columnNum >= 0; columnNum--) {
            if (columnDescriptors[columnNum] != null) {
                if (hasSupportMultirowsAfter) {
                    columnDescriptors[columnNum].setSupportMultirows(true);
                } else {
                    if (columnDescriptors[columnNum].isSupportMultirows()) {
                        hasSupportMultirowsAfter = true;
                    }
                }
            }
        }

        return columnDescriptors;
    }

    /**
     *
     * @param bindingContext is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
     *            <code>null</code>.
     * @param table is needed only for error processing. Can be <code>null</code>.
     */
    public static List<IdentifierNode[]> getColumnIdentifiers(IBindingContext bindingContext,
            ITable table,
            ILogicalTable descriptorRows) {
        int width = descriptorRows.getWidth();
        List<IdentifierNode[]> identifiers = new ArrayList<>();
        for (int columnNum = 0; columnNum < width; columnNum++) {

            GridCellSourceCodeModule cellSourceModule = getCellSourceModule(descriptorRows, columnNum);
            cellSourceModule.update(bindingContext);

            String code = cellSourceModule.getCode();

            if (code.length() != 0) {

                IdentifierNode[] fieldAccessorChainTokens = null;
                try {
                    // fields names nodes
                    fieldAccessorChainTokens = trimAndSplitPrecisionToken(
                        Tokenizer.tokenize(cellSourceModule, CODE_DELIMETERS));
                } catch (OpenLCompilationException e) {
                    String message = String.format("Cannot parse field source \"%s\"", code);
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                    processError(bindingContext, table, error);
                }

                if (contains(identifiers, fieldAccessorChainTokens)) {
                    String message = String.format("Found duplicate of field \"%s\"", code);
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                    processError(bindingContext, table, error);
                } else {
                    identifiers.add(fieldAccessorChainTokens);
                }
            } else {
                identifiers.add(null);
            }
        }
        return identifiers;
    }

    private static IdentifierNode[] trimAndSplitPrecisionToken(IdentifierNode[] chainTokens) {
        if (chainTokens.length == 0) {
            return chainTokens;
        }

        // Trim all identifiers and set correct location for them.
        for (int i = 0; i < chainTokens.length; i++) {
            IdentifierNode token = chainTokens[i];
            String identifier = token.getIdentifier();
            String trimmed = identifier.trim();
            if (trimmed.length() != identifier.length()) {
                int tokenStart = token.getLocation().getStart().getAbsolutePosition(null) + identifier.indexOf(trimmed);

                TextInterval fieldInterval = LocationUtils.createTextInterval(tokenStart,
                    tokenStart + trimmed.length());
                chainTokens[i] = new IdentifierNode(token.getType(), fieldInterval, trimmed, token.getModule());
            }
        }

        // Extract precision node if exists in last identifier chain
        IdentifierNode token = chainTokens[chainTokens.length - 1];
        String identifier = token.getIdentifier();

        Matcher matcher = FIELD_WITH_PRECISION_PATTERN.matcher(identifier);
        if (matcher.matches()) {
            // Separate the token to: 1) field 2) precision
            String field = matcher.group(1);
            String precision = matcher.group(2);

            int tokenStart = token.getLocation().getStart().getAbsolutePosition(null);
            int fieldStart = identifier.indexOf(field);
            int precisionStart = identifier.lastIndexOf(precision);
            TextInterval fieldInterval = LocationUtils.createTextInterval(tokenStart + fieldStart,
                tokenStart + fieldStart + field.length());
            TextInterval precisionInterval = LocationUtils.createTextInterval(tokenStart + precisionStart,
                tokenStart + precisionStart + precision.length());

            chainTokens[chainTokens.length - 1] = new IdentifierNode(token.getType(),
                fieldInterval,
                field,
                token.getModule());

            chainTokens = ArrayUtils.add(chainTokens,
                new IdentifierNode(token.getType(), precisionInterval, precision, token.getModule()));
        }

        return chainTokens;
    }

    private static GridCellSourceCodeModule getCellSourceModule(ILogicalTable descriptorRows, int columnNum) {
        IGridTable gridTable = descriptorRows.getColumn(columnNum).getSource();
        return new GridCellSourceCodeModule(gridTable);
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

    private static IOpenClass getTypeForCollection(IBindingContext bindingContext,
            ITable table,
            IdentifierNode identifierNode) {
        int typeSeparatorIndex = identifierNode.getIdentifier().indexOf(':');
        if (typeSeparatorIndex < 0) {
            return JavaOpenClass.OBJECT;
        }

        String typeName = identifierNode.getIdentifier().substring(typeSeparatorIndex + 1);
        typeName = typeName.trim();

        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
        if (type == null) {
            String message = String.format("Can't bind node: '%s'. Can't find type: '%s'.", identifierNode, typeName);
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, identifierNode);
            processError(bindingContext, table, error);
        }
        return type;
    }

    /**
     * Process the chain of fields, e.g. driver.homeAdress.street;
     *
     * @return {@link IOpenField} for fields chain.
     */
    public static IOpenField processFieldsChain(IBindingContext bindingContext,
            ITable table,
            IOpenClass type,
            IdentifierNode[] fieldAccessorChainTokens) {
        IOpenField chainField = null;
        IOpenClass loadedFieldType = type;

        // the chain of fields to access the target field, e.g. for
        // driver.name it will be array consisting of two fields:
        // 1st for driver, 2nd for name
        IOpenField[] fieldAccessorChain = new IOpenField[fieldAccessorChainTokens.length];
        boolean hasAccessByArrayId = false;
        StringBuilder partPathFromRoot = new StringBuilder();

        boolean multiRowsArentSupported = type instanceof TestMethodOpenClass && fieldAccessorChainTokens[0]
            .getIdentifier()
            .trim()
            .startsWith(TestMethodHelper.EXPECTED_RESULT_NAME);

        for (int fieldIndex = 0; fieldIndex < fieldAccessorChain.length; fieldIndex++) {
            IdentifierNode fieldNameNode = fieldAccessorChainTokens[fieldIndex];
            String identifier = fieldNameNode.getIdentifier();

            IOpenField fieldInChain;
            if (fieldIndex > 0 && (fieldIndex == fieldAccessorChain.length - 1) && identifier
                .equals(FPK) && (fieldAccessorChain[fieldIndex - 1] instanceof CollectionElementWithMultiRowField)) { // Multi-rows
                // support.
                // PK
                // for
                // arrays.
                CollectionElementWithMultiRowField datatypeCollectionMultiRowElementField = (CollectionElementWithMultiRowField) fieldAccessorChain[fieldIndex - 1];
                CollectionElementWithMultiRowField newDatatypeArrayMultiRowElementField = new CollectionElementWithMultiRowField(
                    datatypeCollectionMultiRowElementField.getField(),
                    datatypeCollectionMultiRowElementField.getFieldPathFromRoot(),
                    JavaOpenClass.STRING,
                    datatypeCollectionMultiRowElementField.getCollectionType(),
                    true);
                IOpenField[] fieldAccessorChainTmp = new IOpenField[fieldAccessorChainTokens.length - 1];
                System.arraycopy(fieldAccessorChain, 0, fieldAccessorChainTmp, 0, fieldAccessorChainTokens.length - 1);
                fieldAccessorChain = fieldAccessorChainTmp;
                fieldAccessorChain[fieldAccessorChain.length - 1] = newDatatypeArrayMultiRowElementField;
                continue;
            }

            if (fieldIndex == 0 && StringUtils.matches(THIS_ARRAY_ACCESS_PATTERN,
                identifier) && (!(type instanceof TestMethodOpenClass)) && type.isArray()) {
                fieldAccessorChain[fieldIndex] = new ThisCollectionElementField(getCollectionIndex(fieldNameNode),
                    type.getComponentClass(),
                    CollectionType.ARRAY);
                loadedFieldType = type.getComponentClass();
                continue;
            }

            if (fieldIndex == 0 && StringUtils.matches(THIS_LIST_ACCESS_PATTERN,
                identifier) && (!(type instanceof TestMethodOpenClass)) && List.class
                    .isAssignableFrom(type.getInstanceClass())) {
                IOpenClass elementType = getTypeForCollection(bindingContext, table, fieldNameNode);
                fieldAccessorChain[fieldIndex] = new ThisCollectionElementField(getCollectionIndex(fieldNameNode),
                    elementType,
                    CollectionType.LIST);
                loadedFieldType = elementType;
                continue;
            }

            if (fieldIndex == 0 && StringUtils.matches(THIS_MAP_ACCESS_PATTERN,
                identifier) && (!(type instanceof TestMethodOpenClass)) && Map.class
                    .isAssignableFrom(type.getInstanceClass())) {
                IOpenClass elementType = getTypeForCollection(bindingContext, table, fieldNameNode);
                fieldAccessorChain[fieldIndex] = new ThisCollectionElementField(getCollectionKey(fieldNameNode),
                    elementType);
                loadedFieldType = elementType;
                continue;
            }

            if (StringUtils.matches(PRECISION_PATTERN, identifier)) {
                fieldAccessorChain = ArrayUtils.remove(fieldAccessorChain, fieldIndex);
                fieldAccessorChainTokens = ArrayUtils.remove(fieldAccessorChainTokens, fieldIndex);
                // Skip creation of IOpenField
                continue;
            }

            boolean collectionAccessPattern = StringUtils.matches(COLLECTION_ACCESS_BY_INDEX_PATTERN,
                identifier) || StringUtils.matches(COLLECTION_ACCESS_BY_KEY_PATTERN, identifier);

            if (collectionAccessPattern) {
                hasAccessByArrayId = true;
                fieldInChain = getWritableCollectionElement(bindingContext,
                    fieldNameNode,
                    table,
                    loadedFieldType,
                    partPathFromRoot.toString(),
                    false);
            } else {
                fieldInChain = getWritableField(bindingContext, fieldNameNode, table, loadedFieldType);

                if ((fieldIndex != fieldAccessorChain.length - 1) && fieldInChain != null && (fieldInChain.getType()
                    .isArray() || List.class.isAssignableFrom(fieldInChain.getType().getInstanceClass()))) {
                    fieldInChain = getWritableCollectionElement(bindingContext,
                        fieldNameNode,
                        table,
                        loadedFieldType,
                        partPathFromRoot.toString(),
                        !multiRowsArentSupported);
                }
            }

            if (fieldIndex > 0 && (fieldAccessorChain[fieldIndex - 1] instanceof CollectionElementField || fieldAccessorChain[fieldIndex - 1] instanceof SpreadsheetResultField) && fieldAccessorChain[fieldIndex - 1]
                .getType()
                .equals(JavaOpenClass.OBJECT)) {
                if (StringUtils.matches(SPREADSHEETRESULTFIELD_PATTERN, identifier)) {
                    AOpenField aOpenField = (AOpenField) fieldAccessorChain[fieldIndex - 1];
                    aOpenField.setType(JavaOpenClass.getOpenClass(SpreadsheetResult.class));
                }
            }

            if (fieldInChain == null) {
                // in this case current field and all the followings in
                // fieldAccessorChain will be nulls.
                //
                break;
            }

            loadedFieldType = fieldInChain.getType();

            fieldAccessorChain[fieldIndex] = fieldInChain;
            if (fieldIndex > 0) {
                partPathFromRoot.append('.');
            }
            partPathFromRoot.append(fieldInChain.getName());
        }
        if (!CollectionUtils.hasNull(fieldAccessorChain)) { // check successful
                                                            // loading of all
                                                            // fields in
                                                            // fieldAccessorChain.
            chainField = new FieldChain(type, fieldAccessorChain, fieldAccessorChainTokens, hasAccessByArrayId);
        }
        return chainField;
    }

    public static Integer getPrecisionValue(IdentifierNode fieldNameNode) {
        try {
            String fieldName = fieldNameNode.getIdentifier();
            String txtIndex = fieldName.substring(fieldName.indexOf('(') + 1, fieldName.indexOf(')'));

            return Integer.parseInt(txtIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getCollectionIndex(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        String txtIndex = fieldName.substring(fieldName.indexOf('[') + 1, fieldName.indexOf(']')).trim();
        return Integer.parseInt(txtIndex);
    }

    public static String getCollectionName(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        int ind = fieldName.indexOf('[');
        if (ind > 0) {
            return fieldName.substring(0, ind).trim();
        }

        return getFieldName(fieldName);
    }

    private static void processError(IBindingContext bindingContext, ITable table, SyntaxNodeException error) {
        if (table != null && table.getTableSyntaxNode() != null) {
            table.getTableSyntaxNode().addError(error);
        }
        bindingContext.addError(error);
    }

    /**
     * Returns foreign_key_tokens from the current column.
     * 
     * @param bindingContext is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
     *            <code>null</code>.
     * @see #hasForeignKeysRow(ILogicalTable)
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
     * Gets the field, and if it is not <code>null</code> and isWritable, returns it. In other case processes errors and
     * return <code>null</code>.
     */
    private static IOpenField getWritableField(IBindingContext bindingContext,
            IdentifierNode currentFieldNameNode,
            ITable table,
            IOpenClass loadedFieldType) {
        String fieldName = getFieldName(currentFieldNameNode.getIdentifier());

        IOpenField field = DataTableBindHelper.findField(fieldName, table, loadedFieldType);
        // Try use object type as SpreadsheetResult
        if (field == null && loadedFieldType.equals(JavaOpenClass.OBJECT)) {
            field = DataTableBindHelper
                .findField(fieldName, table, JavaOpenClass.getOpenClass(org.openl.rules.calc.SpreadsheetResult.class));
        }
        if (field == null) {
            String errorMessage;
            if (loadedFieldType instanceof TestMethodOpenClass) {
                StringBuilder sb = new StringBuilder();
                MethodUtil.printMethod(((TestMethodOpenClass) loadedFieldType).getTestedMethod(), sb);
                errorMessage = String.format("Found \"%s\", but expected one of the parameters from the method \"%s\".",
                    fieldName,
                    sb.toString());
            } else {
                errorMessage = String.format("Field \"%s\" is not found in %s", fieldName, loadedFieldType.getName());
            }
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(errorMessage, currentFieldNameNode);
            processError(bindingContext, table, error);
            return null;
        }

        if (!field.isWritable()) {
            String message = String.format("Field '%s' is not writable in %s", fieldName, loadedFieldType.getName());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(bindingContext, table, error);
            return null;
        }

        return field;
    }

    private static String getFieldName(String identifier) {
        String fieldName = identifier.trim();
        int endIndex = fieldName.indexOf(':');
        if (endIndex > 0) {
            fieldName = fieldName.substring(0, endIndex).trim();
        }
        return fieldName;
    }

    private static String buildRootPathForDatatypeArrayMultiRowElementField(String partPathFromRoot, String fieldName) {
        if (StringUtils.isEmpty(partPathFromRoot)) {
            return fieldName + "[]";
        } else {
            return partPathFromRoot + "." + fieldName + "[]";
        }
    }

    private static IOpenField getWritableCollectionElement(IBindingContext bindingContext,
            IdentifierNode currentFieldNameNode,
            ITable table,
            IOpenClass loadedFieldType,
            String partPathFromRoot,
            boolean multiRowElement) {
        String name = getCollectionName(currentFieldNameNode);
        IOpenField field = DataTableBindHelper.findField(name, table, loadedFieldType);
        // Try find field in SpreadsheetResult type
        if (field == null && loadedFieldType.equals(JavaOpenClass.OBJECT)) {
            field = DataTableBindHelper
                .findField(name, table, JavaOpenClass.getOpenClass(org.openl.rules.calc.SpreadsheetResult.class));
        }

        if (field == null) {
            String message = String.format("Field '%s' is not found!", name);
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(bindingContext, table, error);
            return null;
        }

        if (!Map.class.isAssignableFrom(field.getType().getInstanceClass()) && !List.class
            .isAssignableFrom(field.getType().getInstanceClass()) && !field.getType()
                .isArray() && !field.getType().getInstanceClass().equals(Object.class)) {
            String message = String
                .format("Field '%s' isn't a collection! The field type is '%s'", name, field.getType().toString());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(bindingContext, table, error);
            return null;
        }

        IOpenField collectionAccessField;
        if (multiRowElement) {
            if (List.class.isAssignableFrom(field.getType().getInstanceClass())) {
                IOpenClass elementType = getTypeForCollection(bindingContext, table, currentFieldNameNode);
                collectionAccessField = new CollectionElementWithMultiRowField(field,
                    buildRootPathForDatatypeArrayMultiRowElementField(partPathFromRoot, field.getName()),
                    elementType,
                    CollectionType.LIST);
            } else {
                if (!field.getType().isArray() && field.getType().getInstanceClass().equals(Object.class)) {
                    collectionAccessField = new CollectionElementWithMultiRowField(field,
                        buildRootPathForDatatypeArrayMultiRowElementField(partPathFromRoot, field.getName()),
                        JavaOpenClass.OBJECT,
                        CollectionType.ARRAY);
                } else {
                    collectionAccessField = new CollectionElementWithMultiRowField(field,
                        buildRootPathForDatatypeArrayMultiRowElementField(partPathFromRoot, field.getName()),
                        field.getType().getComponentClass(),
                        CollectionType.ARRAY);
                }
            }
        } else {
            if (Map.class.isAssignableFrom(field.getType().getInstanceClass())) {
                Object mapKey;
                try {
                    mapKey = getCollectionKey(currentFieldNameNode);
                } catch (Exception e) {
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Failed to parse map key.",
                        currentFieldNameNode);
                    processError(bindingContext, table, error);
                    return null;
                }
                IOpenClass elementType = getTypeForCollection(bindingContext, table, currentFieldNameNode);
                collectionAccessField = new CollectionElementField(field, mapKey, elementType);
            } else {
                int index;
                try {
                    index = getCollectionIndex(currentFieldNameNode);
                } catch (Exception e) {
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Failed to parse array index.",
                        currentFieldNameNode);
                    processError(bindingContext, table, error);
                    return null;
                }
                if (List.class.isAssignableFrom(field.getType().getInstanceClass())) {
                    IOpenClass elementType = getTypeForCollection(bindingContext, table, currentFieldNameNode);
                    collectionAccessField = new CollectionElementField(field, index, elementType, CollectionType.LIST);
                } else {
                    if (!field.getType().isArray() && field.getType().getInstanceClass().equals(Object.class)) {
                        collectionAccessField = new CollectionElementField(field,
                            index,
                            JavaOpenClass.OBJECT,
                            CollectionType.ARRAY);
                    } else {
                        collectionAccessField = new CollectionElementField(field,
                            index,
                            field.getType().getComponentClass(),
                            CollectionType.ARRAY);
                    }
                }
            }
        }
        if (!collectionAccessField.isWritable()) {
            String message = String.format("Field '%s' is not writable in %s", name, loadedFieldType.getName());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
            processError(bindingContext, table, error);
            return null;
        }

        return collectionAccessField;
    }

    public static Object getCollectionKey(IdentifierNode currentFieldNameNode) {
        String s = currentFieldNameNode.getIdentifier();
        s = s.substring(s.indexOf('[') + 1, s.lastIndexOf(']')).trim();
        if (StringUtils.matches(QUOTED, s)) {
            return s.substring(1, s.length() - 1);
        } else {
            return Integer.valueOf(s);
        }
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
