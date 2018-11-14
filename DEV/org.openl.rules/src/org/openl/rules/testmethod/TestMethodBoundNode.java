package org.openl.rules.testmethod;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.DataTableBoundNode;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.impl.DynamicObject;

/**
 * @author snshor
 * 
 */
public class TestMethodBoundNode extends DataTableBoundNode {

    private TestSuiteMethod testSuiteMethod;
    
    public TestMethodBoundNode(TableSyntaxNode tableSyntaxNode, XlsModuleOpenClass module) {
        super(tableSyntaxNode, module);
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        super.addTo(openClass);

        openClass.addMethod(testSuiteMethod);
    }

    protected void setTestSuite(TestSuiteMethod testSuiteMethod) {
        this.testSuiteMethod = testSuiteMethod;
    }
    
    @Override
    public void finalizeBind(IBindingContext cxt) throws Exception {
        super.finalizeBind(cxt);

        DynamicObject[] testCases = (DynamicObject[]) getField().getData();
        for (DynamicObject testCase : testCases) {
            if (testCase.getFieldValue(TestMethodHelper.EXPECTED_ERROR) != null
                    && testCase.getFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME) != null) {
                ITable table = getTable();
                int row = table.getRowIndex(testCase);
                int column = table.getColumnIndex(TestMethodHelper.EXPECTED_ERROR);
                IGridTable cell = table.getRowTable(row).getColumn(column);
                IOpenSourceCodeModule cellSourceCodeModule = new GridCellSourceCodeModule(cell, cxt);

                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                        "Ambiguous expectation in the test case. Both expected result and expected error have been declared.",
                        cellSourceCodeModule);
                getTableSyntaxNode().addError(error);
                cxt.addError(error);
            }
        }
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        super.removeDebugInformation(cxt);
        if (!TestMethodNodeBinder.isKeepTestsInExecutionMode()) {
            testSuiteMethod.setBoundNode(null);
        }
    }
}
