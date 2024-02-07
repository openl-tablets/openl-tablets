package org.openl.rules.dt;

import org.openl.binding.MethodUtil;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt.algorithm.FailOnMissException;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.enumeration.DTEmptyResultProcessingEnum;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IOpenClass;
import org.openl.util.OpenClassUtils;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * Invoker for {@link DecisionTable}.
 *
 * @author DLiauchuk
 */
public class DecisionTableInvoker extends RulesMethodInvoker<DecisionTable> {

    private final boolean returnEmptyResult;
    private final IOpenClass retType;

    DecisionTableInvoker(DecisionTable decisionTable) {
        super(decisionTable);
        // This expression should be calculated once to improve performance of DT calculations
        this.returnEmptyResult = decisionTable.getMethodProperties() != null && DTEmptyResultProcessingEnum.RETURN
                .equals(decisionTable.getMethodProperties().getEmptyResultProcessing());
        this.retType = decisionTable.getType();
    }

    @Override
    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithm() != null;
    }

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        try {
            env.pushLocalFrame(new Object[]{new DecisionTableRuntimePool()});
            return doInvoke(target, params, env);
        } finally {
            env.popLocalFrame();
        }
    }

    private Object doInvoke(Object target, Object[] params, IRuntimeEnv env) {
        IDecisionTableAlgorithm algorithm = getInvokableMethod().getAlgorithm();
        IIntIterator rulesIntIterator = algorithm.checkedRules(target, params, env);

        // Do not move this line, hasNext should be extracted before action is invoked
        final boolean atLeastOneRuleFired = rulesIntIterator.hasNext();

        IBaseAction[] actions = getInvokableMethod().getActionRows();

        Object returnValue = Tracer
                .invoke(new ActionInvoker(rulesIntIterator, actions, returnEmptyResult), target, params, env, this);
        if (!OpenClassUtils.isVoid(retType) && returnValue != null) {
            return returnValue;
        }

        if (!atLeastOneRuleFired && getInvokableMethod().shouldFailOnMiss()) {
            String method = MethodUtil.printMethodWithParameterValues(getInvokableMethod().getMethod(), params);
            String message = String.format("Table '%s' failed to match any rule condition.", method);

            throw new FailOnMissException(message, getInvokableMethod());
        }

        return retType.nullObject();
    }

}
