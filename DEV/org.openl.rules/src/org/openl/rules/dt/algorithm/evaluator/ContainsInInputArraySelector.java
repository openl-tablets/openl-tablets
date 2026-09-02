package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;

/**
 * Selects the rules whose condition column value is present in the array passed to the decision table.
 *
 * <p>A rule with an empty condition cell is always selected. No rule is selected when the input array is null.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ContainsInInputArraySelector implements IIntSelector {

    private final ICondition condition;
    private final Object inputArray;
    private final ConditionCasts conditionCasts;

    @Override
    public boolean select(int ruleN) {
        if (condition.isEmpty(ruleN)) {
            return true;
        }
        if (inputArray == null) {
            return false;
        }

        var value = conditionCasts.castToInputType(condition.getParamValue(0, ruleN));
        var size = Array.getLength(inputArray);
        for (var i = 0; i < size; i++) {
            if (Objects.equals(conditionCasts.castToConditionType(Array.get(inputArray, i)), value)) {
                return true;
            }
        }

        return false;
    }
}
