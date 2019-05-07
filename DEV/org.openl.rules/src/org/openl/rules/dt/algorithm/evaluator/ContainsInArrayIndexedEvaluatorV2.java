package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.EqualsIndexV2;
import org.openl.rules.dt.index.IRuleIndex;
import org.openl.rules.helpers.NumberUtils;

public class ContainsInArrayIndexedEvaluatorV2 extends AContainsInArrayIndexedEvaluator {

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator iterator) {
        if (iterator.size() < 1) {
            return null;
        }

        DecisionTableRuleNodeBuilder nextNodeBuilder = new DecisionTableRuleNodeBuilder();
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        Map<Object, DecisionTableRuleNodeBuilder> map = null;
        Map<Object, int[]> nodeMap = null;
        boolean comparatorBasedMap = false;

        while (iterator.hasNext()) {
            int ruleN = iterator.nextInt();
            nextNodeBuilder.addRule(ruleN);

            if (condition.isEmpty(ruleN)) {
                emptyBuilder.addRule(ruleN);
                continue;
            }

            Object values = condition.getParamValue(0, ruleN);

            int length = Array.getLength(values);

            for (int j = 0; j < length; j++) {
                Object value = Array.get(values, j);
                if (comparatorBasedMap && !(value instanceof Comparable<?>)) {
                    throw new IllegalArgumentException("Invalid state! Index based on comparable interface!");
                }
                if (map == null) {
                    if (NumberUtils.isFloatPointNumber(value)) {
                        if (value instanceof BigDecimal) {
                            map = new TreeMap<>();
                            nodeMap = new TreeMap<>();
                        } else {
                            map = new TreeMap<>(FloatTypeComparator.getInstance());
                            nodeMap = new TreeMap<>(FloatTypeComparator.getInstance());
                        }
                        comparatorBasedMap = true;
                    } else {
                        map = new HashMap<>();
                        nodeMap = new HashMap<>();
                    }
                }

                DecisionTableRuleNodeBuilder builder = map.computeIfAbsent(value,
                    e -> new DecisionTableRuleNodeBuilder());
                builder.addRule(ruleN);
            }
        }
        if (map == null) {
            nodeMap = Collections.emptyMap();
        } else {
            for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
                nodeMap.put(element.getKey(), element.getValue().makeRulesAry());
            }
        }

        return new EqualsIndexV2(nextNodeBuilder.makeNode(), nodeMap, emptyBuilder.makeRulesAry());
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.ARRAY_CONDITION_PRIORITY_V2;
    }

}
