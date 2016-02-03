package org.openl.rules.dt;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.domain.IIntIterator;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.algorithm.FailOnMissException;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dtx.trace.DTRuleTracerLeaf;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link DecisionTable}.
 *
 * @author DLiauchuk
 */
public class DecisionTableInvoker extends RulesMethodInvoker<DecisionTable> {

    public DecisionTableInvoker(DecisionTable decisionTable) {
        super(decisionTable);
    }

    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithm() != null;
    }

    private Object invokeOptimized(Object target, Object[] params, IRuntimeEnv env) {

        IIntIterator rules = getInvokableMethod().getAlgorithm().checkedRules(target, params, env);

        Object returnValue = null;
        boolean atLeastOneRuleFired = false;

        while (rules.hasNext()) {
            atLeastOneRuleFired = true;
            int ruleN = rules.nextInt();

            returnValue = getReturn(target, params, env, ruleN);
            if (returnValue != null) {
                return returnValue;
            }
        }

        if (!atLeastOneRuleFired && getInvokableMethod().shouldFailOnMiss()) {

            String method = MethodUtil.printMethodWithParameterValues(getInvokableMethod().getMethod(), params,
                    INamedThing.REGULAR);
            String message = String.format("%s failed to match any rule condition", method);

            throw new FailOnMissException(message, getInvokableMethod(), params);
        }

        return returnValue;
    }

    private Object invokeTracedOptimized(Object target, Object[] params, IRuntimeEnv env) {
        DecisionTableTraceObject traceObject = (DecisionTableTraceObject) getTraceObject(params);
        Tracer.begin(traceObject);

        try {
            IDecisionTableAlgorithm algorithm = getInvokableMethod().getAlgorithm();
            IDecisionTableAlgorithm algorithmDelegator = algorithm.asTraceDecorator();

            IIntIterator rules = algorithmDelegator.checkedRules(target, params, env);

            while (rules.hasNext()) {

                int ruleNumber = rules.nextInt();

                Tracer.begin(new DTRuleTracerLeaf(traceObject, ruleNumber));
                try {

                    Object returnValue = getReturn(target, params, env, ruleNumber);
                    if (returnValue != null) {
                        traceObject.setResult(returnValue);
                        return returnValue;
                    }
                } finally {
                    Tracer.end();
                }
            }
        } catch (Exception e) {
            addErrorToTrace(traceObject, e);
        } finally {
            Tracer.end();
        }

        return null;
    }

    protected Object getReturn(Object target, Object[] params, IRuntimeEnv env, int ruleN) {
        Object returnValue = null;
        for (int j = 0; j < getInvokableMethod().getActionRows().length; j++) {
            IAction action = getInvokableMethod().getAction(j);

            Object actionResult = action.executeAction(ruleN, target, params, env);

            if (action.isReturnAction() && returnValue == null && (actionResult != null || (!action.isEmpty(ruleN)))) {
                returnValue = actionResult;
            }
        }
        return returnValue;
    }

    private void addErrorToTrace(DecisionTableTraceObject traceObject, Throwable e) {
        traceObject.setError(e);
        throw new OpenLRuntimeException(e);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        return invokeTracedOptimized(target, params, env);
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        return invokeOptimized(target, params, env);
    }
}
