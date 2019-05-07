package org.openl.rules.dt.algorithm.evaluator;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.EqualsIndexV2;
import org.openl.rules.dt.index.IRuleIndex;
import org.openl.rules.helpers.NumberUtils;

public class EqualsIndexedEvaluatorV2 extends AEqualsIndexedEvaluator {

    public EqualsIndexedEvaluatorV2(IOpenCast openCast) {
        super(openCast);
    }

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }
        DecisionTableRuleNodeBuilder nextNodeBuilder = new DecisionTableRuleNodeBuilder();
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        Map<Object, DecisionTableRuleNodeBuilder> map = null;
        Map<Object, int[]> result = null;
        boolean comparatorBasedMap = false;
        while (it.hasNext()) {
            int ruleN = it.nextInt();
            nextNodeBuilder.addRule(ruleN);

            if (condition.isEmpty(ruleN)) {
                emptyBuilder.addRule(ruleN);
                continue;
            }

            Object value = condition.getParamValue(0, ruleN);
            if (openCast != null) {
                value = openCast.convert(value);
            }
            if (comparatorBasedMap && !(value instanceof Comparable<?>)) {
                throw new IllegalArgumentException("Invalid state! Index based on comparable interface!");
            }
            if (map == null) {
                if (NumberUtils.isFloatPointNumber(value)) {
                    if (value instanceof BigDecimal) {
                        map = new TreeMap<>();
                        result = new TreeMap<>();
                    } else {
                        map = new TreeMap<>(FloatTypeComparator.getInstance());
                        result = new TreeMap<>(FloatTypeComparator.getInstance());
                    }
                    comparatorBasedMap = true;
                } else {
                    map = new HashMap<>();
                    result = new HashMap<>();
                }
            }

            DecisionTableRuleNodeBuilder builder = map.computeIfAbsent(value, e -> new DecisionTableRuleNodeBuilder());

            builder.addRule(ruleN);
        }

        if (map == null) {
            result = Collections.emptyMap();
        } else {
            for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
                result.put(element.getKey(), element.getValue().makeRulesAry());
            }
        }

        return new EqualsIndexV2(nextNodeBuilder.makeNode(), result, emptyBuilder.makeRulesAry());
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
