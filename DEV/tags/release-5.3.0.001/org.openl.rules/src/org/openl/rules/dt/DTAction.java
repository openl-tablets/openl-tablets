package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

public class DTAction extends FunctionalRow implements IDTAction {
    boolean isReturnAction = false;

    boolean isSingleReturnParam = false;

    public DTAction(String name, int row, ILogicalTable decisionTable, boolean isReturAction) {
        super(name, row, decisionTable);
        isReturnAction = isReturAction;
    }

    @Override
    public Object executeAction(int col, Object target, Object[] dtParams, IRuntimeEnv env) {
        if (isSingleReturnParam) {
            Object[] values = paramValues[col];
            if (values == null) {
                return null;
            }

            Object[] ary = new Object[values.length];
            loadParams(ary, 0, values, target, dtParams, env);
            return ary[0];
        }

        return super.executeAction(col, target, dtParams, env);
    }

    public boolean isAction() {
        return true;
    }

    public boolean isCondition() {
        return false;
    }

    public boolean isReturnAction() {
        return isReturnAction;
    }

    public void prepareAction(IOpenClass methodType, IMethodSignature signature, OpenL openl, ModuleOpenClass dtModule,
            IBindingContextDelegator cxtd, RuleRow ruleRow) throws Exception {
        prepare(methodType, signature, openl, dtModule, cxtd, ruleRow);

        IParameterDeclaration[] params = getParams();

        isSingleReturnParam = (params.length == 1 && params[0].getName().equals(
                method.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode()));
    }

}
