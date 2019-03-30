package org.openl.rules.constants;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;

/**
 * Binder for constants table.
 * 
 * @author Marat Kamalov
 * 
 */
public class ConstantsTableBinder extends AXlsTableBinder {

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            IBindingContext cxt,
            XlsModuleOpenClass module) throws Exception {

        assert cxt instanceof RulesModuleBindingContext;

        ILogicalTable table = tsn.getTable();

        return new ConstantsTableBoundNode(tsn, module, table, openl);
    }

}
