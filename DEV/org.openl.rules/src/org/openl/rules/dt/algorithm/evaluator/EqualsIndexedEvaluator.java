package org.openl.rules.dt.algorithm.evaluator;

import java.math.BigDecimal;
import java.util.*;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.helpers.NumberUtils;

/**
 * @author snshor
 *
 */
public class EqualsIndexedEvaluator extends AEqualsIndexedEvaluator {

    public EqualsIndexedEvaluator(IOpenCast openCast) {
        super(openCast);
    }

    @Override
    public ARuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }

        Map<Object, DecisionTableRuleNodeBuilder> map = null;
        Map<Object, DecisionTableRuleNode> nodeMap = null;
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();
        boolean comparatorBasedMap = false;
        for (; it.hasNext();) {
            int i = it.nextInt();

            if (condition.isEmpty(i)) {
                emptyBuilder.addRule(i);
                if (map != null) {
                    for (DecisionTableRuleNodeBuilder dtrnb : map.values()) {
                        dtrnb.addRule(i);
                    }
                }
                continue;
            }

            Object value = condition.getParamValue(0, i);
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

            DecisionTableRuleNodeBuilder dtrb = map.computeIfAbsent(value,
                e -> new DecisionTableRuleNodeBuilder(emptyBuilder));

            dtrb.addRule(i);

        }
        if (map != null) {
            for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
                nodeMap.put(element.getKey(), element.getValue().makeNode());
            }
        } else {
            nodeMap = Collections.emptyMap();
        }

        return new EqualsIndex(emptyBuilder.makeNode(), nodeMap);
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        Set<Object> uniqueVals = null;
        while (it.hasNext()) {
            int i = it.nextInt();
            if (condition.isEmpty(i)) {
                continue;
            }
            Object val = condition.getParamValue(0, i);
            if (uniqueVals == null) {
                if (NumberUtils.isFloatPointNumber(val)) {
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
