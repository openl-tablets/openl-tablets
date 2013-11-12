package org.openl.rules.helpers.scope;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;

/**
 * @deprecated 12.11.2010 what is it for?
 * @author DLiauchuk
 *
 */
@Deprecated
public class Scope extends ModuleOpenClass {

    ModuleOpenClass parentScope;

    public Scope(String name, ModuleOpenClass parentScope) {
        super(parentScope.getSchema(), name, parentScope.getOpenl());
        this.parentScope = parentScope;
    }

    @Override
    public IBindingContext makeBindingContext(IBindingContext topLevelContext) {
        IBindingContext parentContext = parentScope.makeBindingContext(topLevelContext);

        return super.makeBindingContext(parentContext);
    }

}
