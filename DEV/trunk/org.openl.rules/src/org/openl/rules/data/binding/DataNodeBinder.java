/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.binding;

import org.apache.commons.lang.StringUtils;
import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlToolAdaptor;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.data.IColumnDescriptor;
import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.ITable;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.rules.data.impl.OpenlBasedColumnDescriptor;
import org.openl.rules.data.impl.OpenlBasedDataTableModel;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DataNodeBinder extends AXlsTableBinder implements IXlsTableNames {

    private static final char INDEX_ROW_REFERENCE_START_SYMBOL = '>';

    private static final String INDEX_ROW_REFERENCE_DELIMITER = " >\n\r";

    private static final String CODE_DELIMETERS = ". \n\r";

    static class FieldChain extends AOpenField implements IOpenField {

        private IOpenField[] fields;

        static protected String makeNames(IOpenField[] fields) {
            String name = fields[0].getName();
            for (int i = 1; i < fields.length; i++) {
                name += "." + fields[i].getName();
            }
            return name;
        }

        /**
         * @param name
         * @param type
         */
        public FieldChain(IOpenClass type, IOpenField[] fields) {
            super(makeNames(fields), type);
            this.fields = fields;
        }

        /**
         *
         */

        public Object get(Object target, IRuntimeEnv env) {
            Object result = null;
            for (int i = 0; i < fields.length; i++) {
                result = fields[i].get(target, env);
                target = result;
            }

            return result;
        }

        /**
         *
         */

        @Override
        public IOpenClass getDeclaringClass() {
            return fields[0].getDeclaringClass();
        }

        /**
         *
         */

        @Override
        public IOpenClass getType() {
            return fields[fields.length - 1].getType();
        }

        /**
         *
         */

        public void set(Object target, Object value, IRuntimeEnv env) {
            // find last target, make if necessary
            for (int i = 0; i < fields.length - 1; i++) {
                Object newTarget = fields[i].get(target, env);
                if (newTarget == null) {
                    newTarget = fields[i].getType().newInstance(env);
                    fields[i].set(target, newTarget, env);
                }
                target = newTarget;
            }

            fields[fields.length - 1].set(target, value, env);

        }

    }

    // indexes of names in header
    private static final int TYPE_INDEX = 1, TABLE_NAME_INDEX = 2, DATABASE_NAME_INDEX = 3;

    /**
     * The pre-defined names of system and default databases
     */
    static final public String
    // this database is used for user data, including tests
            DEFAULT_DATAgBASE = "org.openl.database.default",
            /**
             * This database is used for all system data, configurations etc.
             * This database is processed first, data there can not use domain
             * types etc
             */

            SYSTEM_DATABASE = "system";

    public static final String CONSTRUCTOR_FIELD = "this";

    private static final String FPK = "_PK_";

    private IdentifierNode[] parsedHeader;

    /**
     * Foreign keys row is optional for data table. It consists reference for field value to other table. 
     * Foreign keys always starts from {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
     * @param dataTable
     * @return <code>TRUE</code> if second row in data table body (next to the field row) consists even one
     * value, in any column, starts with {@value #INDEX_ROW_REFERENCE_START_SYMBOL} symbol.
     */
    private boolean hasForeignKeysRow(ILogicalTable dataTable) {
        ILogicalTable potencialForeignKeysRow = dataTable.rows(1, 1);

        int columnsCount = potencialForeignKeysRow.getLogicalWidth();

        for (int i = 0; i < columnsCount; i++) {
            ILogicalTable cell = potencialForeignKeysRow.getLogicalColumn(i);

            String res = cell.getGridTable().getCell(0, 0).getStringValue();

            if (res == null || res.trim().length() == 0) {
                continue;
            }
            return res.charAt(0) == INDEX_ROW_REFERENCE_START_SYMBOL;
        }

        return false;

    }
    
    /**
     * Checks format of the data table header.
     * @param src
     * @throws BoundError if length of header is less than 3.
     */
    private void checkParsedHeader(IOpenSourceCodeModule src) throws BoundError {
        parsedHeader = TokenizerParser.tokenize(src, " \n\r");
        String errMsg;
        if (parsedHeader.length < 3) {
            errMsg = getErrMsgFormat();
            BoundError err = new BoundError(null, errMsg, null, src);
            throw err;
        }
    }

    

    private IOpenField findField(String fieldName, ITable table, IOpenClass tableType) {
        if (FPK.equals(fieldName)) {
            return new PrimaryKeyField(FPK, table);
        }
        return tableType.getField(fieldName, true);
    }    
    
    /**
     * Gets the table body, by skipping the table header and properties sections.
     * @param tsn
     * @return Table body without table header and properties section.
     */
    private ILogicalTable getTableBody(TableSyntaxNode tsn) {
        int startRow = tsn.getTableProperties() == null ? 1 : 2;
        ILogicalTable dataTable = tsn.getTable().rows(startRow);
        return dataTable;
    }
    
    /**
     * Checks if table representation is horizontal.
     * @param dataTable
     * @param tableType
     * @return
     */
    private boolean isHorizontalTable(ILogicalTable dataTable, IOpenClass tableType) {
        int cnt1 = countFields(dataTable, tableType);
        int cnt2 = countFields(dataTable.transpose(), tableType);
        return cnt1 < cnt2 ? false : true;
    }    

    /**
     * 
     * @param descriptorRows
     * @param type
     * @param openl
     * @param hasForeignKeysRow
     * @param dataWithTitleRows
     * @param table
     * @param hasColumnTytleRow
     * @return
     * @throws Exception
     */
    private IColumnDescriptor[] makeDescriptors(ILogicalTable descriptorRows,
            IOpenClass type, OpenL openl, boolean hasForeignKeysRow,
            ILogicalTable dataWithTitleRows, ITable table,
            boolean hasColumnTytleRow) throws Exception {

        int width = descriptorRows.getLogicalWidth();
        IColumnDescriptor[] columnDescriptors = new IColumnDescriptor[width];

        for (int columnNum = 0; columnNum < width; columnNum++) {
            GridCellSourceCodeModule cellSourceModule = new GridCellSourceCodeModule(
                    descriptorRows.getLogicalColumn(columnNum).getGridTable());

            String s = cellSourceModule.getCode();

            if (s.length() != 0) {
                IdentifierNode[] fieldAccessorChainTokens = TokenizerParser.tokenize(
                        cellSourceModule, CODE_DELIMETERS);

                // the chain of fields to access the target field, e.g. for
                // driver.name it will be array consisting of two fields: 
                // 1st for driver, 2nd for name
                IOpenField[] fieldAccessorChain = new IOpenField[fieldAccessorChainTokens.length];

                // the first field can be found in type itself
                IOpenClass loadedFieldType = type;
                for (int currentFieldNumInChain = 0; currentFieldNumInChain < fieldAccessorChain.length; currentFieldNumInChain++) {
                    IdentifierNode currentFieldNameNode = fieldAccessorChainTokens[currentFieldNumInChain];
                    String fieldName = currentFieldNameNode.getIdentifier();

                    if (CONSTRUCTOR_FIELD.equals(fieldName)
                            && fieldAccessorChain.length == 1) {
                        // targetType = loadedFieldType
                        break;
                    }

                    IOpenField field = getWritableField(currentFieldNameNode,
                            table, loadedFieldType);

                    loadedFieldType = field.getType();

                    fieldAccessorChain[currentFieldNumInChain] = field;
                }

                // the target type is the last field type, e.g. in driver.name the target type will be for name
                IOpenClass targetType = loadedFieldType != null ? loadedFieldType : type;
                
                // FIXME: If field is CONSTRUCTOR_FIELD then this variable will
                // be null and it's intended behavior. It should be rewritten as
                // it's very error prone.
                IOpenField field = fieldAccessorChain.length == 1 ? fieldAccessorChain[0]
                        : new FieldChain(type, fieldAccessorChain);

                IdentifierNode indexTable = null;
                IdentifierNode indexKey = null;

                if (hasForeignKeysRow) {
                    IdentifierNode[] foreignKeyTokens = getForeignKeyTokens(descriptorRows, columnNum);

                    indexTable = foreignKeyTokens.length > 0 ? foreignKeyTokens[0] : null;
                    indexKey = foreignKeyTokens.length > 1 ? foreignKeyTokens[1] : null;
                }

                StringValue header = makeColumnTitle(dataWithTitleRows,
                        columnNum, hasColumnTytleRow);
                
                OpenlBasedColumnDescriptor currentColumnDescriptor;

                if (indexTable != null) {
                    currentColumnDescriptor = new OpenlBasedColumnDescriptor(
                            field, indexTable, indexKey, header, openl);
                } else {
                    IString2DataConvertor convertor = getCellValueConvertor(
                            cellSourceModule, targetType);

                    currentColumnDescriptor = new OpenlBasedColumnDescriptor(
                            field, header, convertor, openl);
                }

                columnDescriptors[columnNum] = currentColumnDescriptor;
            }

        }
        return columnDescriptors;

    }

    /**
     * Gets the convertor from <code>String</code> to data for cell value.
     * @param cellSourceModule
     * @param targetType
     * @return
     * @throws BoundError
     */
    private IString2DataConvertor getCellValueConvertor(
            GridCellSourceCodeModule cellSourceModule, IOpenClass targetType)
            throws BoundError {
        IString2DataConvertor convertor;

        Class<?> targetTypeInstanceClass = targetType.getInstanceClass();
        if (targetTypeInstanceClass.isArray()) {
            targetTypeInstanceClass = targetTypeInstanceClass.getComponentType();
        }
        try {
            convertor = String2DataConvertorFactory.getConvertor(targetTypeInstanceClass);
        } catch (Throwable t) {
            throw new BoundError(null, null, t, cellSourceModule);
        }
        
        return convertor;
    }

    /**
     * Returns foreign_key_tokens from the current column. see {@link #hasForeignKeysRow(ILogicalTable)}.
     * @param descriptorRows
     * @param columnNum
     * @return
     */
    private IdentifierNode[] getForeignKeyTokens(ILogicalTable descriptorRows, int columnNum) {
        GridCellSourceCodeModule indexRowSourceModule = new GridCellSourceCodeModule(
                descriptorRows.getLogicalRegion(columnNum, 1, 1, 1)
                        .getGridTable());

        // Should be in format "> reference_table_name [reference_table_key_column]"
        IdentifierNode[] indexReference = TokenizerParser.tokenize(
                indexRowSourceModule, INDEX_ROW_REFERENCE_DELIMITER);
        return indexReference;
    }

    /**
     * Gets the field, and if it is not <code>null</code> and isWritable, returns it.
     * In other cases throws an exception.
     * @param currentFieldNameNode
     * @param table
     * @param loadedFieldType
     * @return
     * @throws BoundError
     */
    private IOpenField getWritableField(IdentifierNode currentFieldNameNode, ITable table,
            IOpenClass loadedFieldType)
            throws BoundError {
        String fieldName = currentFieldNameNode.getIdentifier();
        IOpenField field = findField(fieldName, table, loadedFieldType);

        if (field == null) {
            String errorMessage = String.format(
                    "Field \"%s\" not found in %s", fieldName,
                    loadedFieldType.getName());
            throw new BoundError(currentFieldNameNode, errorMessage);
        }

        if (!field.isWritable()) {
            BoundError err = new BoundError(currentFieldNameNode,
                    "Field " + fieldName + " is not Writable in "
                            + loadedFieldType.getName());
            throw err;
        }
        return field;
    }
    
    /**
     * Gets title for column if required or returns blank value.
     * 
     * @param dataWithTitleRows Logical part of the data table. Consider to include all rows from base table after 
     * header section (consists from header row + property section) and descriptor section (consists from JavaBean name 
     * obligatory + optional index row, see {@link #hasForeignKeysRow(ILogicalTable)}).<br> This part of table  may consists 
     * from optional first
     * title row and followed data rows.
     * @param column Number of column in data table.
     * @param hasColumnTytleRow Flag shows if data table has column tytle row.
     * @return Column title (aka Display name).
     */
    private StringValue makeColumnTitle(ILogicalTable dataWithTitleRows, int column, boolean hasColumnTytleRow) {
        StringValue result = null;
        if (hasColumnTytleRow) {
            ILogicalTable titleCell = dataWithTitleRows.getLogicalRegion(column, 0, 1, 1);

            String value = titleCell.getGridTable().getCell(0, 0).getStringValue();            
            
            String uri = titleCell.getGridTable().getUri(0, 0);
            if (value == null) {
                value = "";
            }
            result = new StringValue(value, value, value, uri);
        } else {
            result = new StringValue(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        }
        return result;
    }
    
    protected IOpenClass getTableType(String typeName, IBindingContext cxt,
            XlsModuleOpenClass module, DataTableBoundNode dataNode,
            String tableName) {
        IOpenClass tableType = cxt.findType(ISyntaxConstants.THIS_NAMESPACE,
                typeName);
        return tableType;

    }
    
    protected String getErrMsgFormat() {
        return "Data table format: Data <typename> <tablename> [database]";
    }
    
    /**
     * Gets the horizontal table representation from current table. If it was vertical it will 
     * be transposed
     * @param table
     * @param tableType
     * @return Horizontal representation of table.
     */
    protected ILogicalTable getHorizontalTable(TableSyntaxNode tsn, IOpenClass tableType) {
        ILogicalTable resultTable = null;
        ILogicalTable dataTable = getTableBody(tsn);

        if (isHorizontalTable(dataTable, tableType)) {
            resultTable = dataTable;
        } else {
            resultTable = dataTable.transpose();
        }
        return resultTable;
     }
    
    /**
     * Goes through the data table columns from left to right, and count number of <code>{@link IOpenField}</code>.
     * @param dataTable
     * @param tableType
     * @return Number of <code>{@link IOpenField}</code> found in the data table.
     */
    protected int countFields(ILogicalTable dataTable, IOpenClass tableType) {
        int cnt = 0;
        int w = dataTable.getLogicalWidth();
        for (int i = 0; i < w; ++i) {
            String fieldName = dataTable.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();
            if (fieldName == null) {
                continue;
            }
            IOpenField of = findField(fieldName, null, tableType);
            if (of != null) {
                ++cnt;
            }
        }
        return cnt;
    }

    protected DataTableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module) {
        return new DataTableBoundNode(tsn, module);
    }
    
    /**
     * Default method.
     * If you call this method, you want to process table with cell title row set to <code>TRUE</code>.
     */
    public ITable makeTable(XlsModuleOpenClass xlsOpenClass, TableSyntaxNode tsn, String tableName, IOpenClass tableType,
            IBindingContext cxt, OpenL openl) throws Exception {
        return makeTable(xlsOpenClass, tsn, tableName, tableType, cxt, openl, true);
    }
    
    public ITable makeTable(XlsModuleOpenClass xlsOpenClass, TableSyntaxNode tsn, String tableName, IOpenClass tableType,
            IBindingContext cxt, OpenL openl, boolean hasColumnTytleRow) throws Exception {
        
        ITable resultTable = xlsOpenClass.getDataBase().addNewTable(tableName, tsn);

        ILogicalTable horizDataTable = getHorizontalTable(tsn, tableType);
        
        boolean hasForeignKeysRow = hasForeignKeysRow(horizDataTable);
        
        // number of row to get descriptor rows from table.
        int toRow;
        
        // row to start getting data with header from table
        int fromRow;
        
        if (hasForeignKeysRow) {
            // descriptorRows will consist fieldRow + indexRow.
            toRow = 1;
            
            // dataWithHeader will starts from this row.
            fromRow = 2;            
        } else {
            // descriptorRows will consist only fieldRow.
            toRow = 0;
            
            // dataWithHeader will starts from this row.
            fromRow = 1;
        }
        
        
        ILogicalTable descriptorRows = horizDataTable.rows(0, toRow);

        ILogicalTable dataWithTitleRows = horizDataTable.rows(fromRow);

        IColumnDescriptor[] descriptors = makeDescriptors(descriptorRows, tableType, openl, hasForeignKeysRow,
                dataWithTitleRows, resultTable, hasColumnTytleRow);
        
        OpenlBasedDataTableModel dataModel = new OpenlBasedDataTableModel(tableName, tableType, openl, descriptors, 
                hasColumnTytleRow);

        tsn.getSubTables().put(VIEW_BUSINESS, dataWithTitleRows);

        OpenlToolAdaptor ota = new OpenlToolAdaptor(openl, cxt);

        xlsOpenClass.getDataBase().preLoadTable(resultTable, dataModel, dataWithTitleRows, ota);

        return resultTable;
    }    
    
    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module)
            throws Exception {

        DataTableBoundNode dataNode = makeNode(tsn, module);

        ILogicalTable table = LogicalTable.logicalTable(tsn.getTable());

        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());

        parsedHeader = TokenizerParser.tokenize(src, " \n\r");

        checkParsedHeader(src);

        String typeName = parsedHeader[TYPE_INDEX].getIdentifier();

        String tableName = parsedHeader[TABLE_NAME_INDEX].getIdentifier();

        IOpenClass tableType = getTableType(typeName, cxt, module, dataNode, tableName);
        
        String errMsg;
        if (tableType == null) {
            errMsg = "Type not found: " + typeName;

            BoundError err = new BoundError(parsedHeader[TYPE_INDEX], errMsg, null);
            throw err;
        }

        ITable dataTable = makeTable(module, tsn, tableName, tableType, cxt, openl);

        dataNode.setTable(dataTable);

        return dataNode;
    }
    
    

}