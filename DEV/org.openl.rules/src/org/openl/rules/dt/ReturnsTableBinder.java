package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Binder for returns table.
 * 
 * @author Marat Kamalov
 * 
 */
public class ReturnsTableBinder extends ADtColumnsDefinitionTableBinder {

    private static final String DEFAULT_TABLE_NAME_PREFIX = "Returns: ";

    public ReturnsTableBinder() {
        super(DEFAULT_TABLE_NAME_PREFIX);
    }

    @Override
    protected ADtColumnsDefinitionTableBoundNode makeNode(TableSyntaxNode tsn,
            XlsModuleOpenClass module,
            OpenL openl,
            IBindingContext bindingContext) {
        return new ReturnsTableBoundNode(tsn, openl);
    }

}