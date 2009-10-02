/**
 * Created Jan 2, 2007
 */
package org.openl.rules.testmethod.binding;

import org.openl.binding.IBindingContext;
import org.openl.rules.data.binding.DataNodeBinder;
import org.openl.rules.data.binding.DataTableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;

/**
 * @author snshor
 *
 */
public class TestMethodNodeBinder extends DataNodeBinder {

    // TestMethodHelper tmNode = null;

    /**
     *
     */
    public TestMethodNodeBinder() {
        super();
    }

    @Override
    protected String getErrMsgFormat() {
        return "Testmethod table format: Testmethod <methodname> <testname>";
    }

    @Override
    protected synchronized IOpenClass getTableType(String typeName, IBindingContext cxt, XlsModuleOpenClass module,
            DataTableBoundNode dataNode, String tableName) {

        TestMethodHelper tmNode = ((TestMethodBoundNode) dataNode).getTmhelper();

        if (tmNode == null) {
            IOpenMethod m = AOpenClass.getSingleMethod(typeName, module.methods());

            tmNode = new TestMethodHelper(m, tableName);

            ((TestMethodBoundNode) dataNode).setTmhelper(tmNode);
        }

        return tmNode.getMethodBasedClass();
    }

    @Override
    protected DataTableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module) {
        return new TestMethodBoundNode(tsn, module);
    }

}
