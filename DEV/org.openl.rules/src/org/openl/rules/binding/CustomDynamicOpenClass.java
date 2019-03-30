package org.openl.rules.binding;

import org.openl.types.IOpenClass;

public interface CustomDynamicOpenClass {
    IOpenClass copy();

    void updateOpenClass(IOpenClass openClass);
}
