package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ContainsInArraySelector implements IIntSelector {

    private final ICondition condition;
    private final Object value;

    @Override
    public boolean select(int ruleN) {
        if (condition.isEmpty(ruleN)) {
            return true;
        }

        var array = condition.getParamValue(0, ruleN);
        var size = Array.getLength(array);
        for (var i = 0; i < size; ++i) {
            if (Objects.equals(Array.get(array, i), value)) {
                return true;
            }
        }

        return false;
    }
}
