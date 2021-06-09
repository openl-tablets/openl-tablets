package org.openl.rules.lang.xls.binding;

import org.openl.types.IOpenClass;

public interface ModuleSpecificType {
    IOpenClass makeCopyForModule(XlsModuleOpenClass module);

    void updateWithType(IOpenClass openClass);
}
