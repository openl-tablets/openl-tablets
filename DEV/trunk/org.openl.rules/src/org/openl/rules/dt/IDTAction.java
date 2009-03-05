package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public interface IDTAction extends IDecisionRow {
    Object executeAction(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    public boolean isReturnAction();

    /**
     * @param methodType
     * @param signature
     * @param openl
     * @param dtModule
     * @param cxtd
     * @param ruleRow
     */
    void prepareAction(IOpenClass methodType, IMethodSignature signature, OpenL openl, ModuleOpenClass dtModule,
            IBindingContextDelegator cxtd, RuleRow ruleRow) throws Exception;

}
