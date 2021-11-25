package org.openl.binding.impl.module;

import org.openl.types.IOpenClass;

public interface ModuleSpecificType {

    ModuleOpenClass getModule();

    IOpenClass convertToModuleTypeAndRegister(ModuleOpenClass module);

    void updateWithType(IOpenClass openClass);
}
