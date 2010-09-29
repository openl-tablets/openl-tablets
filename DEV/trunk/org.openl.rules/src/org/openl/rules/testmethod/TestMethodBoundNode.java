/**
 * Created Jan 3, 2007
 */
package org.openl.rules.testmethod;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.DataTableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

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
        if (cxt.isExecutionMode()) {
            testSuiteMethod.setBoundNode(null);
        }
    }
    
}
