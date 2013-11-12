package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntSelector;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.BooleanTypeAdaptor;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

public class ContainsInOrNotInArraySelector implements IIntSelector {

    private ICondition condition;
    private Object value;
    private Object target;
    private Object[] params;
    private IRuntimeEnv env;
    private BooleanTypeAdaptor adaptor;

    public ContainsInOrNotInArraySelector(ICondition condition,
            Object value,
            Object target,
            Object[] params,
            BooleanTypeAdaptor adaptor,
            IRuntimeEnv env) {

        this.condition = condition;
        this.value = value;
        this.params = params;
        this.env = env;
        this.target = target;
        this.adaptor = adaptor;
    }

    public boolean select(int rule) {

        Object[][] params = condition.getParamValues();
        Object[] ruleParams = params[rule];

        if (ruleParams == null) {
            return true;
        }

        Object[] realParams = new Object[params.length];

        RuleRowHelper.loadParams(realParams, 0, ruleParams, target, this.params, env);

        if (ruleParams.length < 2 || ruleParams[1] == null) {
            return true;
        }

        boolean isIn = ruleParams[0] == null || adaptor.extractBooleanValue(ruleParams[0]);

        return ArrayTool.contains(ruleParams[1], value) ^ isIn;
    }
}
