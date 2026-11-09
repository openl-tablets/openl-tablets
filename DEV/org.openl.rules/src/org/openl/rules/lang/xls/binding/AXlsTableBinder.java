/*
 * Created on Oct 3, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import org.jspecify.annotations.Nullable;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 */
public abstract class AXlsTableBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    @Override
    public @Nullable IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return null;
    }

    // Implemented by every table binder, whose binding steps throw many different checked exceptions.
    @SuppressWarnings("java:S112")
    public abstract IMemberBoundNode preBind(TableSyntaxNode syntaxNode,
                                             OpenL openl,
                                             RulesModuleBindingContext bindingContext,
                                             XlsModuleOpenClass module) throws Exception;

    /**
     * Whether a table of this binder can be bound alone against a module compiled before, leaving the module as it
     * was: what the binder defines goes to the binding context, not straight into the module.
     *
     * @return {@code false} for a binder that writes into the module as it binds
     */
    public boolean bindsAlone() {
        return true;
    }
}
