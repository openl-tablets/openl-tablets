package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.util.Objects;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;

public class ContainsInArraySelector implements IIntSelector {

    private ICondition condition;
    private Object value;

    ContainsInArraySelector(ICondition condition, Object value) {
        this.condition = condition;
        this.value = value;
    }

    @Override
    public boolean select(int ruleN) {
        if (condition.isEmpty(ruleN)) {
            return true;
        }

        Object array = condition.getParamValue(0, ruleN);
        int size = Array.getLength(array);
        for (int i = 0; i < size; ++i) {
            if (Objects.equals(Array.get(array, i), value)) {
                return true;
            }
        }

        return false;
    }
}
