package org.openl.rules.dt;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public interface IBaseAction extends IBaseDecisionRow {

    Object executeAction(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env);
    
    IOpenClass getType();
    
    boolean isReturnAction();
    
    boolean isCollectReturnAction();
    
    boolean isCollectReturnKeyAction();

    void removeDebugInformation();
}
