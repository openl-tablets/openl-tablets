package org.openl.types.impl;

import org.openl.binding.impl.module.ModuleOpenClass;

public interface BelongsToModuleOpenClass {
    String getExternalRefName();

    ModuleOpenClass getModule();
}
