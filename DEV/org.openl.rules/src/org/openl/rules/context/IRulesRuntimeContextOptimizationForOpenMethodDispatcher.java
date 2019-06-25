package org.openl.rules.context;

import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;

public interface IRulesRuntimeContextOptimizationForOpenMethodDispatcher {
    IOpenMethod getMethodForOpenMethodDispatcher(OpenMethodDispatcher openMethodDispatcher);
    
    void putMethodForOpenMethodDispatcher(OpenMethodDispatcher openMethodDispatcher, IOpenMethod method);
}
