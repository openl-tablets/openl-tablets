package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.vm.IRuntimeEnv;

public class DefaultConditionSelector implements IIntSelector {

    private ICondition condition;
    private Object target;
    private Object[] params;
    private IRuntimeEnv env;

    public DefaultConditionSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {
        this.condition = condition;
        this.target = target;
        this.params = params;
        this.env = env;
    }

    public boolean select(int rule) {
        return evaluateConditionExpression(condition, rule, target, params, env);
    }

    public boolean evaluateConditionExpression(ICondition condition,
            int rule,
            Object target,
            Object[] dtparams,
            IRuntimeEnv env) {

        return condition.calculateCondition(rule, target, dtparams, env).getBooleanValue();
    }

}
