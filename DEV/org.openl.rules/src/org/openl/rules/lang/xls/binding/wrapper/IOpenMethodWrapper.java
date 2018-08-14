package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public interface IOpenMethodWrapper {
    IOpenMethod getDelegate();
    
    XlsModuleOpenClass getXlsModuleOpenClass();
    
    IOpenMethod getTopOpenClassMethod(IOpenClass openClass);
}
