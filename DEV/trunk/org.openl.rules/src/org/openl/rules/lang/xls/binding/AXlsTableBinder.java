/*
 * Created on Oct 3, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 * 
 */
public abstract class AXlsTableBinder extends ANodeBinder {

    public static final String PROPERTIES_HEADER = "properties";

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return null;
    }

    public ILogicalTable getPropertiesTableSection(ILogicalTable table) {

        if (table.getLogicalHeight() < 2) {
            return null;
        }

        ILogicalTable propTable = table.rows(1, 1);
        String header = propTable.getGridTable().getCell(0, 0).getStringValue();
        
        if (!PROPERTIES_HEADER.equals(header)) {
            return null;
        }

        return propTable.columns(1);
    }

    public abstract IMemberBoundNode preBind(TableSyntaxNode syntaxNode,
                                             OpenL openl,
                                             IBindingContext cxt,
                                             XlsModuleOpenClass module) throws Exception;
}
