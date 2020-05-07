/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import static org.openl.rules.utils.TableNameChecker.NAME_ERROR_MESSAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DataTableMetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.rules.testmethod.TestMethodOpenClass;
import org.openl.rules.utils.TableNameChecker;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.MessageUtils;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 *
 */
public class DataNodeBinder extends AXlsTableBinder {

    // indexes of names in header
    public static final int TYPE_INDEX = 1;
    private static final int TABLE_NAME_INDEX = 2;

    protected ATableBoundNode makeNode(TableSyntaxNode tsn,
            XlsModuleOpenClass module,
            RulesModuleBindingContext bindingContext) {
        DataTableBoundNode boundNode = new DataTableBoundNode(tsn, module);

        if (!bindingContext.isExecutionMode()) {
            tsn.setMetaInfoReader(new DataTableMetaInfoReader(boundNode));
        }

        return boundNode;
    }

    protected ILogicalTable getTableBody(TableSyntaxNode tsn) {
        return DataTableBindHelper.getTableBody(tsn);
    }

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {

        DataTableBoundNode dataNode = (DataTableBoundNode) makeNode(tableSyntaxNode, module, bindingContext);
        ILogicalTable table = tableSyntaxNode.getTable();

        IOpenSourceCodeModule source = new GridCellSourceCodeModule(table.getSource(), bindingContext);

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(source, " \n\r");

        if (parsedHeader.length < 3) {
            throw SyntaxNodeExceptionUtils.createError("Data table format: Data <typename> <tablename>", source);
        }

        parsedHeader = mergeArraySimbols(parsedHeader);

        if (parsedHeader.length > 3) {
            throw SyntaxNodeExceptionUtils.createError("Data table format: Data <typename> <tablename>", source);
        }

        String typeName = parsedHeader[TYPE_INDEX].getText();
        String tableName = parsedHeader[TABLE_NAME_INDEX].getText();
        if (TableNameChecker.isInvalidJavaIdentifier(tableName)) {
            String message = "Data table " + tableName + NAME_ERROR_MESSAGE;
            throw SyntaxNodeExceptionUtils.createError(message, null, parsedHeader[TABLE_NAME_INDEX]);
        }
        IOpenClass tableType = RuleRowHelper.getType(typeName, parsedHeader[TYPE_INDEX], bindingContext);

        // Check that table type loaded properly.
        //
        if (tableType.getInstanceClass() == null) {
            String message = MessageUtils.getTypeDefinedErrorMessage(typeName);
            throw SyntaxNodeExceptionUtils.createError(message, parsedHeader[TYPE_INDEX]);
        }

        ITable dataTable = makeTable(module, tableSyntaxNode, tableName, tableType, bindingContext, openl, true);
        dataNode.setTable(dataTable);

        return dataNode;
    }

    private IdentifierNode[] mergeArraySimbols(IdentifierNode[] parsedHeader) {
        List<IdentifierNode> parsedHeader1 = new ArrayList<>();
        parsedHeader1.add(parsedHeader[0]);
        int i = 2;
        StringBuilder sb = new StringBuilder();
        while (i < parsedHeader.length - 1) {
            if ("[]".equals(parsedHeader[i].getIdentifier()) || "]".equals(parsedHeader[i].getIdentifier()) || "["
                .equals(parsedHeader[i].getIdentifier())) {
                sb.append(parsedHeader[i].getIdentifier());
            } else {
                break;
            }
            i++;
        }
        parsedHeader1.add(new IdentifierNode(parsedHeader[1].getType(),
            new TextInterval(parsedHeader[1].getLocation().getStart(), parsedHeader[i - 1].getLocation().getEnd()),
            parsedHeader[1].getIdentifier() + sb.toString(),
            parsedHeader[1].getModule()));
        parsedHeader1.addAll(Arrays.asList(parsedHeader).subList(i, parsedHeader.length));
        return parsedHeader1.toArray(new IdentifierNode[] {});
    }

    /**
     * Populate the <code>ITable</code> with data from <code>ILogicalTable</code>.
     *
     * @param xlsOpenClass Open class representing OpenL module.
     * @param tableToProcess Table to be processed.
     * @param tableBody Body of the table (without header and properties sections). Its like a source to process
     *            <code>ITable</code> with data.
     * @param tableName Name of the outcome table.
     * @param tableType Type of the data in table.
     * @param bindingContext OpenL context.
     * @param openl OpenL instance.
     * @param hasColumnTitleRow Flag representing if tableBody has title row for columns.
     */
    public void processTable(XlsModuleOpenClass xlsOpenClass,
            ITable tableToProcess,
            ILogicalTable tableBody,
            String tableName,
            IOpenClass tableType,
            IBindingContext bindingContext,
            OpenL openl,
            boolean hasColumnTitleRow) throws Exception {

        if (tableBody == null) {
            String message = "There is no body in 'Data' table.";
            throw SyntaxNodeExceptionUtils.createError(message, tableToProcess.getTableSyntaxNode());
        } else {
            ILogicalTable horizDataTableBody = DataTableBindHelper.getHorizontalTable(tableBody, tableType);
            if (horizDataTableBody.getHeight() > 1) {
                ILogicalTable descriptorRows = DataTableBindHelper.getDescriptorRows(horizDataTableBody);
                ILogicalTable dataWithTitleRows = DataTableBindHelper.getHorizontalDataWithTitle(horizDataTableBody);

                dataWithTitleRows = LogicalTableHelper
                    .logicalTable(dataWithTitleRows.getSource(), descriptorRows, null);

                ColumnDescriptor[] descriptors = makeDescriptors(tableToProcess,
                    tableType,
                    bindingContext,
                    openl,
                    hasColumnTitleRow,
                    horizDataTableBody,
                    descriptorRows,
                    dataWithTitleRows);

                if (tableType instanceof TestMethodOpenClass) {
                    validateTestTableDescriptors(descriptors, tableToProcess, bindingContext);
                }

                OpenlBasedDataTableModel dataModel = new OpenlBasedDataTableModel(tableName,
                    tableType,
                    openl,
                    descriptors,
                    hasColumnTitleRow);

                OpenlToolAdaptor ota = new OpenlToolAdaptor(openl, bindingContext, tableToProcess.getTableSyntaxNode());

                xlsOpenClass.getDataBase().preLoadTable(tableToProcess, dataModel, dataWithTitleRows, ota);
            } else {
                String message = "Invalid table structure: data table body should contain key and value columns.";
                throw SyntaxNodeExceptionUtils.createError(message, tableToProcess.getTableSyntaxNode());
            }
        }
    }

    private void validateTestTableDescriptors(ColumnDescriptor[] descriptors,
            ITable tableToProcess,
            IBindingContext bindingContext) throws SyntaxNodeException {
        Set<String> runtimeContextProps = new HashSet<>();
        Set<String> testRuntimeContextProps = new HashSet<>();
        Map<String, String> duplicatedRuntimeContextProps = new HashMap();
        for (ColumnDescriptor descriptor : descriptors) {
            if (descriptor != null) {
                IOpenField field = descriptor.getField();
                if (field instanceof FieldChain) {
                    IOpenField[] fields = ((FieldChain) field).getFields();
                    // for fields with a context property, the length must be 2
                    if (fields.length != 2) {
                        continue;
                    }
                    if (fields[1].isContextProperty()) {
                        String contextProperty = fields[1].getContextProperty();
                        if (runtimeContextProps.contains(contextProperty)) {
                            duplicatedRuntimeContextProps.put(contextProperty, fields[0].getName());
                        }
                        if (testRuntimeContextProps.contains(contextProperty)) {
                            throw SyntaxNodeExceptionUtils.createError(String.format(
                                "'%s' is redundant since this field is already defined in '%s' parameter.",
                                TestMethodHelper.CONTEXT_NAME + "." + contextProperty,
                                fields[0].getName()), tableToProcess.getTableSyntaxNode());
                        }
                        runtimeContextProps.add(contextProperty);
                    } else if (fields[0].getName().equals(TestMethodHelper.CONTEXT_NAME)) {
                        testRuntimeContextProps.add(fields[1].getName());
                    }
                }
            }
        }
        for (String prop : duplicatedRuntimeContextProps.keySet()) {
            bindingContext.addMessage(new OpenLWarnMessage(
                String.format("Multiple fields refer to the same context property '%s'. '%s.%s' will be applied.",
                    prop,
                    duplicatedRuntimeContextProps.get(prop),
                    prop),
                tableToProcess.getTableSyntaxNode()));
        }
    }

    protected ColumnDescriptor[] makeDescriptors(ITable tableToProcess,
            IOpenClass tableType,
            IBindingContext bindingContext,
            OpenL openl,
            boolean hasColumnTitleRow,
            ILogicalTable horizDataTableBody,
            ILogicalTable descriptorRows,
            ILogicalTable dataWithTitleRows) throws Exception {
        return DataTableBindHelper.makeDescriptors(bindingContext,
            tableToProcess,
            tableType,
            openl,
            descriptorRows,
            dataWithTitleRows,
            DataTableBindHelper.hasForeignKeysRow(horizDataTableBody),
            hasColumnTitleRow,
            true);
    }

    /**
     * Adds sub table for displaying on bussiness view.
     *
     * @param tableSyntaxNode <code>TableSyntaxNode</code> representing table.
     * @param tableType Type of the data in table.
     */
    public static void putSubTableForBusinessView(TableSyntaxNode tableSyntaxNode, IOpenClass tableType) {

        ILogicalTable tableBody = DataTableBindHelper.getTableBody(tableSyntaxNode);
        ILogicalTable dataWithTitle = DataTableBindHelper.getSubTableForBusinessView(tableBody, tableType);

        tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, dataWithTitle);
    }

    /**
     * Default method. It is called during processing OpenL module. If you call this method, you want to process table
     * with cell title row set to <code>TRUE</code>. calls
     * {@link #processTable(XlsModuleOpenClass, ITable, ILogicalTable, String, IOpenClass, IBindingContext, OpenL, boolean)}
     * to populate <code>ITable</code> with data. Also adds to <code>TableSyntaxNode</code> sub table for displaying on
     * business view.
     *
     * @param xlsOpenClass Open class representing OpenL module.
     * @param tableSyntaxNode <code>TableSyntaxNode</code> to be processed.
     * @param tableName Name of the outcome table.
     * @param tableType Type of the data in table.
     * @param bindingContext OpenL context.
     * @param openl OpenL instance.
     */
    protected ITable makeTable(XlsModuleOpenClass xlsOpenClass,
            TableSyntaxNode tableSyntaxNode,
            String tableName,
            IOpenClass tableType,
            IBindingContext bindingContext,
            OpenL openl,
            boolean useRegistered) throws Exception {

        ITable resultTable;
        if (useRegistered) {
            resultTable = xlsOpenClass.getDataBase().registerTable(tableName, tableSyntaxNode);
        } else {
            resultTable = xlsOpenClass.getDataBase().registerNewTable(tableName, tableSyntaxNode);
        }
        ILogicalTable tableBody = DataTableBindHelper.getTableBody(tableSyntaxNode);

        processTable(xlsOpenClass, resultTable, tableBody, tableName, tableType, bindingContext, openl, true);
        putSubTableForBusinessView(tableSyntaxNode, tableType);

        return resultTable;
    }

}
