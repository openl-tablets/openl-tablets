package org.openl.rules.helpers;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.DefaultMethodCallerWrapperFactory;
import org.openl.types.IOpenClass;

public class FlattenMethodCallerWrapperFactory extends DefaultMethodCallerWrapperFactory {
    @Override
    protected IOpenClass findTypeForVarargs(IOpenClass[] types, ICastFactory castFactory) {
        if (types == null || types.length == 0) {
            throw new IllegalArgumentException("types cannot be empty or null");
        }
        IOpenClass type = findBaseComponent(types[0]);
        for (int i = 1; i < types.length; i++) {
            type = castFactory.findParentClass(type, findBaseComponent(types[i]));
        }
        return type;
    }

    private IOpenClass findBaseComponent(IOpenClass type) {
        return type.isArray() ? findBaseComponent(type.getComponentClass()) : type;
    }
}
