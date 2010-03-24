/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.binding;

import org.apache.commons.lang.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.data.ITable;
import org.openl.rules.data.impl.ColumnDescriptor;
import org.openl.rules.data.impl.ForeignKeyColumnDescriptor;
import org.openl.rules.data.impl.OpenlBasedDataTableModel;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
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

    private static final int HEADER_NUM_TOKENS = 3;

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
    private static final int TYPE_INDEX = 1;
    private static final int TABLE_NAME_INDEX = 2;
    /**
     * The pre-defined names of system and default databases
     */
    
    //FIXME: delete this declaration because it is not used anywhere (check)
    // this database is used for user data, including tests
    public static final String DEFAULT_DATABASE = "org.openl.database.default";
        
    //FIXME: delete this declaration because it is not used anywhere (check)
    /**
    * This database is used for all system data, configurations etc.
    * This database is processed first, data there can not use domain
    * types etc
    */
    public static final String SYSTEM_DATABASE = "system";

    private static final String CONSTRUCTOR_FIELD = "this";
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
     * @throws BoundError if length of header is less than {@link #HEADER_NUM_TOKENS}.
     */
    private void checkParsedHeader(IOpenSourceCodeModule src) throws BoundError {
        parsedHeader = TokenizerParser.tokenize(src, " \n\r");
        String errMsg;
        if (parsedHeader.length < HEADER_NUM_TOKENS) {
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
    protected ILogicalTable getTableBody(TableSyntaxNode tsn) {
        int startRow = 0;
        if (!tsn.hasPropertiesDefinedInTable()) {
            startRow = 1;
        } else {
            startRow = 2;
        }
        return tsn.getTable().rows(startRow);
    }
    
    /**
     * Checks if table representation is horizontal. Horizontal is data table where parameters are listed from left 
     * to right.</br>
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
     * @param dataTableBody                                                        
     * @param tableType
     * @return <code>TRUE</code> if table is horizontal.
     */
    private boolean isHorizontalTable(ILogicalTable dataTableBody, IOpenClass tableType) {
        boolean result = false;
        // if data table body contains only one row, we consider it is vertical.
        if (dataTableBody.getLogicalHeight() != 1) {
            int cnt1 = countFields(dataTableBody, tableType);
            int cnt2 = countFields(dataTableBody.transpose(), tableType);
            result = cnt1 < cnt2 ? false : true;
        }
        return result;
    }    
    
    private ColumnDescriptor[] makeDescriptors(ITable table, IOpenClass type, OpenL openl,
            ILogicalTable descriptorRows, ILogicalTable dataWithTitleRows,
            boolean hasForeignKeysRow, boolean hasColumnTytleRow) throws Exception {

        int width = descriptorRows.getLogicalWidth();
        ColumnDescriptor[] columnDescriptors = new ColumnDescriptor[width];

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
//                IOpenClass targetType = loadedFieldType != null ? loadedFieldType : type;
                
                // FIXME: If field is CONSTRUCTOR_FIELD then this variable will
                // be null and it's intended behavior. It should be rewritten as
                // it's very error prone.
                IOpenField field = fieldAccessorChain.length == 1 ? fieldAccessorChain[0]
                        : new FieldChain(type, fieldAccessorChain);

                IdentifierNode foreignKeyTable = null;
                IdentifierNode foreignKey = null;

                if (hasForeignKeysRow) {
                    IdentifierNode[] foreignKeyTokens = getForeignKeyTokens(descriptorRows, columnNum);

                    foreignKeyTable = foreignKeyTokens.length > 0 ? foreignKeyTokens[0] : null;
                    foreignKey = foreignKeyTokens.length > 1 ? foreignKeyTokens[1] : null;
                }

                StringValue header = makeColumnTitle(dataWithTitleRows,
                        columnNum, hasColumnTytleRow);
                
                ColumnDescriptor currentColumnDescriptor;

                if (foreignKeyTable != null) {
                    currentColumnDescriptor = new ForeignKeyColumnDescriptor(
                            field, foreignKeyTable, foreignKey, header, openl);
                } else {
                    currentColumnDescriptor = new ColumnDescriptor(
                            field, header, openl);
                }

                columnDescriptors[columnNum] = currentColumnDescriptor;
            }

        }
        return columnDescriptors;

    }

    /**
     * Returns foreign_key_tokens from the current column. see {@link #hasForeignKeysRow(ILogicalTable)}.
     * 
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
     * 
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
        String value = StringUtils.EMPTY;
        if (hasColumnTytleRow) {
            ILogicalTable titleCell = dataWithTitleRows.getLogicalRegion(column, 0, 1, 1);

            value = titleCell.getGridTable().getCell(0, 0).getStringValue();            
            
            String uri = titleCell.getGridTable().getUri(0, 0);
            
            // remove extra spaces
            value = StringUtils.trimToEmpty(value);
            result = new StringValue(value, value, value, uri);
        } else {
            result = new StringValue(value, value, value, value);
        }
        return result;
    }
    
    /**
     * 
     * @param typeName
     * @param cxt
     * @param module
     * @param dataNode
     * @param tableName
     * @return
     */
    protected IOpenClass getTableType(String typeName, IBindingContext cxt,
            XlsModuleOpenClass module, DataTableBoundNode dataNode,
            String tableName) {
        IOpenClass tableType = cxt.findType(ISyntaxConstants.THIS_NAMESPACE,
                typeName);
        return tableType;

    }
    
    protected String getErrMsgFormat() {
        return "Data table format: Data <typename> <tablename>";
    }
    
    /**
     * Gets the horizontal table representation from current table. If it was vertical it will 
     * be transposed.
     * 
     * @param tableBody
     * @param tableType
     * @return Horizontal representation of table.
     */
    private ILogicalTable getHorizontalTable(ILogicalTable tableBody, IOpenClass tableType) {
        ILogicalTable resultTable = null;

        if (isHorizontalTable(tableBody, tableType)) {
            resultTable = tableBody;
        } else {
            resultTable = tableBody.transpose();
        }
        return resultTable;
     }
    
    /**
     * Goes through the data table columns from left to right, and count number of <code>{@link IOpenField}</code>.
     * 
     * @param dataTable
     * @param tableType
     * @return Number of <code>{@link IOpenField}</code> found in the data table.
     */
    private int countFields(ILogicalTable dataTable, IOpenClass tableType) {
        int cnt = 0;
        int w = dataTable.getLogicalWidth();
        for (int i = 0; i < w; ++i) {
            String fieldName = dataTable.getLogicalColumn(i).getGridTable().getCell(0, 0).getStringValue();            
            if (fieldName == null) {
                continue;
            }
            // remove extra spaces
            fieldName = StringUtils.trim(fieldName);
            IOpenField of = findField(fieldName, null, tableType);
            if (of != null) {
                ++cnt;
            }
        }
        return cnt;
    }

    protected ATableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module) {
        return new DataTableBoundNode(tsn, module);
    }
    
    /**
     * Gets the Data_With_Titles rows from the data table body. Data_With_Titles start row consider to be the 
     * next row after descriptor section of the table and till the end of the table. 
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Data_With_Titles rows for current data table body. 
     */
    private ILogicalTable getDataWithTitleRows(ILogicalTable horizDataTableBody) {
        int startRow = getStartRowForDataWithTitlesSection(horizDataTableBody);         
        return horizDataTableBody.rows(startRow);
    }
    
    /**
     * Gets the number of the start row for Data_With_Titles section of the data table body.
     * It depends on whether table has or no the foreign key row.
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Number of the start row for the Data_With_Titles section.
     */
    private int getStartRowForDataWithTitlesSection(ILogicalTable horizDataTableBody) {
        boolean hasForeignKeysRow = hasForeignKeysRow(horizDataTableBody);
        int startRow;
        if (hasForeignKeysRow) {
         // Data_With_Titles will starts from this row.
            startRow = 2;            
        } else {
         // Data_With_Titles will starts from this row.
            startRow = 1;
        }
        return startRow;
    }
    
    /**
     * Gets the descriptor rows from the data table body. Descriptor rows are
     * obligatory parameter row and optional foreign key row if it exists in  the table.
     * 
     * @param horizDataTableBody Horizontal representation of data table body.
     * @return Descriptor rows for current data table body. 
     */
    private ILogicalTable getDescriptorRows(ILogicalTable horizDataTableBody) {
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
    private int getEndRowForDescriptorSection(ILogicalTable horizDataTableBody) {
        boolean hasForeignKeysRow = hasForeignKeysRow(horizDataTableBody);
        int endRow;
        
        if (hasForeignKeysRow) {
            // descriptorRows will consist fieldRow + iforeignKeyRow.
            endRow = 1;
        } else {
            // descriptorRows will consist only fieldRow.
            endRow = 0;
        }
        return endRow;
    }
    
    /**
     * Adds sub table for displaying on bussiness view.
     * 
     * @param tsn <code>TableSyntaxNode</code> representing table.
     * @param tableType Type of the data in table.
     */
    private void putSubTableForBussinesView(TableSyntaxNode tsn, IOpenClass tableType) {
        ILogicalTable tableBody = getTableBody(tsn);
        ILogicalTable horizDataTable = getHorizontalTable(tableBody, tableType);
        ILogicalTable dataWithTitleRows = getDataWithTitleRows(horizDataTable);
        tsn.getSubTables().put(VIEW_BUSINESS, dataWithTitleRows);
    }      
    
    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl, IBindingContext cxt, XlsModuleOpenClass module)
            throws Exception {

        DataTableBoundNode dataNode = (DataTableBoundNode) makeNode(tsn, module);

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
    
    /**
     * Default method. It is called during processing OpenL module.
     * If you call this method, you want to process table with cell title row set to <code>TRUE</code>.
     * calls {@link #processTable(XlsModuleOpenClass, ITable, ILogicalTable, String, IOpenClass, 
     * IBindingContext, OpenL, boolean)} to populate <code>ITable</code> with data. Also adds to 
     * <code>TableSyntaxNode</code> sub table for displaying on bussiness view.
     * 
     * @param xlsOpenClass Open class representing OpenL module.
     * @param tsn <code>TableSyntaxNode</code> to be processed.
     * @param tableName Name of the outcome table.
     * @param tableType Type of the data in table.
     * @param cxt OpenL context.
     * @param openl OpenL instance.
     */
    public ITable makeTable(XlsModuleOpenClass xlsOpenClass, TableSyntaxNode tsn, String tableName, IOpenClass tableType,
            IBindingContext cxt, OpenL openl) throws Exception {
        ITable resultTable = xlsOpenClass.getDataBase().addNewTable(tableName, tsn);
        ILogicalTable tableBody = getTableBody(tsn);
        processTable(xlsOpenClass, resultTable, tableBody, tableName, tableType, cxt, openl, true);
        putSubTableForBussinesView(tsn, tableType);
        
        return resultTable;
    }
    
    /**
     * Populate the <code>ITable</code> with data from <code>ILogicalTable</code>. 
     * 
     * @param xlsOpenClass Open class representing OpenL module.
     * @param tableToProcess Table to be processed.
     * @param tableBody Body of the table (without header and properties sections). Its like a source to process 
     * <code>ITable</code> with data.
     * @param tableName Name of the outcome table.
     * @param tableType Type of the data in table.
     * @param cxt OpenL context.
     * @param openl OpenL instance.
     * @param hasColumnTytleRow Flag representing if tableBody has title row for columns.
     * @throws Exception
     */
    public void processTable(XlsModuleOpenClass xlsOpenClass, ITable tableToProcess, ILogicalTable tableBody, 
            String tableName, IOpenClass tableType,
            IBindingContext cxt, OpenL openl, boolean hasColumnTytleRow) throws Exception {        
       
        ILogicalTable horizDataTableBody = getHorizontalTable(tableBody, tableType);
        
        ILogicalTable descriptorRows = getDescriptorRows(horizDataTableBody);
        
        ILogicalTable dataWithTitleRows = getDataWithTitleRows(horizDataTableBody);
        
        dataWithTitleRows = LogicalTable.logicalTable(dataWithTitleRows, descriptorRows, null);

        ColumnDescriptor[] descriptors = makeDescriptors(tableToProcess, tableType, openl, descriptorRows,  
                dataWithTitleRows, hasForeignKeysRow(horizDataTableBody), hasColumnTytleRow);
        
        OpenlBasedDataTableModel dataModel = new OpenlBasedDataTableModel(tableName, tableType, openl, descriptors, 
                hasColumnTytleRow);
        
        OpenlToolAdaptor ota = new OpenlToolAdaptor(openl, cxt);

        xlsOpenClass.getDataBase().preLoadTable(tableToProcess, dataModel, dataWithTitleRows, ota);
    }
    
    

}