/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class DataNodeBinder extends AXlsTableBinder {

    private static final int HEADER_NUM_TOKENS = 3;
    private static final String FORMAT_ERROR_MESSAGE = "Data table format: Data <typename> <tablename>";

    // indexes of names in header
    private static final int TYPE_INDEX = 1;
    private static final int TABLE_NAME_INDEX = 2;

    private IdentifierNode[] parsedHeader;

    protected String getFormatErrorMessage() {
        return FORMAT_ERROR_MESSAGE;
    }

    protected IOpenClass getTableType(String typeName,
            IBindingContext bindingContext,
            XlsModuleOpenClass module,
            DataTableBoundNode dataNode,
            String tableName) {

        return bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
    }

    protected ATableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module) {
        return new DataTableBoundNode(tsn, module);
    }

    protected IGridTable getTableBody(TableSyntaxNode tsn) {
        return DataTableBindHelper.getTableBody(tsn);
    }

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {

        DataTableBoundNode dataNode = (DataTableBoundNode) makeNode(tableSyntaxNode, module);
        IGridTable table = tableSyntaxNode.getTable();
        IOpenSourceCodeModule source = new GridCellSourceCodeModule(table.getGridTable());

        parsedHeader = Tokenizer.tokenize(source, " \n\r");
        checkParsedHeader(source);

        String typeName = parsedHeader[TYPE_INDEX].getIdentifier();
        String tableName = parsedHeader[TABLE_NAME_INDEX].getIdentifier();

        IOpenClass tableType = getTableType(typeName, bindingContext, module, dataNode, tableName);

        if (tableType == null) {
            String message = String.format("Type not found: '%s'", typeName);
            throw SyntaxNodeExceptionUtils.createError(message, null, parsedHeader[TYPE_INDEX]);
        }

        ITable dataTable = makeTable(module, tableSyntaxNode, tableName, tableType, bindingContext, openl);
        dataNode.setTable(dataTable);

        return dataNode;
    }

    /**
     * Populate the <code>ITable</code> with data from
     * <code>IGridTable</code>.
     * 
     * @param xlsOpenClass Open class representing OpenL module.
     * @param tableToProcess Table to be processed.
     * @param tableBody Body of the table (without header and properties
     *            sections). Its like a source to process <code>ITable</code>
     *            with data.
     * @param tableName Name of the outcome table.
     * @param tableType Type of the data in table.
     * @param bindingContext OpenL context.
     * @param openl OpenL instance.
     * @param hasColumnTitleRow Flag representing if tableBody has title row for
     *            columns.
     * @throws Exception
     */
    public void processTable(XlsModuleOpenClass xlsOpenClass,
            ITable tableToProcess,
            IGridTable tableBody,
            String tableName,
            IOpenClass tableType,
            IBindingContext bindingContext,
            OpenL openl,
            boolean hasColumnTitleRow) throws Exception {

        IGridTable horizDataTableBody = DataTableBindHelper.getHorizontalTable(tableBody, tableType);
        IGridTable descriptorRows = DataTableBindHelper.getDescriptorRows(horizDataTableBody);
        IGridTable dataWithTitleRows = DataTableBindHelper.getDataWithTitleRows(horizDataTableBody);

        dataWithTitleRows = LogicalTableHelper.logicalTable(dataWithTitleRows, descriptorRows, null);

        ColumnDescriptor[] descriptors = DataTableBindHelper.makeDescriptors(tableToProcess,
            tableType,
            openl,
            descriptorRows,
            dataWithTitleRows,
            DataTableBindHelper.hasForeignKeysRow(horizDataTableBody),
            hasColumnTitleRow);

        OpenlBasedDataTableModel dataModel = new OpenlBasedDataTableModel(tableName,
            tableType,
            openl,
            descriptors,
            hasColumnTitleRow);

        OpenlToolAdaptor ota = new OpenlToolAdaptor(openl, bindingContext);

        xlsOpenClass.getDataBase().preLoadTable(tableToProcess, dataModel, dataWithTitleRows, ota);
    }

    /**
     * Adds sub table for displaying on bussiness view.
     * 
     * @param tableSyntaxNode <code>TableSyntaxNode</code> representing table.
     * @param tableType Type of the data in table.
     */
    private void putSubTableForBussinesView(TableSyntaxNode tableSyntaxNode, IOpenClass tableType) {

        IGridTable tableBody = DataTableBindHelper.getTableBody(tableSyntaxNode);
        IGridTable horizDataTable = DataTableBindHelper.getHorizontalTable(tableBody, tableType);
        IGridTable dataWithTitleRows = DataTableBindHelper.getDataWithTitleRows(horizDataTable);

        tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, dataWithTitleRows);
    }

    /**
     * Default method. It is called during processing OpenL module. If you call
     * this method, you want to process table with cell title row set to
     * <code>TRUE</code>. calls
     * {@link #processTable(XlsModuleOpenClass, ITable, IGridTable, String, IOpenClass, IBindingContext, OpenL, boolean)}
     * to populate <code>ITable</code> with data. Also adds to
     * <code>TableSyntaxNode</code> sub table for displaying on bussiness view.
     * 
     * @param xlsOpenClass Open class representing OpenL module.
     * @param tableSyntaxNode <code>TableSyntaxNode</code> to be processed.
     * @param tableName Name of the outcome table.
     * @param tableType Type of the data in table.
     * @param bindingContext OpenL context.
     * @param openl OpenL instance.
     */
    private ITable makeTable(XlsModuleOpenClass xlsOpenClass,
            TableSyntaxNode tableSyntaxNode,
            String tableName,
            IOpenClass tableType,
            IBindingContext bindingContext,
            OpenL openl) throws Exception {

        ITable resultTable = xlsOpenClass.getDataBase().addNewTable(tableName, tableSyntaxNode);
        IGridTable tableBody = DataTableBindHelper.getTableBody(tableSyntaxNode);

        processTable(xlsOpenClass, resultTable, tableBody, tableName, tableType, bindingContext, openl, true);
        putSubTableForBussinesView(tableSyntaxNode, tableType);

        return resultTable;
    }

    /**
     * Checks format of the data table header.
     * 
     * @param source source code
     * @throws error if length of header is less than {@link #HEADER_NUM_TOKENS}
     */
    private void checkParsedHeader(IOpenSourceCodeModule source) throws SyntaxNodeException {

        try {
            parsedHeader = Tokenizer.tokenize(source, " \n\r");
        } catch (OpenLCompilationException e) {
            throw SyntaxNodeExceptionUtils.createError("Cannot parse header", null, null, source);
        }

        if (parsedHeader.length < HEADER_NUM_TOKENS) {
            String message = getFormatErrorMessage();

            throw SyntaxNodeExceptionUtils.createError(message, null, null, source);
        }
    }
}