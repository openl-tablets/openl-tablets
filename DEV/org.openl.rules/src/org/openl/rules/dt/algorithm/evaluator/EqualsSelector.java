package org.openl.rules.dt.algorithm.evaluator;

import java.math.BigDecimal;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.helpers.NumberUtils;
import org.openl.vm.IRuntimeEnv;

public class EqualsSelector implements IIntSelector {

    private final ICondition condition;
    private final Object value;
    private final Object target;
    private final Object[] params;
    private final IRuntimeEnv env;

    EqualsSelector(ICondition condition, Object value, Object target, Object[] params, IRuntimeEnv env) {
        this.condition = condition;
        this.value = value;
        this.params = params;
        this.env = env;
        this.target = target;
    }

    @Override
    public boolean select(int ruleN) {

        if (condition.isEmpty(ruleN)) {
            return true;
        }

        Object[] realParams = new Object[condition.getNumberOfParams()];

        condition.loadValues(realParams, 0, ruleN, target, this.params, env);

        if (realParams[0] == null) {
            return value == null;
        }

        // Work around for BigDecimal
        if (value instanceof BigDecimal decimal && realParams[0] instanceof BigDecimal decimal1) {
            return decimal.compareTo(decimal1) == 0;
        }

        if (value instanceof BigDecimal decimal && NumberUtils.isObjectFloatPointNumber(realParams[0])) {
            Double d = NumberUtils.convertToDouble(realParams[0]);
            return decimal.compareTo(BigDecimal.valueOf(d)) == 0;
        }

        if (NumberUtils.isObjectFloatPointNumber(value) && realParams[0] instanceof BigDecimal decimal) {
            Double d = NumberUtils.convertToDouble(value);
            return decimal.compareTo(BigDecimal.valueOf(d)) == 0;
        }

        return realParams[0].equals(value);
    }
}
