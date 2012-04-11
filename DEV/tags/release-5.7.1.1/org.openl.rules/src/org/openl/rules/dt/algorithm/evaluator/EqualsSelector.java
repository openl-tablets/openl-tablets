package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.element.ICondition;
import org.openl.vm.IRuntimeEnv;

public class EqualsSelector implements IIntSelector {

    private ICondition condition;
    private Object value;
    private Object target;
    private Object[] params;
    private IRuntimeEnv env;

    public EqualsSelector(ICondition condition, Object value, Object target, Object[] params, IRuntimeEnv env) {
        this.condition = condition;
        this.value = value;
        this.params = params;
        this.env = env;
        this.target = target;
    }

    public boolean select(int rule) {
        
        Object[][] params = condition.getParamValues();
        Object[] ruleParams = params[rule];

        if (ruleParams == null) {
            return true;
        }

        Object[] realParams = new Object[ruleParams.length];

        RuleRowHelper.loadParams(realParams, 0, ruleParams, target, this.params, env);

        if (realParams[0] == null) {
            return value == null;
        }

        return realParams[0].equals(value);
    }
}
