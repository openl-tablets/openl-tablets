package org.openl.rules.lang.xls.prebind;

import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public interface LazyMethodWrapper {
    IOpenMethod getCompiledMethod(IRuntimeEnv env);
}
