package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.helpers.NumberUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public class ContainsInArrayIndexedEvaluator extends AConditionEvaluator implements IConditionEvaluator {

    private int uniqueKeysSize = -1;
    private int maxArrayLength = -1;

    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        IParameterDeclaration[] cparams = condition.getParams();

        IOpenSourceCodeModule conditionSource = condition.getSourceCodeModule();

        String code = String.format("containsCtr(%1$s, %2$s)", cparams[0].getName(), conditionSource.getCode());

        return new StringSourceCodeModule(code, conditionSource.getUri());
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, params, env);

        return new ContainsInArraySelector(condition, value);
    }

    public boolean isIndexed() {
        return true;
    }

    public ARuleIndex makeIndex(ICondition condition, IIntIterator iterator) {

        if (iterator.size() < 1) {
            return null;
        }

        Map<Object, DecisionTableRuleNodeBuilder> map = null;
        Map<Object, DecisionTableRuleNode> nodeMap = null;
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();
        boolean comparatorBasedMap = false;

        while (iterator.hasNext()) {

            int i = iterator.nextInt();

            if (condition.isEmpty(i)) {

                emptyBuilder.addRule(i);
                if (map != null) {
                    for (DecisionTableRuleNodeBuilder builder : map.values()) {
                        builder.addRule(i);
                    }
                }

                continue;
            }

            Object values = condition.getParamValue(0, i);

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
                    e -> new DecisionTableRuleNodeBuilder(emptyBuilder));
                builder.addRule(i);
            }
        }
        if (map != null) {
            for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
                nodeMap.put(element.getKey(), (element.getValue()).makeNode());
            }
        } else {
            nodeMap = Collections.emptyMap();
        }

        return new EqualsIndex(emptyBuilder.makeNode(), nodeMap);
    }

    @Override
    public int countUniqueKeys(ICondition condition, IIntIterator it) {
        if (uniqueKeysSize < 0) {
            countUniqueKeysAndMaxArrayLength(condition, it);
        }
        return uniqueKeysSize;
    }

    public int getMaxArrayLength(ICondition condition, IIntIterator it) {
        if (maxArrayLength < 0) {
            countUniqueKeysAndMaxArrayLength(condition, it);
        }
        return maxArrayLength;
    }

    private void countUniqueKeysAndMaxArrayLength(ICondition condition, IIntIterator it) {
        Set<Object> uniqueVals = null;
        while (it.hasNext()) {
            int i = it.nextInt();
            if (condition.isEmpty(i)) {
                continue;
            }
            Object values = condition.getParamValue(0, i);
            int length = Array.getLength(values);
            maxArrayLength = Math.max(length, maxArrayLength);
            for (int j = 0; j < length; j++) {
                Object val = Array.get(values, j);
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
        }
        uniqueKeysSize = uniqueVals == null ? 0 : uniqueVals.size();
    }

    protected IDomain<Object> indexedDomain(IBaseCondition condition) {
        int len = condition.getNumberOfRules();
        ArrayList<Object> list = new ArrayList<>(len);
        HashSet<Object> set = new HashSet<>(len);

        for (int ruleN = 0; ruleN < len; ruleN++) {
            if (condition.isEmpty(ruleN))
                continue;
            Object ary = condition.getParamValue(0, ruleN);

            int plen = Array.getLength(ary);

            for (int j = 0; j < plen; j++) {
                Object key = Array.get(ary, j);
                if (key == null)
                    continue;
                if (!set.add(key))
                    continue;
                list.add(key);
            }
        }

        return new EnumDomain<>(list.toArray());
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.ARRAY_CONDITION_PRIORITY;
    }
}
