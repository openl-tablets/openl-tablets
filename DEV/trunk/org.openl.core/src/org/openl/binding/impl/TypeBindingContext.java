package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.impl.module.RootDictionaryContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenField;

/**
 * Binding context for all expressions that are related to some type. All fields
 * specified in the type will be available as variables.
 * 
 * @author PUdalau
 */
public class TypeBindingContext extends BindingContextDelegator {
    private RootDictionaryContext context;

    public TypeBindingContext(IBindingContext delegate, ILocalVar localVar) {
        super(delegate);
        context = new RootDictionaryContext(new IOpenField[]{localVar}, 1);
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = context.findField(name);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }
}