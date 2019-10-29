package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;

/**
 * @author snshor
 *
 */
public class ContainsInArrayIndexedEvaluator extends AContainsInArrayIndexedEvaluator {

    public ContainsInArrayIndexedEvaluator(IOpenCast paramToExpressionOpenCast, IOpenCast expressionToParamOpenCast) {
        super(paramToExpressionOpenCast, expressionToParamOpenCast);
    }

    @Override
    public ARuleIndex makeIndex(ICondition condition, IIntIterator iterator) {
        if (iterator.size() < 1) {
            return null;
        }

        EqualsIndex.Builder builder = new EqualsIndex.Builder();
        builder.setExpressionToParamOpenCast(expressionToParamOpenCast);
        while (iterator.hasNext()) {
            int ruleN = iterator.nextInt();

            if (condition.isEmpty(ruleN)) {
                builder.putEmptyRule(ruleN);
                continue;
            }

            Object values = condition.getParamValue(0, ruleN);
            int length = Array.getLength(values);

            for (int j = 0; j < length; j++) {
                Object value = Array.get(values, j);
                value = convertWithParamToExpressionOpenCast(value);
                builder.putValueToRule(value, ruleN);
            }
        }
        return builder.build();
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.ARRAY_CONDITION_PRIORITY;
    }
}
