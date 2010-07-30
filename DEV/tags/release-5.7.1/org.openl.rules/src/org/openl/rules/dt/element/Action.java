package org.openl.rules.dt.element;

import org.apache.commons.lang.ClassUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.data.RuleExecutionObject;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class Action extends FunctionalRow implements IAction {

    private boolean isReturnAction = false;
    private boolean isSingleReturnParam = false;
    
    private IOpenClass ruleExecutionType;

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

            Object returnValue = array[0];
            IOpenMethod method = getMethod();
            IOpenClass returnType = method.getType();
            
            // Check that returnValue object has the same type as a return type
            // of method. If they are same return returnValue as result of
            // execution.
            //
            if (ClassUtils.isAssignable(returnValue.getClass(), returnType.getInstanceClass(), true)) {
                return returnValue;
            }
            
            // At this point of action execution we have the result value but it
            // has different type than return type of method. We should skip
            // optimization for this step and invoke method.
            //
            return method.invoke(target, new Object[] { returnValue }, env);
        }

        return executeActionInternal(column, target, params, env);
    }

    private Object executeActionInternal(int ruleNum, Object target, Object[] params, IRuntimeEnv env) {

        Object[][] values = getParamValues();
        
        if (values == null) {
            return null;
        }
        
        Object value = getParamValues()[ruleNum];

        if (value == null) {
            return null;
        }
        
        RuleExecutionObject newTarget = new RuleExecutionObject(ruleExecutionType, target, ruleNum);
        return getMethod().invoke(newTarget, mergeParams(target, params, env, (Object[]) value), env);
    }

    public void prepareAction(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow, IOpenClass ruleExecutionType) throws Exception {

        prepare(methodType, signature, openl, module, bindingContextDelegator, ruleRow);
        this.ruleExecutionType = ruleExecutionType;
        
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
