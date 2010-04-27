package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class Action extends FunctionalRow implements IAction {

    private boolean isReturnAction = false;
    private boolean isSingleReturnParam = false;

    public Action(String name, int row, ILogicalTable decisionTable, boolean isReturnAction) {
        super(name, row, decisionTable);

        this.isReturnAction = isReturnAction;
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

    public Object executeAction(int column, Object target, Object[] params, IRuntimeEnv env) {

        if (isSingleReturnParam) {

            Object[] values = getParamValues()[column];

            if (values == null) {
                return null;
            }

            Object[] array = new Object[values.length];
            RuleRowHelper.loadParams(array, 0, values, target, params, env);

            return array[0];
        }

        return executeActionInternal(column, target, params, env);
    }

    private Object executeActionInternal(int column, Object target, Object[] params, IRuntimeEnv env) {

        Object value = getParamValues()[column];

        if (value == null) {
            return null;
        }

        if (hasNoParams()) {
            return getMethod().invoke(target, params, env);
        } else {
            return getMethod().invoke(target, mergeParams(target, params, env, (Object[]) value), env);
        }
    }

    public void prepareAction(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow) throws Exception {

        prepare(methodType, signature, openl, module, bindingContextDelegator, ruleRow);

        IParameterDeclaration[] params = getParams();
        CompositeMethod method = (CompositeMethod) getMethod();
        String code = method.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();

        if (params.length == 1 && params[0].getName().equals(code)) {
            isSingleReturnParam = true;
        } else {
            isSingleReturnParam = false;
        }
    }

}
