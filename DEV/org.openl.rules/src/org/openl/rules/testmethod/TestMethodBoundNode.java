package org.openl.rules.testmethod;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.DataTableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.impl.DynamicObject;

/**
 * @author snshor
 */
public class TestMethodBoundNode extends DataTableBoundNode {

    private TestSuiteMethod testSuiteMethod;

    public TestMethodBoundNode(TableSyntaxNode tableSyntaxNode, XlsModuleOpenClass module) {
        super(tableSyntaxNode, module);
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        super.addTo(openClass);
        testSuiteMethod.setModuleName(getModule().getModuleName());
        openClass.addMethod(testSuiteMethod);
    }

    protected void setTestSuite(TestSuiteMethod testSuiteMethod) {
        this.testSuiteMethod = testSuiteMethod;
    }

    @Override
    public void finalizeBind(IBindingContext cxt) throws Exception {
        super.finalizeBind(cxt);

        var testCases = (DynamicObject[]) getField().getData();
        for (DynamicObject testCase : testCases) {
            if (testCase.getFieldValue(TestMethodHelper.EXPECTED_ERROR) != null && testCase
                    .getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME) != null) {
                var table = getTable();
                for (var i = 0; i < table.getNumberOfColumns(); i++) {
                    var descriptor = table.getColumnDescriptor(i);
                    var identifiers = descriptor.getFieldChainTokens();
                    if (identifiers.length > 0 && TestMethodHelper.EXPECTED_ERROR
                            .equals(identifiers[0].getIdentifier()) && descriptor.getColumnValue(testCase) != null) {
                        var row = table.getRowIndex(testCase);
                        var column = table.getColumnIndex(descriptor.getDisplayName());
                        var cell = table.getRowTable(row).getColumn(column);
                        var cellSourceCodeModule = new GridCellSourceCodeModule(cell, cxt);

                        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                                "Ambiguous expectation in the test case. Both expected result and expected error have been declared.",
                                cellSourceCodeModule);
                        cxt.addError(error);
                    }
                }
            }
        }
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        super.removeDebugInformation(cxt);
        testSuiteMethod.clearForExecutionMode();
    }
}
