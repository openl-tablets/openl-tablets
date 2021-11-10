package org.openl.binding.impl.module;

import org.openl.types.IOpenClass;

public interface ModuleSpecificType {
    IOpenClass makeCopyForModule(ModuleOpenClass module);

    void updateWithType(IOpenClass openClass);
}
