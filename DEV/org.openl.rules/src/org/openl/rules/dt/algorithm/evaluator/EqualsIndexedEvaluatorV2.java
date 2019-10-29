package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.EqualsIndexV2;
import org.openl.rules.dt.index.IRuleIndex;

public class EqualsIndexedEvaluatorV2 extends AEqualsIndexedEvaluator {

    public EqualsIndexedEvaluatorV2(ConditionCasts conditionCasts) {
        super(conditionCasts);
    }

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }

        EqualsIndexV2.Builder builder = new EqualsIndexV2.Builder();
        builder.setConditionCasts(conditionCasts);
        while (it.hasNext()) {
            int ruleN = it.nextInt();
            builder.addRule(ruleN);

            if (condition.isEmpty(ruleN)) {
                builder.putEmptyRule(ruleN);
                continue;
            }

            Object value = conditionCasts.castToInputType(condition.getParamValue(0, ruleN));
            builder.putValueToRule(value, ruleN);
        }

        return builder.build();
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        return 0;
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.EQUALS_CONDITION_PRIORITY_V2;
    }

}
