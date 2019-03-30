package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
 * 
 */
public class ContainsInOrNotInArrayIndexedEvaluator implements IConditionEvaluator {

    private BooleanTypeAdaptor adaptor;

    public ContainsInOrNotInArrayIndexedEvaluator(BooleanTypeAdaptor adaptor) {
        this.adaptor = adaptor;
    }

    // TODO fix
    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {

        Object value = condition.getEvaluator().invoke(target, params, env);

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

        DecisionTableRuleNodeBuilder copyRules = new DecisionTableRuleNodeBuilder();
        List<Set<?>> valueSets = new ArrayList<>();

        boolean globalComparatorBasedSet = false;
        boolean globalSmartFloatComparatorIsUsed = false;

        while (iterator.hasNext()) {

            int i = iterator.nextInt();
            copyRules.addRule(i);

            if (condition.isEmpty(i)) {
                valueSets.add(Collections.emptySet());
                continue;
            }

            Set<Object> values = null;
            Object valuesArray = condition.getParamValue(1, i);

            int length = Array.getLength(valuesArray);
            boolean comparatorBasedSet = false;
            boolean smartFloatComparatorIsUsed = false;

            for (int j = 0; j < length; j++) {
                Object value = Array.get(valuesArray, j);
                if (comparatorBasedSet) {
                    if (!(value instanceof Comparable<?>)) {
                        throw new IllegalArgumentException("Invalid state! Index based on comparable interface!");
                    }
                }
                if (allValues == null) {
                    if (NumberUtils.isFloatPointNumber(value)) {
                        if (value instanceof BigDecimal) {
                            allValues = new TreeSet<>();
                        } else {
                            allValues = new TreeSet<>(FloatTypeComparator.getInstance());
                            smartFloatComparatorIsUsed = true;
                        }
                        comparatorBasedSet = true;
                    } else {
                        allValues = new HashSet<>();
                    }
                }
                if (comparatorBasedSet) {
                    if (smartFloatComparatorIsUsed) {
                        values = new TreeSet<>(FloatTypeComparator.getInstance());
                    } else {
                        values = new TreeSet<>();
                    }
                }
                allValues.add(value);
                values.add(value);
            }
            globalComparatorBasedSet = globalComparatorBasedSet || comparatorBasedSet;
            globalSmartFloatComparatorIsUsed = globalSmartFloatComparatorIsUsed || smartFloatComparatorIsUsed;
            valueSets.add(values);
        }

        int[] rules = copyRules.makeRulesAry();
        iterator = new IntArrayIterator(rules);

        Map<Object, DecisionTableRuleNodeBuilder> map;
        Map<Object, DecisionTableRuleNode> nodeMap;

        if (globalComparatorBasedSet) {
            if (globalSmartFloatComparatorIsUsed) {
                map = new TreeMap<>(FloatTypeComparator.getInstance());
                nodeMap = new TreeMap<>(FloatTypeComparator.getInstance());
            } else {
                nodeMap = new TreeMap<>();
                map = new TreeMap<>();
            }

        } else {
            map = new HashMap<>();
            nodeMap = new HashMap<>();
        }

        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        while (iterator.hasNext()) {

            int i = iterator.nextInt();

            if (condition.isEmpty(i)) {

                emptyBuilder.addRule(i);
                for (DecisionTableRuleNodeBuilder dtrnb : map.values()) {
                    dtrnb.addRule(i);
                }
                continue;
            }

            Object isInObject = condition.getParamValue(0, i);
            boolean isIn = isInObject == null || adaptor.extractBooleanValue(isInObject);

            Set<?> values = valueSets.get(i);

            if (isIn) {

                for (Object value : values) {

                    DecisionTableRuleNodeBuilder builder = map.get(value);

                    if (builder == null) {
                        builder = new DecisionTableRuleNodeBuilder(emptyBuilder);
                        map.put(value, builder);
                    }

                    builder.addRule(i);
                }
            } else {

                for (Object value : allValues) {

                    if (values.contains(value)) {
                        continue;
                    }

                    DecisionTableRuleNodeBuilder bilder = map.get(value);

                    if (bilder == null) {
                        bilder = new DecisionTableRuleNodeBuilder(emptyBuilder);
                        map.put(value, bilder);
                    }

                    bilder.addRule(i);
                }

                emptyBuilder.addRule(i); // !!!!!
            }
        }

        for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
            nodeMap.put(element.getKey(), element.getValue().makeNode());
        }

        return new EqualsIndex(emptyBuilder.makeNode(), nodeMap);
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
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.ARRAY2_CONDITION_PRIORITY;
    }
}
