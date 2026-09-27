package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import lombok.RequiredArgsConstructor;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.domain.IntArrayIterator;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.dt.type.BooleanTypeAdaptor;
import org.openl.rules.helpers.NumberUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class ContainsInOrNotInArrayIndexedEvaluator implements IConditionEvaluator {

    private final BooleanTypeAdaptor adaptor;

    // TODO fix
    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {

        var value = condition.getEvaluator().invoke(target, params, env);

        return new ContainsInOrNotInArraySelector(condition, value, target, params, this.adaptor, env);
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public ARuleIndex makeIndex(ICondition condition, IIntIterator iterator) {

        if (iterator.size() < 1) {
            return null;
        }

        Set<Object> allValues = null;

        var copyRules = new DecisionTableRuleNodeBuilder();
        var valueSets = new ArrayList<Set<?>>();

        var globalComparatorBasedSet = false;
        var globalSmartFloatComparatorIsUsed = false;

        while (iterator.hasNext()) {

            var i = iterator.nextInt();
            copyRules.addRule(i);

            if (condition.isEmpty(i)) {
                valueSets.add(Set.of());
                continue;
            }

            Set<Object> values = null;
            var valuesArray = condition.getParamValue(1, i);

            var length = Array.getLength(valuesArray);
            var comparatorBasedSet = false;
            var smartFloatComparatorIsUsed = false;

            for (var j = 0; j < length; j++) {
                Object value = Array.get(valuesArray, j);
                requireComparable(comparatorBasedSet, value);
                if (allValues == null) {
                    comparatorBasedSet = NumberUtils.isObjectFloatPointNumber(value);
                    smartFloatComparatorIsUsed = comparatorBasedSet && !(value instanceof BigDecimal);
                    allValues = newValueSet(comparatorBasedSet, smartFloatComparatorIsUsed);
                }
                if (comparatorBasedSet) {
                    values = newValueSet(comparatorBasedSet, smartFloatComparatorIsUsed);
                }
                allValues.add(value);
                values.add(value);
            }
            globalComparatorBasedSet = globalComparatorBasedSet || comparatorBasedSet;
            globalSmartFloatComparatorIsUsed = globalSmartFloatComparatorIsUsed || smartFloatComparatorIsUsed;
            valueSets.add(values);
        }

        var rules = copyRules.makeRulesAry();
        iterator = new IntArrayIterator(rules);

        Map<Object, DecisionTableRuleNodeBuilder> map = newIndexMap(globalComparatorBasedSet,
                globalSmartFloatComparatorIsUsed);

        var emptyBuilder = new DecisionTableRuleNodeBuilder();

        addRules(condition, iterator, valueSets, allValues, map, emptyBuilder);

        var nodeMap = makeNodes(map, globalComparatorBasedSet, globalSmartFloatComparatorIsUsed);

        return new EqualsIndex(emptyBuilder.makeNode(), nodeMap, null);
    }

    private static void requireComparable(boolean comparatorBasedSet, Object value) {
        if (comparatorBasedSet && !(value instanceof Comparable<?>)) {
            throw new IllegalArgumentException("Illegal state. Index based on comparable interface.");
        }
    }

    private static Set<Object> newValueSet(boolean comparatorBasedSet, boolean smartFloatComparatorIsUsed) {
        if (comparatorBasedSet) {
            if (smartFloatComparatorIsUsed) {
                return new TreeSet<>(FloatTypeComparator.getInstance());
            } else {
                return new TreeSet<>();
            }
        } else {
            return new HashSet<>();
        }
    }

    private static <V> Map<Object, V> newIndexMap(boolean comparatorBasedSet, boolean smartFloatComparatorIsUsed) {
        if (comparatorBasedSet) {
            if (smartFloatComparatorIsUsed) {
                return new TreeMap<>(FloatTypeComparator.getInstance());
            } else {
                return new TreeMap<>();
            }
        } else {
            return new HashMap<>();
        }
    }

    private void addRules(ICondition condition,
                          IIntIterator iterator,
                          List<Set<?>> valueSets,
                          Set<Object> allValues,
                          Map<Object, DecisionTableRuleNodeBuilder> map,
                          DecisionTableRuleNodeBuilder emptyBuilder) {
        while (iterator.hasNext()) {

            var i = iterator.nextInt();

            if (condition.isEmpty(i)) {

                emptyBuilder.addRule(i);
                for (DecisionTableRuleNodeBuilder dtrnb : map.values()) {
                    dtrnb.addRule(i);
                }
                continue;
            }

            addRule(condition, i, valueSets, allValues, map, emptyBuilder);
        }
    }

    private void addRule(ICondition condition,
                         int i,
                         List<Set<?>> valueSets,
                         Set<Object> allValues,
                         Map<Object, DecisionTableRuleNodeBuilder> map,
                         DecisionTableRuleNodeBuilder emptyBuilder) {
        var isInObject = condition.getParamValue(0, i);
        var isIn = isInObject == null || adaptor.extractBooleanValue(isInObject);

        var values = valueSets.get(i);

        if (isIn) {

            for (Object value : values) {
                addRuleForValue(map, value, emptyBuilder, i);
            }
        } else {

            for (Object value : allValues) {

                if (values.contains(value)) {
                    continue;
                }

                addRuleForValue(map, value, emptyBuilder, i);
            }

            emptyBuilder.addRule(i); // !!!!!
        }
    }

    private static void addRuleForValue(Map<Object, DecisionTableRuleNodeBuilder> map,
                                        Object value,
                                        DecisionTableRuleNodeBuilder emptyBuilder,
                                        int i) {
        var builder = map.get(value);

        if (builder == null) {
            builder = new DecisionTableRuleNodeBuilder(emptyBuilder);
            map.put(value, builder);
        }

        builder.addRule(i);
    }

    private static Map<Object, DecisionTableRuleNode> makeNodes(Map<Object, DecisionTableRuleNodeBuilder> map,
                                                                boolean comparatorBasedSet,
                                                                boolean smartFloatComparatorIsUsed) {
        Map<Object, DecisionTableRuleNode> nodeMap = newIndexMap(comparatorBasedSet, smartFloatComparatorIsUsed);
        for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
            nodeMap.put(element.getKey(), element.getValue().makeNode());
        }
        return nodeMap;
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        return 0;
    }

    @Override
    public IDomain<? extends Object> getRuleParameterDomain(IBaseCondition condition) {
        return null;
    }

    @Override
    public IDomain<? extends Object> getConditionParameterDomain(int paramIdx, IBaseCondition condition) {
        return null;
    }

    @Override
    public String getOptimizedSourceCode() {
        return null;
    }

    @Override
    public void setOptimizedSourceCode(String code) {
        // The evaluator keeps the source code as it is.
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.ARRAY2_CONDITION_PRIORITY;
    }
}
