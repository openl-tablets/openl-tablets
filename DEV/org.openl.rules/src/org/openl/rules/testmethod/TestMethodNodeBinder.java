/*
  Created Jan 2, 2007
 */
package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DataTableMetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.TableNameChecker;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.MessageUtils;

/**
 * @author snshor
 */
public class TestMethodNodeBinder extends DataNodeBinder {

    // indexes of names in header
    private static final int TESTED_METHOD_INDEX = 1;
    private static final int TABLE_NAME_INDEX = 2;
    private static final AtomicInteger counter = new AtomicInteger();

    @Override
    protected ATableBoundNode makeNode(TableSyntaxNode tableSyntaxNode,
            XlsModuleOpenClass module,
            RulesModuleBindingContext bindingContext) {
        TestMethodBoundNode boundNode = new TestMethodBoundNode(tableSyntaxNode, module);

        if (!bindingContext.isExecutionMode()) {
            tableSyntaxNode.setMetaInfoReader(new DataTableMetaInfoReader(boundNode));
        }

        return boundNode;
    }

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {
        if (bindingContext.isExecutionMode()) {
            return null;// skipped in execution mode
        }

        ILogicalTable table = tableSyntaxNode.getTable();

        IOpenSourceCodeModule source = new GridCellSourceCodeModule(table.getSource(), bindingContext);

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(source, " \n\r");

        if (parsedHeader.length < 2) {
            throw SyntaxNodeExceptionUtils.createError("Test table format: Test <methodname> <testname>", source);
        }

        final String methodName = parsedHeader[TESTED_METHOD_INDEX].getIdentifier();
        String tableName;
        if (parsedHeader.length == 2) {
            // $Test$0 or $Run$0
            tableName = methodName + "$" + parsedHeader[0].getIdentifier() + "$" + counter.getAndIncrement();
        } else {
            tableName = parsedHeader[TABLE_NAME_INDEX].getIdentifier();
        }
        if (TableNameChecker.isInvalidJavaIdentifier(tableName)) {
            String message = "Test table " + tableName + TableNameChecker.NAME_ERROR_MESSAGE;
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(message, parsedHeader[TABLE_NAME_INDEX]));
        }

        IOpenMethodHeader header = new OpenMethodHeader(tableName,
            JavaOpenClass.getOpenClass(TestUnitsResults.class),
            IMethodSignature.VOID,
            module);

        TestMethodBoundNode bestCaseTestMethodBoundNode = null;
        IOpenMethod bestCaseOpenMethod = null;
        List<SyntaxNodeException> bestCaseErrors = null;
        TestMethodOpenClass bestTestMethodOpenClass = null;
        ITable bestDataTable = null;

        boolean hasNoErrorBinding = false;
        Collection<OpenLMessage> bestMessages = null;
        List<SyntaxNodeException> bestBindingContextErrors = null;

        for (IOpenMethod testedMethod : module.getMethods()) {
            if (!methodName.equals(testedMethod.getName())) {
                continue;
            }
            TestMethodBoundNode testMethodBoundNode = (TestMethodBoundNode) makeNode(tableSyntaxNode,
                module,
                bindingContext);
            TestSuiteMethod testSuite = new TestSuiteMethod(testedMethod, header, testMethodBoundNode);
            testMethodBoundNode.setTestSuite(testSuite);
            TestMethodOpenClass testMethodOpenClass = new TestMethodOpenClass(tableName, testedMethod);

            // Check that table type loaded properly.
            //
            if (testMethodOpenClass.getInstanceClass() == null) {
                String message = String.format("Table '%s' was defined with errors", methodName);
                throw SyntaxNodeExceptionUtils.createError(message, parsedHeader[TESTED_METHOD_INDEX]);
            }
            bindingContext.pushErrors();
            bindingContext.pushMessages();
            ITable dataTable;
            Collection<OpenLMessage> messages;
            List<SyntaxNodeException> errors1;
            try {
                dataTable = makeTable(module,
                    tableSyntaxNode,
                    tableName,
                    testMethodOpenClass,
                    bindingContext,
                    openl,
                    false);
            } finally {
                errors1 = bindingContext.popErrors();
                messages = bindingContext.popMessages();
            }
            testMethodBoundNode.setTable(dataTable);
            if (!errors1.isEmpty() && (bestCaseErrors == null || bestCaseErrors.size() > errors1.size())) {
                bestCaseErrors = errors1;
                bestCaseTestMethodBoundNode = testMethodBoundNode;
                bestCaseOpenMethod = testedMethod;
                bestTestMethodOpenClass = testMethodOpenClass;
                bestDataTable = dataTable;
                bestMessages = messages;
                bestBindingContextErrors = errors1;
            } else if (errors1.isEmpty()) {
                if (!hasNoErrorBinding) {
                    bestCaseTestMethodBoundNode = testMethodBoundNode;
                    bestCaseOpenMethod = testedMethod;
                    bestTestMethodOpenClass = testMethodOpenClass;
                    hasNoErrorBinding = true;
                    bestDataTable = dataTable;
                    bestMessages = messages;
                    bestBindingContextErrors = errors1;
                } else {
                    List<IOpenMethod> list = new ArrayList<>();
                    list.add(testedMethod);
                    list.add(bestCaseOpenMethod);
                    throw new AmbiguousMethodException(tableName, IOpenClass.EMPTY, list);
                }
                bestCaseErrors = Collections.emptyList();

            }

        }

        if (bestCaseTestMethodBoundNode != null) {
            bestCaseTestMethodBoundNode.setTable(bestDataTable);

            DataNodeBinder.putSubTableForBusinessView(tableSyntaxNode, bestTestMethodOpenClass);

            if (bestMessages != null) {
                bestMessages.forEach(bindingContext::addMessage);
            }
            bestBindingContextErrors.forEach(bindingContext::addError);
            return bestCaseTestMethodBoundNode;
        }

        String message = MessageUtils.getTableNotFoundErrorMessage(methodName);
        throw SyntaxNodeExceptionUtils.createError(message, parsedHeader[TESTED_METHOD_INDEX]);
    }

    @Override
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
            false);
    }

}
