package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

public class ContainsInArraySelector implements IIntSelector {

    private ICondition condition;
    private Object value;
    private Object target;
    private Object[] params;
    private IRuntimeEnv env;

    ContainsInArraySelector(ICondition condition, Object value, Object target, Object[] params, IRuntimeEnv env) {
        this.condition = condition;
        this.value = value;
        this.params = params;
        this.env = env;
        this.target = target;
    }

    public boolean select(int ruleN) {

        if (condition.isEmpty(ruleN)) {
            return true;
        }

        Object[] realParams = new Object[condition.getNumberOfParams()];

        condition.loadValues(realParams, 0, ruleN, target, this.params, env);

        if (realParams[0] == null) {
            return true;
        }

        return ArrayTool.contains(realParams[0], value);
    }
}
