/*
  Created Jan 2, 2007
 */
package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import org.openl.util.TableNameChecker;
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

    private static class TestedMethodBindingDetails {
        TestMethodBoundNode testMethodBoundNode = null;
        IOpenMethod testedMethod = null;
        TestMethodOpenClass testMethodOpenClass = null;
        ITable dataTable = null;
        Collection<OpenLMessage> messages = null;
        List<SyntaxNodeException> errors = null;
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

        TestedMethodBindingDetails best = null;
        boolean hasNoErrorBinding = false;
        List<TestedMethodBindingDetails> noErrorsCases = null;
        for (IOpenMethod testedMethod : module.getMethods()) {
            if (!methodName.equals(testedMethod.getName())) {
                continue;
            }
            TestedMethodBindingDetails current = new TestedMethodBindingDetails();
            current.testedMethod = testedMethod;
            current.testMethodBoundNode = (TestMethodBoundNode) makeNode(tableSyntaxNode, module, bindingContext);
            TestSuiteMethod testSuite = new TestSuiteMethod(testedMethod, header, current.testMethodBoundNode);
            current.testMethodBoundNode.setTestSuite(testSuite);
            current.testMethodOpenClass = new TestMethodOpenClass(tableName, testedMethod);

            // Check that table type loaded properly.
            //
            if (current.testMethodOpenClass.getInstanceClass() == null) {
                String message = String.format("Table '%s' is defined with errors.", methodName);
                throw SyntaxNodeExceptionUtils.createError(message, parsedHeader[TESTED_METHOD_INDEX]);
            }
            bindingContext.pushErrors();
            bindingContext.pushMessages();
            try {
                current.dataTable = makeTable(module,
                    tableSyntaxNode,
                    tableName,
                    current.testMethodOpenClass,
                    bindingContext,
                    openl,
                    false);
            } finally {
                current.errors = bindingContext.popErrors();
                if (current.errors == null) {
                    current.errors = Collections.emptyList();
                }
                current.messages = bindingContext.popMessages();
                if (current.messages == null) {
                    current.messages = Collections.emptyList();
                }
            }
            current.testMethodBoundNode.setTable(current.dataTable);

            if (!current.errors.isEmpty() && (best == null || best.errors.size() > current.errors.size())) {
                best = current;
            } else if (current.errors.isEmpty()) {
                if (!hasNoErrorBinding) {
                    hasNoErrorBinding = true;
                    best = current;
                } else {
                    if (noErrorsCases == null) {
                        noErrorsCases = new ArrayList<>();
                        noErrorsCases.add(best);
                    }
                    noErrorsCases.add(current);
                }
            }
        }

        if (noErrorsCases != null && noErrorsCases.size() > 1) {
            List<TestedMethodBindingDetails> exactMatches = new ArrayList<>();
            for (TestedMethodBindingDetails noErrorCase : noErrorsCases) {
                int c = 0;
                for (int i = 0; i < noErrorCase.testedMethod.getSignature().getNumberOfParameters(); i++) {
                    String parameterName = noErrorCase.testedMethod.getSignature().getParameterName(i);
                    for (int j = 0; j < noErrorCase.dataTable.getNumberOfColumns(); j++) {
                        String columnFieldName = noErrorCase.dataTable.getColumnDescriptor(j).getName();
                        if (Objects.equals(columnFieldName, parameterName)) {
                            c++;
                            break;
                        }
                    }
                }
                if (c == noErrorCase.testedMethod.getSignature().getNumberOfParameters()) {
                    exactMatches.add(noErrorCase);
                }
            }
            if (exactMatches.isEmpty()) {
                throw new AmbiguousMethodException(methodName,
                    noErrorsCases.stream().map(e -> e.testedMethod).collect(Collectors.toList()));
            }
            if (exactMatches.size() > 1) {
                throw new AmbiguousMethodException(methodName,
                    exactMatches.stream().map(e -> e.testedMethod).collect(Collectors.toList()));
            } else {
                best = exactMatches.iterator().next();
            }
        }

        if (best != null) {
            best.testMethodBoundNode.setTable(best.dataTable);
            DataNodeBinder.putSubTableForBusinessView(tableSyntaxNode, best.testMethodOpenClass);
            best.messages.forEach(bindingContext::addMessage);
            best.errors.forEach(bindingContext::addError);
            return best.testMethodBoundNode;
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
