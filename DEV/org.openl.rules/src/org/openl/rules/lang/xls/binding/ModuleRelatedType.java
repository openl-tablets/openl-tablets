package org.openl.rules.lang.xls.binding;

import org.openl.types.IOpenClass;

public interface ModuleRelatedType {
    IOpenClass makeCopyForModule(XlsModuleOpenClass module);

    void extendWith(IOpenClass openClass);
}
