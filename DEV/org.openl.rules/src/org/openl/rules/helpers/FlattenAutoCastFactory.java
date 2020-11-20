package org.openl.rules.helpers;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.DefaultAutoCastFactory;
import org.openl.types.IOpenClass;

public class FlattenAutoCastFactory extends DefaultAutoCastFactory {
    @Override
    protected IOpenClass findTypeForVarargs(IOpenClass[] types, IBindingContext bindingContext) {
        if (types == null || types.length == 0) {
            throw new IllegalArgumentException("types cannot be empty or null");
        }
        IOpenClass type = types[0];
        if (type.isArray()) {
            type = type.getComponentClass();
        }
        for (int i = 1; i < types.length; i++) {
            type = bindingContext.findParentClass(type, types[i].isArray() ? types[i].getComponentClass() : types[i]);
        }
        return type;
    }
}
