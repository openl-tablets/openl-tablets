package org.openl.rules.dt;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt.algorithm.FailOnMissException;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dtx.IBaseAction;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

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

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        IDecisionTableAlgorithm algorithm = getInvokableMethod().getAlgorithm();
        if (Tracer.isTracerOn()) {
            algorithm = algorithm.asTraceDecorator();
        }
        IIntIterator rules = algorithm.checkedRules(target, params, env);

        Object returnValue;
        boolean atLeastOneRuleFired = false;
        IBaseAction[] actions = getInvokableMethod().getActionRows();

        while (rules.hasNext()) {
            atLeastOneRuleFired = true;
            int ruleN = rules.nextInt();

            returnValue = Tracer.invoke(new ActionInvoker(ruleN, actions), target, params, env, this);
            if (returnValue != null) {
                return returnValue;
            }
        }

        if (!atLeastOneRuleFired && getInvokableMethod().shouldFailOnMiss()) {

            String method = MethodUtil.printMethodWithParameterValues(getInvokableMethod().getMethod(),
                params,
                INamedThing.REGULAR);
            String message = String.format("%s failed to match any rule condition", method);

            throw new FailOnMissException(message, getInvokableMethod(), params);
        }

        return null;
    }

}
