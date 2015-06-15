package org.openl.rules.lang.xls.prebind;

import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public interface LazyWrapper {
    IOpenMethod getCompiledMethod(IRuntimeEnv env);
}
