package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Binder for conditions table.
 * 
 * @author Marat Kamalov
 * 
 */
public class ConditionsTableBinder extends ADtColumnsDefinitionTableBinder {
    
    private static final String DEFAULT_TABLE_NAME_PREFIX = "Conditions: ";
    
    public ConditionsTableBinder() {
        super(DEFAULT_TABLE_NAME_PREFIX);
    }
    
    protected ADtColumnsDefinitionTableBoundNode makeNode(TableSyntaxNode tsn,
            XlsModuleOpenClass module,
            OpenL openl,
            IBindingContext bindingContext) {
        ConditionsTableBoundNode boundNode = new ConditionsTableBoundNode(tsn, openl);
        return boundNode;
    }

}