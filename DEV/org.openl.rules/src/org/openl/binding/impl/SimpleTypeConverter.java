package org.openl.binding.impl;

import org.openl.base.INamedThing;
import org.openl.types.IOpenClass;
import org.openl.types.impl.OpenClassDelegator;
import org.openl.util.IConvertor;

/**
 * Returns short name of generic types and full name of alias types
 */
public class SimpleTypeConverter implements IConvertor<IOpenClass, String> {
    @Override
    public String convert(IOpenClass type) {
        return type instanceof OpenClassDelegator ? type.getName() : type.getDisplayName(INamedThing.SHORT);
    }
}
