package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.vm.IRuntimeEnv;

public class DefaultConditionSelector implements IIntSelector {

    private ICondition condition;
    private Object target;
    private Object[] params;
    private IRuntimeEnv env;

    DefaultConditionSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {
        this.condition = condition;
        this.target = target;
        this.params = params;
        this.env = env;
    }

    @Override
    public boolean select(int rule) {
        return condition.calculateCondition(rule, target, params, env).getBooleanValue();
    }

}
