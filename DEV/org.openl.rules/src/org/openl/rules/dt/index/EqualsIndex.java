package org.openl.rules.dt.index;

import java.math.BigDecimal;
import java.util.*;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.algorithm.evaluator.FloatTypeComparator;
import org.openl.rules.helpers.NumberUtils;

public class EqualsIndex extends ARuleIndex {

    private Map<Object, DecisionTableRuleNode> valueNodes;

    public EqualsIndex(DecisionTableRuleNode emptyOrFormulaNodes,
            Map<Object, DecisionTableRuleNode> valueNodes,
            IOpenCast expressionToParamOpenCast) {
        super(emptyOrFormulaNodes, expressionToParamOpenCast);
        this.valueNodes = Objects.requireNonNull(valueNodes, "valueNodes cannot be null");
    }

    @Override
    DecisionTableRuleNode findNodeInIndex(Object value) {
        if (value != null) {
            return valueNodes.get(value);
        }
        return null;
    }

    @Override
    public Iterable<DecisionTableRuleNode> nodes() {
        return valueNodes.values();
    }

    public static class Builder {
        private Map<Object, DecisionTableRuleNodeBuilder> map = null;
        private Map<Object, DecisionTableRuleNode> nodeMap = null;
        private DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();
        private boolean comparatorBasedMap = false;
        private IOpenCast expressionToParamOpenCast;

        public void putEmptyRule(int ruleN) {
            emptyBuilder.addRule(ruleN);
            if (map != null) {
                for (DecisionTableRuleNodeBuilder nodeBuilder : map.values()) {
                    nodeBuilder.addRule(ruleN);
                }
            }
        }

        public void setExpressionToParamOpenCast(IOpenCast expressionToParamOpenCast) {
            this.expressionToParamOpenCast = expressionToParamOpenCast;
        }

        public void putValueToRule(Object value, int ruleN) {
            if (comparatorBasedMap && !(value instanceof Comparable<?>)) {
                throw new IllegalArgumentException("Invalid state! Index based on comparable interface.");
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
                e -> new DecisionTableRuleNodeBuilder(emptyBuilder));

            builder.addRule(ruleN);
        }

        public EqualsIndex build() {
            if (map == null) {
                nodeMap = Collections.emptyMap();
            } else {
                for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
                    nodeMap.put(element.getKey(), element.getValue().makeNode());
                }
            }
            return new EqualsIndex(emptyBuilder.makeNode(), nodeMap, expressionToParamOpenCast);
        }
    }
}
