package org.openl.rules.dt.algorithm.evaluator;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.helpers.NumberUtils;

/**
 * @author snshor
 */
public class EqualsIndexedEvaluator extends AEqualsIndexedEvaluator {

    public EqualsIndexedEvaluator(ConditionCasts conditionCasts) {
        super(conditionCasts);
    }

    @Override
    public ARuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }

        var builder = new EqualsIndex.Builder();
        builder.setConditionCasts(conditionCasts);
        while (it.hasNext()) {
            var ruleN = it.nextInt();

            if (condition.isEmpty(ruleN)) {
                builder.putEmptyRule(ruleN);
                continue;
            }

            var value = conditionCasts.castToInputType(condition.getParamValue(0, ruleN));
            builder.putValueToRule(value, ruleN);

        }
        return builder.build();
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        Set<Object> uniqueVals = null;
        while (it.hasNext()) {
            var i = it.nextInt();
            if (condition.isEmpty(i)) {
                continue;
            }
            var val = conditionCasts.castToInputType(condition.getParamValue(0, i));
            if (uniqueVals == null) {
                if (NumberUtils.isObjectFloatPointNumber(val)) {
                    if (val instanceof BigDecimal) {
                        uniqueVals = new HashSet<>();
                    } else {
                        uniqueVals = new TreeSet<>(FloatTypeComparator.getInstance());
                    }
                } else {
                    uniqueVals = new HashSet<>();
                }
            }
            uniqueVals.add(val);
        }
        return uniqueVals == null ? 0 : uniqueVals.size();
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.EQUALS_CONDITION_PRIORITY;
    }
}
