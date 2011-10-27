/**
 * Created Jan 2, 2007
 */
package org.openl.rules.testmethod;

import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.DataTableBoundNode;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodsHelper;

/**
 * @author snshor
 */
public class TestMethodNodeBinder extends DataNodeBinder {

    private static final String FORMAT_ERROR_MESSAGE = "Testmethod table format: Testmethod <methodname> <testname>";

    @Override
    protected String getFormatErrorMessage() {
        return FORMAT_ERROR_MESSAGE;
    }
    
    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tableSyntaxNode, OpenL openl, IBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {
        if (bindingContext.isExecutionMode()) {
            return null;//skipped in execution mode
        } else {
            return super.preBind(tableSyntaxNode, openl, bindingContext, module);
        }
    }

    @Override
    protected ATableBoundNode makeNode(TableSyntaxNode tableSyntaxNode, XlsModuleOpenClass module) {
        return new TestMethodBoundNode(tableSyntaxNode, module);
    }

    @Override
    protected synchronized IOpenClass getTableType(String typeName,
            IBindingContext bindingContext,
            XlsModuleOpenClass module,
            DataTableBoundNode dataNode,
            String tableName, TableSyntaxNode tsn) {

        TestMethodBoundNode testMethodBoundNode = (TestMethodBoundNode) dataNode;
        IOpenMethod testedMethod = MethodsHelper.getSingleMethod(typeName, module.getMethods());
        IOpenMethodHeader header = TestMethodHelper.makeHeader(tableName, module);
        TestSuiteMethod testSuite = new TestSuiteMethod(tableName, testedMethod, header, testMethodBoundNode);
        testMethodBoundNode.setTestSuite(testSuite);
        
        ILogicalTable horiztableBody = DataTableBindHelper.getTableBody(tsn);
        ILogicalTable descriptorRows = DataTableBindHelper.getDescriptorRows(horiztableBody);
        
        List<IdentifierNode[]> columnIdentifiers = null;
        try {
            columnIdentifiers = DataTableBindHelper.getColumnIdentifiers(null, null, descriptorRows);
        } catch (OpenLCompilationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return testSuite.getMethodBasedClass(columnIdentifiers);
    }

}
