package org.openl.rules.dt.algorithm.evaluator;

import java.math.BigDecimal;
import java.util.*;

import org.openl.binding.impl.cast.IOpenCast;
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
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class EqualsIndexedEvaluator extends AConditionEvaluator implements IConditionEvaluator {

    private IOpenCast openCast;

    public EqualsIndexedEvaluator(IOpenCast openCast) {
        this.openCast = openCast;
    }

    @Override
    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        IOpenSourceCodeModule condSource = condition.getSourceCodeModule();
        return new StringSourceCodeModule("(" + condSource.getCode() + ") == " + condition.getParams()[0].getName(),
            condSource.getUri());
    }

    @Override
    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);
        return new EqualsSelector(condition, value, target, dtparams, env);
    }

    @Override
    public boolean isIndexed() {
        return true;
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
    protected IDomain<Object> indexedDomain(IBaseCondition condition) {
        int len = condition.getNumberOfRules();
        ArrayList<Object> list = new ArrayList<>(len);
        HashSet<Object> set = new HashSet<>(len);

        for (int ruleN = 0; ruleN < len; ruleN++) {
            if (condition.isEmpty(ruleN)) {
                continue;
            }
            Object key = condition.getParamValue(0, ruleN);
            if (key == null || !set.add(key)) {
                continue;
            }
            list.add(key);
        }

        return new EnumDomain<>(list.toArray());
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.EQUALS_CONDITION_PRIORITY;
    }
}
