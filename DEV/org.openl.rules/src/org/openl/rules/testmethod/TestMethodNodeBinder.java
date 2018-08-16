/*
  Created Jan 2, 2007
 */
package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
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
import org.openl.util.CollectionUtils;

/**
 * @author snshor
 */
public class TestMethodNodeBinder extends DataNodeBinder {

    // indexes of names in header
    private static final int TESTED_METHOD_INDEX = 1;
    private static final int TABLE_NAME_INDEX = 2;
    private static final AtomicInteger counter = new AtomicInteger();
    /**
     * Workaround for ability to run tests when executionMode is true.
     * In future we should remove this field and replace executionMode with a type with 3 possible states: execute only
     * business rules, execute rules and tests, keep all tables and meta info for edit in WebStudio.
     */
    private static ThreadLocal<Boolean> keepTests = new ThreadLocal<>();

    public static boolean isKeepTestsInExecutionMode() {
        Boolean keep = keepTests.get();
        return keep != null && keep;
    }

    public static void keepTestsInExecutionMode() {
        keepTests.set(Boolean.TRUE);
    }

    public static void removeTestsInExecutionMode() {
        keepTests.remove();
    }


    @Override
    protected ATableBoundNode makeNode(TableSyntaxNode tableSyntaxNode, XlsModuleOpenClass module) {
        return new TestMethodBoundNode(tableSyntaxNode, module);
    }

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {
        if (bindingContext.isExecutionMode() && !isKeepTestsInExecutionMode()) {
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

        List<IOpenMethod> testedMethods = CollectionUtils.findAll(module.getMethods(),
            new CollectionUtils.Predicate<IOpenMethod>() {
                @Override
                public boolean evaluate(IOpenMethod method) {
                    return methodName.equals(method.getName());
                }
            });
        if (testedMethods.isEmpty()) {
            throw new MethodNotFoundException(null, methodName, IOpenClass.EMPTY);
        }

        IOpenMethodHeader header = new OpenMethodHeader(tableName,
            JavaOpenClass.getOpenClass(TestUnitsResults.class),
            IMethodSignature.VOID,
            module);

        int i = 0;
        TestMethodBoundNode bestCaseTestMethodBoundNode = null;
        IOpenMethod bestCaseOpenMethod = null;
        SyntaxNodeException[] bestCaseErrors = null;
        TestMethodOpenClass bestTestMethodOpenClass = null;

        boolean hasNoErrorBinding = false;
        List<OpenLMessage> messages = OpenLMessages.getCurrentInstance().getMessages();
        SyntaxNodeException[] errors = tableSyntaxNode.getErrors();
        for (IOpenMethod testedMethod : testedMethods) {
            OpenLMessages.getCurrentInstance().clear();
            for (OpenLMessage message : messages) {
                OpenLMessages.getCurrentInstance().addMessage(message);
            }
            tableSyntaxNode.clearErrors();
            if (errors != null) {
                for (SyntaxNodeException error : errors) {
                    tableSyntaxNode.addError(error);
                }
            }
            TestMethodBoundNode testMethodBoundNode = (TestMethodBoundNode) makeNode(tableSyntaxNode, module);
            TestSuiteMethod testSuite = new TestSuiteMethod(testedMethod, header, testMethodBoundNode);
            testMethodBoundNode.setTestSuite(testSuite);
            TestMethodOpenClass testMethodOpenClass = new TestMethodOpenClass(tableName, testedMethod);

            // Check that table type loaded properly.
            //
            if (testMethodOpenClass.getInstanceClass() == null) {
                String message = String.format("Table '%s' was defined with errors", methodName);
                throw SyntaxNodeExceptionUtils.createError(message, parsedHeader[TESTED_METHOD_INDEX]);
            }
            try {
                ITable dataTable = makeTable(module,
                    tableSyntaxNode,
                    tableName,
                    testMethodOpenClass,
                    bindingContext,
                    openl);
                testMethodBoundNode.setTable(dataTable);
                if (testMethodBoundNode.getTableSyntaxNode()
                    .hasErrors() && (bestCaseErrors == null || bestCaseErrors.length > testMethodBoundNode
                        .getTableSyntaxNode().getErrors().length)) {
                    bestCaseErrors = testMethodBoundNode.getTableSyntaxNode().getErrors();
                    bestCaseTestMethodBoundNode = testMethodBoundNode;
                    bestCaseOpenMethod = testedMethod;
                    bestTestMethodOpenClass = testMethodOpenClass;
                } else {
                    if (!testMethodBoundNode.getTableSyntaxNode().hasErrors()) {
                        if (!hasNoErrorBinding) {
                            bestCaseTestMethodBoundNode = testMethodBoundNode;
                            bestCaseOpenMethod = testedMethod;
                            bestTestMethodOpenClass = testMethodOpenClass;
                            hasNoErrorBinding = true;
                        } else {
                            List<IOpenMethod> list = new ArrayList<IOpenMethod>();
                            list.add(testedMethod);
                            list.add(bestCaseOpenMethod);
                            throw new AmbiguousMethodException(tableName, IOpenClass.EMPTY, list);
                        }
                        bestCaseErrors = new SyntaxNodeException[0];
                    }
                }
            } catch (AmbiguousMethodException e) {
                throw e;
            } catch (Exception e) {
                if (i < testedMethods.size() - 1) {
                    continue;
                }
                throw e;
            }
        }

        if (bestCaseTestMethodBoundNode != null) {
            if (bindingContext.isExecutionMode() && ((TableSyntaxNode) bestCaseTestMethodBoundNode.getSyntaxNode()).hasErrors()) {
                // In execution mode we don't need test tables that can't be run because of errors (even if isKeepTestsInExecutionMode() == true)
                return null;
            }
            tableSyntaxNode.clearErrors();
            OpenLMessages.getCurrentInstance().clear();
            for (OpenLMessage message : messages) {
                OpenLMessages.getCurrentInstance().addMessage(message);
            }
            if (errors != null) {
                for (SyntaxNodeException error : errors) {
                    tableSyntaxNode.addError(error);
                }
            }

            ITable dataTable = makeTable(module,
                tableSyntaxNode,
                tableName,
                bestTestMethodOpenClass,
                bindingContext,
                openl);
            bestCaseTestMethodBoundNode.setTable(dataTable);

            return bestCaseTestMethodBoundNode;
        }

        String message = String.format("Table '%s' is not found", methodName);
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
