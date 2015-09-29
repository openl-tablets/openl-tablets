package org.openl.rules.dt.element;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.data.RuleExecutionObject;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.*;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterDeclaration;
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

        if (target instanceof IDynamicObject) {
            target = new RuleExecutionObject(ruleExecutionType, (IDynamicObject) target, column);
        }

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
            if (returnValue == null || ClassUtils.isAssignable(returnValue.getClass(),
                    returnType.getInstanceClass(),
                    true)) {
                return returnValue;
            }

            // At this point of action execution we have the result value but it
            // has different type than return type of method. We should skip
            // optimization for this step and invoke method.
            //
            return executeActionInternal(column, target, params, env);
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

        return getMethod().invoke(target, mergeParams(target, params, env, (Object[]) value), env);
    }

    public void prepareAction(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            ComponentOpenClass componentOpenClass,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow, IOpenClass ruleExecutionType) throws Exception {

        prepare(methodType, signature, openl, componentOpenClass, bindingContextDelegator, ruleRow);
        this.ruleExecutionType = ruleExecutionType;

        IParameterDeclaration[] params = getParams();
        CompositeMethod method = getMethod();
        String code = method.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();

        isSingleReturnParam = params.length == 1 && params[0].getName().equals(code);
    }

    @Override
    protected IParameterDeclaration[] getParams(
            IOpenSourceCodeModule methodSource, IMethodSignature signature,
            IOpenClass declaringClass, IOpenClass methodType, OpenL openl,
            IBindingContext bindingContext) throws Exception {

        if ("extraRet".equals(methodSource.getCode()) && isReturnAction() && getParams() == null) {
            if (!bindingContext.isExecutionMode()) {
                setCellMetaInfo(0, methodType);
            }
            ParameterDeclaration extraParam = new ParameterDeclaration(methodType, "extraRet");

            IParameterDeclaration[] parameterDeclarations = new IParameterDeclaration[] { extraParam };
            setParams(parameterDeclarations);
            return getParams();
        }

        return super.getParams(methodSource, signature, declaringClass, methodType,
                openl, bindingContext);
    }

    @Override
    protected IOpenSourceCodeModule getExpressionSource(IBindingContext bindingContext) {

        IOpenSourceCodeModule source = super.getExpressionSource(bindingContext);

        if (isReturnAction() && StringUtils.isEmpty(source.getCode()) && getParams() == null) {
            return new StringSourceCodeModule("extraRet", source.getUri(0));
        }

        return super.getExpressionSource(bindingContext);
    }

    @Override
    public void removeDebugInformation() {
        getMethod().removeDebugInformation();
        Object[][] paramValues = getParamValues();
        if (paramValues != null) {
            for (Object[] paramValueColumn : paramValues) {
                for (Object paramValue : paramValueColumn) {
                    if (paramValue instanceof CompositeMethod) {
                        ((CompositeMethod) paramValue).removeDebugInformation();
                    }
                }
            }
        }
    }
}
