package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * Binding context for all expressions that are related to some type. All fields
 * specified in the type will be available as variables.
 * 
 * @author PUdalau
 */
public class TypeBindingContext extends BindingContextDelegator {
    private IOpenClass componentType;

    public TypeBindingContext(IBindingContext delegate, IOpenClass componentType) {
        super(delegate);
        this.componentType = componentType;
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = componentType.getField(name, strictMatch);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }
}