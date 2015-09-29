package org.openl.rules.dtx;

import org.openl.vm.IRuntimeEnv;

public interface IBaseAction extends IBaseDecisionRow {

    Object executeAction(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    boolean isReturnAction();

    void removeDebugInformation();
}
