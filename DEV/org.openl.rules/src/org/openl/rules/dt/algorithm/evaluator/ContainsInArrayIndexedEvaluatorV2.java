package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.EqualsIndexV2;
import org.openl.rules.dt.index.IRuleIndex;

public class ContainsInArrayIndexedEvaluatorV2 extends AContainsInArrayIndexedEvaluator {

    public ContainsInArrayIndexedEvaluatorV2(IOpenCast paramToExpressionOpenCast, IOpenCast expressionToParamOpenCast) {
        super(paramToExpressionOpenCast, expressionToParamOpenCast);
    }

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator iterator) {
        if (iterator.size() < 1) {
            return null;
        }

        EqualsIndexV2.Builder builder = new EqualsIndexV2.Builder();
        builder.setExpressionToParamOpenCast(expressionToParamOpenCast);
        while (iterator.hasNext()) {
            int ruleN = iterator.nextInt();
            builder.addRule(ruleN);

            if (condition.isEmpty(ruleN)) {
                builder.putEmptyRule(ruleN);
                continue;
            }

            Object values = condition.getParamValue(0, ruleN);

            int length = Array.getLength(values);

            for (int j = 0; j < length; j++) {
                Object value = Array.get(values, j);
                builder.putValueToRule(value, ruleN);
            }
        }
        return builder.build();
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.ARRAY_CONDITION_PRIORITY_V2;
    }

}
