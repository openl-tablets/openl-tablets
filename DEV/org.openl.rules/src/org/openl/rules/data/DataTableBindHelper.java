package org.openl.rules.data;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultField;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.rules.testmethod.TestMethodOpenClass;
import org.openl.rules.testmethod.UserErrorOpenClass;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.CollectionElementField;
import org.openl.types.impl.CollectionType;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

@Slf4j
public class DataTableBindHelper {

    private static final String ERROR_OCCURRED = "Error occurred: ";

    private DataTableBindHelper() {
    }


    private static final char INDEX_ROW_REFERENCE_START_SYMBOL = '>';

    /** The field that names the rows of a Data table, when the table declares one: its primary key. */
    public static final String FPK = "_PK_";

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
            .compile("\\s*[^\\:\\s\\[\\]]+\\s*\\[\\s*\\d+\\s*\\]\\s*+(\\:\\s*[^\\:\\s]+|)\\s*$");
    public static final Pattern COLLECTION_ACCESS_BY_KEY_PATTERN = Pattern
            .compile("\\s*[^\\:\\s\\[\\]]+\\s*\\[\\s*(\\\".*\\\"|\\d+)\\s*\\]\\s*+(\\:\\s*[^\\:\\s]+|)\\s*$");

    static final Pattern THIS_ARRAY_ACCESS_PATTERN = Pattern.compile("\\s*\\[\\s*\\d+\\s*\\]\\s*$");
    static final Pattern THIS_LIST_ACCESS_PATTERN = Pattern.compile("\\s*\\[\\s*\\d+\\s*\\]\\s*(\\:[^\\:]+|)$");
    static final Pattern THIS_MAP_ACCESS_PATTERN = Pattern
            .compile("\\s*\\[\\s*(\\\".*\\\"|\\d+)\\s*\\]\\s*+(\\:\\s*[^\\:\\s]+|)\\s*$");
    public static final Pattern PRECISION_PATTERN = Pattern.compile("^\\(\\-?\\d+\\)$");
    public static final Pattern SPREADSHEETRESULT_FIELD_PATTERN = Pattern.compile("^\\$.+$");
    private static final Pattern FIELD_WITH_PRECISION_PATTERN = Pattern.compile("^(.*\\S)\\s*(\\(-?\\d+\\))$");
    private static final Pattern QUOTED = Pattern.compile("\\\".*\\\"");

    /**
     * Foreign keys row is optional for data table. It consists reference for field value to other table. Foreign keys
     * always starts from {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
     *
     * @param dataTable data table to check
     * @return <code>TRUE</code> if second row in data table body (next to the field row) consists even one value, in
     * any column, starts with {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
     */
    public static boolean hasForeignKeysRow(ILogicalTable dataTable) {

        var potentialForeignKeysRow = dataTable.getRows(1, 1);

        var columnsCount = potentialForeignKeysRow.getWidth();

        for (var i = 0; i < columnsCount; i++) {

            var cell = potentialForeignKeysRow.getColumn(i);
            var value = cell.getSource().getCell(0, 0).getStringValue();

            if (value == null || value.trim().isEmpty()) {
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
     * @param tableType     the type of data table
     * @return <code>TRUE</code> if table is horizontal.
     */
    public static boolean isHorizontalTable(ILogicalTable dataTableBody, IOpenClass tableType) {

        // If data table body contains only one row, we consider it is vertical.
        //
        if (dataTableBody.getHeight() != 1) {
            if (ClassUtils.isAssignable(tableType.getInstanceClass(), TableProperties.class)) {
                // Properties are always vertical
                return false;
            }
            var fieldsCount1 = countChangeableFields(dataTableBody, tableType);
            var dataTableBodyT = dataTableBody.transpose();
            var fieldsCount2 = countChangeableFields(dataTableBodyT, tableType);

            if (fieldsCount1 > fieldsCount2) {
                return true;
            } else if (fieldsCount1 < fieldsCount2) {
                return false;
            } else {
                return isHorizontalByRefs(dataTableBody, dataTableBodyT, tableType);
            }
        }
        return false;
    }

    /**
     * Compares a table with the same number of fields in both directions by its references, and then by the result
     * fields of a test table.
     */
    private static boolean isHorizontalByRefs(ILogicalTable dataTableBody,
                                              ILogicalTable dataTableBodyT,
                                              IOpenClass tableType) {
        var refCount1 = countRefs(dataTableBody);
        var refCount2 = countRefs(dataTableBodyT);
        if (refCount1 < refCount2) {
            return true;
        } else if (refCount1 > refCount2) {
            return false;
        } else {
            if (tableType instanceof TestMethodOpenClass) {
                var resCount1 = countResFields(dataTableBody);
                var resCount2 = countResFields(dataTableBodyT);
                return resCount1 >= resCount2;
            }
            return true;
        }
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

        var count = 0;
        var width = dataTable.getWidth();
        var uniqueFieldNames = new HashSet<String>();

        for (var i = 0; i < width; ++i) {

            var fieldName = dataTable.getColumn(i).getSource().getCell(0, 0).getStringValue();

            // Remove extra spaces.
            //
            fieldName = StringUtils.trim(fieldName);
            if (fieldName == null || !uniqueFieldNames.add(fieldName)) {
                continue; // don't count empty cells and duplicates
            }
            if (isChangeableField(fieldName, tableType)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Checks whether a column title refers to a field of the table type that is writable and is not a constant. The
     * numeric indexes the title starts with select the component type of an array table type.
     */
    private static boolean isChangeableField(String columnTitle, IOpenClass tableType) {
        var fieldName = columnTitle;
        // if it's field chain started with array index
        var openClass = tableType;
        while (openClass.isArray() && !fieldName.isEmpty() && fieldName.charAt(0) == '[') {
            var endIndex = fieldName.indexOf(']');
            if (!isNumericArrayIndex(fieldName, endIndex)) {
                break;
            }
            openClass = openClass.getComponentClass();
            if (!openClass.isArray()) {
                endIndex++;
                if (fieldName.length() <= endIndex || fieldName.charAt(endIndex) != '.') {
                    endIndex--;
                }
            }
            fieldName = fieldName.substring(endIndex + 1);
        }

        // if it is field chain get first token
        var dotIndex = fieldName.indexOf('.');
        if (dotIndex > 0) {
            fieldName = fieldName.substring(0, dotIndex);
        }
        // if it is array field correct field name
        var brIndex = fieldName.indexOf('[');
        if (brIndex > 0) {
            fieldName = fieldName.substring(0, brIndex);
        }

        IOpenField field = findField(fieldName, null, openClass);

        return field != null && !field.isConst() && field.isWritable();
    }

    /**
     * Checks whether the text between the leading bracket and the closing one at the given index is a non-empty
     * number.
     */
    private static boolean isNumericArrayIndex(String fieldName, int endIndex) {
        var arrayIndex = false;
        for (var j = 1; j < endIndex; j++) {
            var ch = fieldName.charAt(j);
            arrayIndex = Character.isDigit(ch);
            if (!arrayIndex) {
                break; // stop parsing if index is not numeric
            }
        }
        return arrayIndex;
    }

    private static int countRefs(ILogicalTable dataTable) {
        var count = 0;
        var width = dataTable.getWidth();

        for (var i = 0; i < width; ++i) {

            var fieldName = dataTable.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (fieldName == null) {
                continue;
            }

            fieldName = StringUtils.trim(fieldName);

            if (fieldName.startsWith(">")) {
                count++;
            }
        }
        return count;
    }

    private static int countResFields(ILogicalTable dataTable) {

        var count = 0;
        var width = dataTable.getWidth();

        for (var i = 0; i < width; ++i) {

            var fieldName = dataTable.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (fieldName == null) {
                continue;
            }

            // Remove extra spaces.
            //
            fieldName = StringUtils.trim(fieldName);

            // if it is field chain get first token
            var dotIndex = fieldName.indexOf('.');
            if (dotIndex > 0) {
                fieldName = fieldName.substring(0, dotIndex);
            }
            // if it is array field correct field name
            var brIndex = fieldName.indexOf('[');
            if (brIndex > 0) {
                fieldName = fieldName.substring(0, brIndex);
            }

            if (TestMethodHelper.EXPECTED_RESULT_NAME.equals(fieldName)) {
                count++;
            }
        }

        return count;
    }

    // Data tables of existing projects use the _PK_ column, so it stays until its removal is decided.
    @SuppressWarnings("java:S1135")
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
            if (tableBody.getWidth() == 1 || isHorizontalTable(tableBody, tableType)) {
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
        var startIndex = getStartIndexForDataWithTitlesSection(horizDataTableBody);

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
        var horizDataTableBody = verticalTableBody.transpose();
        var startIndex = getStartIndexForDataWithTitlesSection(horizDataTableBody);
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

        var hasForeignKeysRow = hasForeignKeysRow(horizDataTableBody);

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

        var endRow = getEndRowForDescriptorSection(horizDataTableBody);

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

        var hasForeignKeysRow = hasForeignKeysRow(horizDataTableBody);

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
     *                          header section (consists from header row + property section) and descriptor section (consists from
     *                          JavaBean name obligatory + optional index row, see {@link #hasForeignKeysRow(ILogicalTable)}).<br>
     *                          This part of table may consists from optional first title row and followed data rows.
     * @param bindingContext    is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
     *                          <code>null</code>.
     * @param column            Number of column in data table.
     * @param hasColumnTitleRow Flag shows if data table has column tytle row.
     * @return Column title (aka Display name).
     */
    public static StringValue makeColumnTitle(IBindingContext bindingContext,
                                              ILogicalTable dataWithTitleRows,
                                              int column,
                                              boolean hasColumnTitleRow) {

        var value = StringUtils.EMPTY;

        if (hasColumnTitleRow) {

            var titleCell = dataWithTitleRows.getSubtable(column, 0, 1, 1);
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
     * @param bindingContext is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
     *                       <code>null</code>.
     */
    // The descriptors depend on the table, its type, the descriptor and data rows, and the binding options.
    @SuppressWarnings("java:S107")
    public static ColumnDescriptor[] makeDescriptors(IBindingContext bindingContext,
                                                     ITable table,
                                                     IOpenClass type,
                                                     OpenL openl,
                                                     ILogicalTable descriptorRows,
                                                     ILogicalTable dataWithTitleRows,
                                                     boolean hasForeignKeysRow,
                                                     boolean hasColumnTitleRow,
                                                     boolean supportConstructorFields)
                                                             throws OpenLCompilationException {

        var width = descriptorRows.getWidth();
        ColumnDescriptor[] columnDescriptors = new ColumnDescriptor[width];

        var columnIdentifiers = getColumnIdentifiers(bindingContext, descriptorRows);
        var columnNum = 0;
        for (IdentifierNodesBucket node : columnIdentifiers) {
            var fieldAccessorChainTokens = node.getNode();
            if (fieldAccessorChainTokens != null) {

                IOpenField descriptorField = null;

                // indicates if field is a constructor.
                var constructorField = isConstructorField(fieldAccessorChainTokens,
                        hasForeignKeysRow,
                        supportConstructorFields);

                if (!constructorField && !(fieldAccessorChainTokens.length == 1 && hasForeignKeysRow && CONSTRUCTOR_FIELD
                        .equals(fieldAccessorChainTokens[0].getIdentifier()))) {
                    descriptorField = processFieldsChain(bindingContext, table, type, fieldAccessorChainTokens);
                }

                var foreignKey = hasForeignKeysRow ? parseForeignKey(bindingContext, descriptorRows, columnNum)
                        : ForeignKey.NONE;

                StringValue header = DataTableBindHelper
                        .makeColumnTitle(bindingContext, dataWithTitleRows, columnNum, hasColumnTitleRow);

                ColumnDescriptor currentColumnDescriptor = getColumnDescriptor(openl,
                        descriptorField,
                        constructorField,
                        foreignKey.table,
                        foreignKey.key,
                        foreignKey.tableAccessorChainTokens,
                        foreignKey.cell,
                        header,
                        fieldAccessorChainTokens,
                        columnNum);

                columnDescriptors[columnNum] = currentColumnDescriptor;
            }
            columnNum++;
        }

        propagateSupportMultirows(columnDescriptors, columnIdentifiers.size());

        return columnDescriptors;
    }

    private static boolean isConstructorField(IdentifierNode[] fieldAccessorChainTokens,
                                              boolean hasForeignKeysRow,
                                              boolean supportConstructorFields) {
        if (fieldAccessorChainTokens.length == 1 && !hasForeignKeysRow) {
            var fieldNameNode = fieldAccessorChainTokens[0];
            return supportConstructorFields && CONSTRUCTOR_FIELD.equals(fieldNameNode.getIdentifier());
        }
        return false;
    }

    /**
     * The foreign key of a column: the referenced table, the tokens of the accessor chain of the table reference, the
     * referenced column, and the cell that defines the foreign key.
     */
    @RequiredArgsConstructor
    private static final class ForeignKey {
        private static final ForeignKey NONE = new ForeignKey(null, null, null, null);

        private final IdentifierNode table;
        private final IdentifierNode key;
        private final IdentifierNode[] tableAccessorChainTokens;
        private final ICell cell;
    }

    private static ForeignKey parseForeignKey(IBindingContext bindingContext,
                                              ILogicalTable descriptorRows,
                                              int columnNum) throws OpenLCompilationException {
        IdentifierNode[] foreignKeyTokens = getForeignKeyTokens(bindingContext, descriptorRows, columnNum);
        var foreignKeyTable = foreignKeyTokens.length > 0 ? foreignKeyTokens[0] : null;
        var foreignKey = foreignKeyTokens.length > 1 ? foreignKeyTokens[1] : null;
        var foreignKeyCell = descriptorRows.getSubtable(columnNum, 1, 1, 1).getSource().getCell(0, 0);
        IdentifierNode[] accessorChainTokens = null;

        if (foreignKeyTable != null) {
            accessorChainTokens = Tokenizer
                    .tokenize(foreignKeyTable.getModule(), LINK_DELIMETERS, foreignKeyTable.getLocation());

            if (!ArrayUtils.isEmpty(accessorChainTokens)) {
                foreignKeyTable = accessorChainTokens.length > 0 ? accessorChainTokens[0] : null;
            }
        }
        return new ForeignKey(foreignKeyTable, foreignKey, accessorChainTokens, foreignKeyCell);
    }

    /**
     * Makes the columns before a column that supports multiple rows support them too.
     */
    private static void propagateSupportMultirows(ColumnDescriptor[] columnDescriptors, int columnsCount) {
        var hasSupportMultirowsAfter = false;

        for (var columnNum = columnsCount - 1; columnNum >= 0; columnNum--) {
            if (columnDescriptors[columnNum] != null) {
                if (hasSupportMultirowsAfter) {
                    columnDescriptors[columnNum].setSupportMultirows(true);
                } else if (columnDescriptors[columnNum].isSupportMultirows()) {
                    hasSupportMultirowsAfter = true;
                }
            }
        }
    }

    /**
     * @param bindingContext is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
     *                       <code>null</code>.
     */
    private static SequencedSet<IdentifierNodesBucket> getColumnIdentifiers(IBindingContext bindingContext,
                                                                            ILogicalTable descriptorRows) {
        var width = descriptorRows.getWidth();
        var identifiers = new LinkedHashSet<IdentifierNodesBucket>();
        for (var columnNum = 0; columnNum < width; columnNum++) {
            GridCellSourceCodeModule cellSourceModule = getCellSourceModule(descriptorRows, columnNum);
            cellSourceModule.update(bindingContext);

            var code = cellSourceModule.getCode();

            if (!code.isEmpty()) {

                IdentifierNode[] fieldAccessorChainTokens = null;
                try {
                    // fields names nodes
                    fieldAccessorChainTokens = trimAndSplitPrecisionToken(
                            Tokenizer.tokenize(cellSourceModule, CODE_DELIMETERS));
                } catch (OpenLCompilationException e) {
                    log.debug(ERROR_OCCURRED, e);
                    var message = "Cannot parse field source '%s'".formatted(code);
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                    bindingContext.addError(error);
                }
                if (identifiers.contains(new IdentifierNodesBucket(fieldAccessorChainTokens))) {
                    var message = "Found duplicate of field '%s'".formatted(code);
                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                    bindingContext.addError(error);
                } else {
                    var added = identifiers.add(new IdentifierNodesBucket(fieldAccessorChainTokens));
                    if (!added) {
                        var message = "Found duplicate of field '%s'".formatted(code);
                        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, cellSourceModule);
                        bindingContext.addError(error);
                    }
                }
            } else {
                identifiers.add(new IdentifierNodesBucket(null));
            }
        }
        return identifiers;
    }

    private static IdentifierNode[] trimAndSplitPrecisionToken(IdentifierNode[] chainTokens) {
        if (chainTokens.length == 0) {
            return chainTokens;
        }

        // Trim all identifiers and set correct location for them.
        for (var i = 0; i < chainTokens.length; i++) {
            var token = chainTokens[i];
            var identifier = token.getIdentifier();
            var trimmed = identifier.trim();
            if (trimmed.length() != identifier.length()) {
                var tokenStart = token.getLocation().getStart().getAbsolutePosition(null) + identifier.indexOf(trimmed);

                TextInterval fieldInterval = LocationUtils.createTextInterval(tokenStart,
                        tokenStart + trimmed.length());
                chainTokens[i] = new IdentifierNode(token.getType(), fieldInterval, trimmed, token.getModule());
            }
        }

        // Extract precision node if exists in last identifier chain
        var token = chainTokens[chainTokens.length - 1];
        var identifier = token.getIdentifier();

        var matcher = FIELD_WITH_PRECISION_PATTERN.matcher(identifier);
        if (matcher.matches()) {
            // Separate the token to: 1) field 2) precision
            var field = matcher.group(1);
            var precision = matcher.group(2);

            var tokenStart = token.getLocation().getStart().getAbsolutePosition(null);
            var fieldStart = identifier.indexOf(field);
            var precisionStart = identifier.lastIndexOf(precision);
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
        var gridTable = descriptorRows.getColumn(columnNum).getSource();
        return new GridCellSourceCodeModule(gridTable);
    }

    // A column descriptor takes the field, the foreign key and the header the column is parsed into.
    @SuppressWarnings("java:S107")
    private static ColumnDescriptor getColumnDescriptor(OpenL openl,
                                                        IOpenField descriptorField,
                                                        boolean constructorField,
                                                        IdentifierNode foreignKeyTable,
                                                        IdentifierNode foreignKey,
                                                        IdentifierNode[] foreignKeyTableAccessorChainTokens,
                                                        ICell foreignKeyCell,
                                                        StringValue header,
                                                        IdentifierNode[] fieldChainTokens,
                                                        int columnNum) {
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
                    fieldChainTokens,
                    columnNum);
        } else {
            var primaryKey = fieldChainTokens.length > 0 && FPK
                    .equals(fieldChainTokens[fieldChainTokens.length - 1].getIdentifier());
            currentColumnDescriptor = new ColumnDescriptor(descriptorField,
                    header,
                    openl,
                    constructorField,
                    fieldChainTokens,
                    columnNum,
                    primaryKey);
        }
        return currentColumnDescriptor;
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
        return new FieldsChainProcessor(bindingContext, table, type, fieldAccessorChainTokens).process();
    }

    /**
     * Finds the fields of a chain one by one. The chain is not built when a field of it is not found.
     */
    private static final class FieldsChainProcessor {
        private final IBindingContext bindingContext;
        private final ITable table;
        private final IOpenClass type;
        private IdentifierNode[] fieldAccessorChainTokens;
        private IOpenClass loadedFieldType;
        // the chain of fields to access the target field, e.g. for
        // driver.name it will be array consisting of two fields:
        // 1st for driver, 2nd for name
        private IOpenField[] fieldAccessorChain;
        private boolean hasAccessByArrayId;
        private final StringBuilder partPathFromRoot = new StringBuilder();
        private final boolean isResult;
        private final boolean multiRowsArentSupported;
        private boolean stop;

        private FieldsChainProcessor(IBindingContext bindingContext,
                                     ITable table,
                                     IOpenClass type,
                                     IdentifierNode[] fieldAccessorChainTokens) {
            this.bindingContext = bindingContext;
            this.table = table;
            this.type = type;
            this.fieldAccessorChainTokens = fieldAccessorChainTokens;
            this.loadedFieldType = type;
            this.fieldAccessorChain = new IOpenField[fieldAccessorChainTokens.length];
            this.isResult = fieldAccessorChainTokens[0].getIdentifier()
                    .startsWith(TestMethodHelper.EXPECTED_RESULT_NAME) || fieldAccessorChainTokens[0].getIdentifier()
                    .startsWith(TestMethodHelper.EXPECTED_ERROR);
            this.multiRowsArentSupported = type instanceof TestMethodOpenClass && isResult;
        }

        private IOpenField process() {
            IOpenField chainField = null;
            // HERE
            for (var fieldIndex = 0; !stop && fieldIndex < fieldAccessorChain.length; fieldIndex++) {
                processField(fieldIndex);
            }
            if (!CollectionUtils.hasNull(fieldAccessorChain)) { // check successful
                // loading of all
                // fields in
                // fieldAccessorChain.
                chainField = new FieldChain(type,
                        fieldAccessorChain,
                        fieldAccessorChainTokens,
                        hasAccessByArrayId);
            }
            return chainField;
        }

        private void processField(int fieldIndex) {
            var fieldNameNode = fieldAccessorChainTokens[fieldIndex];
            var identifier = fieldNameNode.getIdentifier();

            if (fieldIndex == 0 && !(type instanceof TestMethodOpenClass)
                    && processThisCollectionElement(fieldIndex, fieldNameNode, identifier)) {
                return;
            }

            if (fieldIndex > 0 && fieldIndex == fieldAccessorChain.length - 1 && identifier.equals(FPK)) {
                processPrimaryKey(fieldIndex, fieldNameNode);
            } else if (isResult && StringUtils.matches(PRECISION_PATTERN, identifier)) {
                fieldAccessorChain = ArrayUtils.remove(fieldAccessorChain, fieldIndex);
                fieldAccessorChainTokens = ArrayUtils.remove(fieldAccessorChainTokens, fieldIndex);
                // Skip creation of IOpenField
            } else {
                processNamedField(fieldIndex, fieldNameNode, identifier);
            }
        }

        /**
         * Resolves an access to an element of a collection of the table type, like {@code this[0]}.
         *
         * @return {@code false} when the field is not such an access
         */
        private boolean processThisCollectionElement(int fieldIndex, IdentifierNode fieldNameNode, String identifier) {
            ThisCollectionElementField collectionElementField = null;
            IOpenClass collectionElementType = null;
            if (StringUtils.matches(THIS_ARRAY_ACCESS_PATTERN, identifier) && type.isArray()) {
                collectionElementType = type.getComponentClass();
                collectionElementField = new ThisCollectionElementField(getCollectionIndex(fieldNameNode),
                        collectionElementType,
                        CollectionType.ARRAY);
            } else if (StringUtils.matches(THIS_LIST_ACCESS_PATTERN, identifier) && ClassUtils
                    .isAssignable(type.getInstanceClass(), List.class)) {
                collectionElementType = getTypeForCollection(fieldNameNode, null, bindingContext);
                collectionElementField = new ThisCollectionElementField(getCollectionIndex(fieldNameNode),
                        collectionElementType,
                        CollectionType.LIST);
            } else if (StringUtils.matches(THIS_MAP_ACCESS_PATTERN, identifier) && ClassUtils
                    .isAssignable(type.getInstanceClass(), Map.class)) {
                collectionElementType = getTypeForCollection(fieldNameNode, null, bindingContext);
                collectionElementField = new ThisCollectionElementField(getCollectionKey(fieldNameNode),
                        collectionElementType);
            }
            if (collectionElementField == null) {
                return false;
            }
            // If type is not found, chain cannot be evaluated further
            if (collectionElementType != null) {
                fieldAccessorChain[fieldIndex] = collectionElementField;
                loadedFieldType = collectionElementType;
            } else {
                stop = true;
            }
            return true;
        }

        private void processPrimaryKey(int fieldIndex, IdentifierNode fieldNameNode) {
            if (fieldAccessorChain[fieldIndex - 1]
                    instanceof CollectionElementWithMultiRowField datatypeCollectionMultiRowElementField) {
                // Multi-rows support. PK for arrays.
                var newDatatypeArrayMultiRowElementField = new CollectionElementWithMultiRowField(
                        datatypeCollectionMultiRowElementField.getField(),
                        datatypeCollectionMultiRowElementField.getFieldPathFromRoot(),
                        JavaOpenClass.STRING,
                        datatypeCollectionMultiRowElementField.getCollectionType(),
                        true);
                IOpenField[] fieldAccessorChainTmp = new IOpenField[fieldAccessorChainTokens.length - 1];
                System.arraycopy(fieldAccessorChain, 0, fieldAccessorChainTmp, 0,
                        fieldAccessorChainTokens.length - 1);
                fieldAccessorChain = fieldAccessorChainTmp;
                fieldAccessorChain[fieldAccessorChain.length - 1] = newDatatypeArrayMultiRowElementField;
            } else {
                SyntaxNodeException error = SyntaxNodeExceptionUtils
                        .createError("Primary key was defined incorrectly.", fieldNameNode);
                bindingContext.addError(error);
            }
        }

        private void processNamedField(int fieldIndex, IdentifierNode fieldNameNode, String identifier) {
            IOpenField fieldInChain = findFieldInChain(fieldIndex, fieldNameNode, identifier);

            if (fieldIndex > 0
                    && (fieldAccessorChain[fieldIndex - 1] instanceof CollectionElementField
                            || fieldAccessorChain[fieldIndex - 1] instanceof SpreadsheetResultField)
                    && fieldAccessorChain[fieldIndex - 1].getType().equals(JavaOpenClass.OBJECT)
                    && StringUtils.matches(SPREADSHEETRESULT_FIELD_PATTERN, identifier)) {
                var aOpenField = (AOpenField) fieldAccessorChain[fieldIndex - 1];
                aOpenField.setType(JavaOpenClass.getOpenClass(SpreadsheetResult.class));
            }

            if (fieldInChain == null) {
                // in this case current field and all the followings in
                // fieldAccessorChain will be nulls.
                //
                stop = true;
            } else {
                loadedFieldType = fieldInChain.getType();

                fieldAccessorChain[fieldIndex] = fieldInChain;
                if (fieldIndex > 0) {
                    partPathFromRoot.append('.');
                }
                partPathFromRoot.append(fieldInChain.getName());
            }
        }

        /**
         * Finds a field, or an element of a collection field. A collection field that is not the last in the chain
         * is accessed by its elements.
         */
        private IOpenField findFieldInChain(int fieldIndex, IdentifierNode fieldNameNode, String identifier) {
            IOpenField fieldInChain;
            var collectionAccessPattern = StringUtils.matches(COLLECTION_ACCESS_BY_INDEX_PATTERN,
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

                if (fieldIndex != fieldAccessorChain.length - 1 && fieldInChain != null
                        && fieldInChain.getType() != NullOpenClass.the && (fieldInChain.getType().isArray()
                        || ClassUtils.isAssignable(fieldInChain.getType().getInstanceClass(), List.class))) {
                    fieldInChain = getWritableCollectionElement(bindingContext,
                            fieldNameNode,
                            table,
                            loadedFieldType,
                            partPathFromRoot.toString(),
                            !multiRowsArentSupported);
                }
            }
            return fieldInChain;
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
                        .findField(fieldName, table, JavaOpenClass.getOpenClass(SpreadsheetResult.class));
            }
            if (field == null) {
                String errorMessage;
                if (loadedFieldType instanceof TestMethodOpenClass class1) {
                    var sb = new StringBuilder();
                    MethodUtil.printMethod(class1.getTestedMethod(), sb);
                    errorMessage = "Expected one of the parameters from the method '%s', but found '%s'."
                            .formatted(sb, fieldName);
                } else {
                    errorMessage = "%s '%s' is not found in type '%s'.".formatted(
                            loadedFieldType.isStatic() ? "Static field" : "Field",
                            fieldName,
                            loadedFieldType.getName());
                }
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(errorMessage, currentFieldNameNode);
                bindingContext.addError(error);
                return null;
            }

            if (!field.isWritable()) {
                var message = "Field '%s' is not writable in type '%s'."
                        .formatted(fieldName, loadedFieldType.getName());
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
                bindingContext.addError(error);
                return null;
            }

            return field;
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
                        .findField(name, table, JavaOpenClass.getOpenClass(SpreadsheetResult.class));
            }

            if (field == null) {
                var message = "%s '%s' is not found."
                        .formatted(loadedFieldType.isStatic() ? "Static field" : "Field", name);
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
                bindingContext.addError(error);
                return null;
            }

            if (!ClassUtils.isAssignable(field.getType().getInstanceClass(), Map.class) && !ClassUtils.isAssignable(
                    field.getType().getInstanceClass(),
                    List.class) && !field.getType().isArray() && Object.class != field.getType().getInstanceClass()) {
                var message = "Expected a collection type for field '%s', but found type '%s'.".formatted(
                        name,
                        field.getType().toString());
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
                bindingContext.addError(error);
                return null;
            }

            IOpenField collectionAccessField;
            if (multiRowElement) {
                collectionAccessField = createMultiRowElementField(bindingContext,
                        currentFieldNameNode,
                        loadedFieldType,
                        partPathFromRoot,
                        field);
            } else if (ClassUtils.isAssignable(field.getType().getInstanceClass(), Map.class)) {
                collectionAccessField = createMapElementField(bindingContext, currentFieldNameNode, loadedFieldType, field);
            } else {
                collectionAccessField = createIndexedElementField(bindingContext,
                        currentFieldNameNode,
                        loadedFieldType,
                        field);
            }
            if (collectionAccessField == null) {
                return null;
            }
            if (!collectionAccessField.isWritable()) {
                var message = "Field '%s' is not writable in %s.".formatted(name, loadedFieldType.getName());
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, currentFieldNameNode);
                bindingContext.addError(error);
                return null;
            }

            return collectionAccessField;
        }

        private static IOpenField createMultiRowElementField(IBindingContext bindingContext,
                                                             IdentifierNode currentFieldNameNode,
                                                             IOpenClass loadedFieldType,
                                                             String partPathFromRoot,
                                                             IOpenField field) {
            IOpenField collectionAccessField;
            var fieldType = field.getType();
            if (ClassUtils.isAssignable(fieldType.getInstanceClass(), List.class)) {
                IOpenClass elementType = getTypeForCollection(currentFieldNameNode,
                        loadedFieldType instanceof TestMethodOpenClass tmoc ? tmoc : null,
                        bindingContext);
                collectionAccessField = new CollectionElementWithMultiRowField(field,
                        buildRootPathForDatatypeArrayMultiRowElementField(partPathFromRoot, field.getName()),
                        elementType,
                        CollectionType.LIST);
            } else {
                collectionAccessField = new CollectionElementWithMultiRowField(field,
                        buildRootPathForDatatypeArrayMultiRowElementField(partPathFromRoot, field.getName()),
                        getArrayElementType(fieldType),
                        CollectionType.ARRAY);
            }
            return collectionAccessField;
        }

        /**
         * Creates the field of a map element.
         *
         * @return {@code null} when the key cannot be parsed, which is reported to the binding context
         */
        private static IOpenField createMapElementField(IBindingContext bindingContext,
                                                        IdentifierNode currentFieldNameNode,
                                                        IOpenClass loadedFieldType,
                                                        IOpenField field) {
            Object mapKey;
            try {
                mapKey = getMapKey(currentFieldNameNode,
                        loadedFieldType instanceof TestMethodOpenClass tmoc ? tmoc : null,
                        bindingContext);
            } catch (SyntaxNodeException e) {
                bindingContext.addError(e);
                return null;
            } catch (Exception e) {
                log.debug(ERROR_OCCURRED, e);
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Failed to parse a map key.",
                        currentFieldNameNode);
                bindingContext.addError(error);
                return null;
            }
            IOpenClass elementType = getTypeForCollection(currentFieldNameNode,
                    loadedFieldType instanceof TestMethodOpenClass tmoc ? tmoc : null,
                    bindingContext);
            return new CollectionElementField(field, mapKey, elementType);
        }

        /**
         * Creates the field of an array or a list element.
         *
         * @return {@code null} when the index cannot be parsed, which is reported to the binding context
         */
        private static IOpenField createIndexedElementField(IBindingContext bindingContext,
                                                            IdentifierNode currentFieldNameNode,
                                                            IOpenClass loadedFieldType,
                                                            IOpenField field) {
            int index;
            try {
                index = getCollectionIndex(currentFieldNameNode);
            } catch (Exception e) {
                log.debug(ERROR_OCCURRED, e);
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Failed to parse an array index.",
                        currentFieldNameNode);
                bindingContext.addError(error);
                return null;
            }
            IOpenField collectionAccessField;
            var fieldType = field.getType();
            if (ClassUtils.isAssignable(fieldType.getInstanceClass(), List.class)) {
                IOpenClass elementType = getTypeForCollection(currentFieldNameNode,
                        loadedFieldType instanceof TestMethodOpenClass tmoc ? tmoc : null,
                        bindingContext);
                collectionAccessField = new CollectionElementField(field, index, elementType, CollectionType.LIST);
            } else {
                collectionAccessField = new CollectionElementField(field,
                        index,
                        getArrayElementType(fieldType),
                        CollectionType.ARRAY);
            }
            return collectionAccessField;
        }

        private static IOpenClass getTypeForCollection(IdentifierNode identifierNode,
                                                       TestMethodOpenClass testMethodOpenClass,
                                                       IBindingContext bindingContext) {
            var typeSeparatorIndex = identifierNode.getIdentifier().indexOf(':');
            if (typeSeparatorIndex < 0) {
                return getCollectedType(testMethodOpenClass, bindingContext);
            }

            var typeName = identifierNode.getIdentifier().substring(typeSeparatorIndex + 1);
            typeName = typeName.trim();

            var type = bindingContext.findType(typeName);
            if (type == null) {
                var message = "Cannot bind node: '%s'. Cannot find type: '%s'.".formatted(identifierNode, typeName);
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, identifierNode);
                bindingContext.addError(error);
            }
            return type;
        }

        private static String buildRootPathForDatatypeArrayMultiRowElementField(String partPathFromRoot, String fieldName) {
            if (StringUtils.isEmpty(partPathFromRoot)) {
                return fieldName + "[]";
            } else {
                return partPathFromRoot + "." + fieldName + "[]";
            }
        }

        /**
         * Returns the element type of an array field. A field of the {@code Object} type holds {@code Object} elements.
         */
        private static IOpenClass getArrayElementType(IOpenClass fieldType) {
            if (fieldType instanceof UserErrorOpenClass) {
                return new UserErrorOpenClass();
            } else if (!fieldType.isArray() && Object.class == fieldType.getInstanceClass()) {
                return JavaOpenClass.OBJECT;
            } else {
                return fieldType.getComponentClass();
            }
        }

        /**
         * Returns the type of the values collected by the tested method, or {@code Object} when it is unknown.
         */
        private static IOpenClass getCollectedType(TestMethodOpenClass testMethodOpenClass,
                                                   IBindingContext bindingContext) {
            if (testMethodOpenClass != null
                    && testMethodOpenClass.getTestedMethod() instanceof ExecutableRulesMethod executableRulesMethod) {
                var tableSyntaxNode = executableRulesMethod.getSyntaxNode();
                if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
                    var cType = bindingContext
                            .findType(
                                    tableSyntaxNode.getHeader()
                                            .getCollectParameters()[ClassUtils
                                            .isAssignable(executableRulesMethod.getType().getInstanceClass(), Map.class) ? 1
                                            : 0]);
                    if (cType != null) {
                        return cType;

                    }
                }
            }
            return JavaOpenClass.OBJECT;
        }

        private static Object getMapKey(IdentifierNode currentFieldNameNode,
                                        TestMethodOpenClass testMethodOpenClass,
                                        IBindingContext bindingContext) throws SyntaxNodeException {
            var s = currentFieldNameNode.getIdentifier();
            s = s.substring(s.indexOf('[') + 1, s.lastIndexOf(']')).trim();
            if (testMethodOpenClass != null
                    && testMethodOpenClass.getTestedMethod() instanceof ExecutableRulesMethod executableRulesMethod) {
                var tableSyntaxNode = executableRulesMethod.getSyntaxNode();
                if (tableSyntaxNode.getHeader().getCollectParameters().length > 1) {
                    var keyOpenClass = bindingContext.findType(
                            tableSyntaxNode.getHeader().getCollectParameters()[0]);
                    if (keyOpenClass != null) {
                        return parseCollectionKey(s, keyOpenClass, currentFieldNameNode);
                    }
                }
            }
            return getCollectionKey(currentFieldNameNode);
        }

        /**
         * Converts a map key to the key type. A quoted key of the {@code String} type is unquoted.
         */
        private static Object parseCollectionKey(String s,
                                                 IOpenClass keyOpenClass,
                                                 IdentifierNode currentFieldNameNode) throws SyntaxNodeException {
            if (keyOpenClass.getInstanceClass() == String.class && StringUtils.matches(QUOTED, s)) {
                s = s.substring(1, s.length() - 1);
            }
            try {
                var converter = String2DataConvertorFactory
                        .getConvertor(keyOpenClass.getInstanceClass());
                return converter.parse(s, null);
            } catch (Exception e) {
                log.debug(ERROR_OCCURRED, e);
                throw SyntaxNodeExceptionUtils.createError(
                        "Cannot convert a key value '%s' to type '%s'.".formatted(s, keyOpenClass.getName()),
                        currentFieldNameNode);
            }
        }
    }

    public static Integer getPrecisionValue(IdentifierNode fieldNameNode) {
        try {
            var fieldName = fieldNameNode.getIdentifier();
            var txtIndex = fieldName.substring(fieldName.indexOf('(') + 1, fieldName.indexOf(')'));

            return Integer.parseInt(txtIndex);
        } catch (Exception e) {
            log.debug("Ignored error: ", e);
            return null;
        }
    }

    public static int getCollectionIndex(IdentifierNode fieldNameNode) {
        var fieldName = fieldNameNode.getIdentifier();
        var txtIndex = fieldName.substring(fieldName.indexOf('[') + 1, fieldName.indexOf(']')).trim();
        return Integer.parseInt(txtIndex);
    }

    public static String getCollectionName(IdentifierNode fieldNameNode) {
        var fieldName = fieldNameNode.getIdentifier();
        var ind = fieldName.indexOf('[');
        if (ind > 0) {
            return fieldName.substring(0, ind).trim();
        }

        return getFieldName(fieldName);
    }

    /**
     * Returns foreign_key_tokens from the current column.
     *
     * @param bindingContext is used for optimization {@link GridCellSourceCodeModule} in execution mode. Can be
     *                       <code>null</code>.
     * @see #hasForeignKeysRow(ILogicalTable)
     */
    private static IdentifierNode[] getForeignKeyTokens(IBindingContext bindingContext,
                                                        ILogicalTable descriptorRows,
                                                        int columnNum) throws OpenLCompilationException {

        var logicalRegion = descriptorRows.getSubtable(columnNum, 1, 1, 1);
        var indexRowSourceModule = new GridCellSourceCodeModule(logicalRegion.getSource(),
                bindingContext);

        // Should be in format
        // "> reference_table_name [reference_table_key_column]"
        return Tokenizer.tokenize(indexRowSourceModule, INDEX_ROW_REFERENCE_DELIMITER);
    }

    private static String getFieldName(String identifier) {
        var fieldName = identifier.trim();
        var endIndex = fieldName.indexOf(':');
        if (endIndex > 0) {
            fieldName = fieldName.substring(0, endIndex).trim();
        }
        return fieldName;
    }

    public static Object getCollectionKey(IdentifierNode currentFieldNameNode) {
        var s = currentFieldNameNode.getIdentifier();
        s = s.substring(s.indexOf('[') + 1, s.lastIndexOf(']')).trim();
        if (StringUtils.matches(QUOTED, s)) {
            return s.substring(1, s.length() - 1);
        } else {
            return Integer.valueOf(s);
        }
    }

    static boolean isPrecisionNode(IdentifierNode node) {
        return StringUtils.matches(PRECISION_PATTERN, node.getIdentifier());
    }
}
