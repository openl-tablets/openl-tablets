/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.algorithm.evaluator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.dt.IBaseCondition;
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

    public EqualsIndexedEvaluator() {
    }

    public EqualsIndexedEvaluator(IOpenCast openCast) {
        this.openCast = openCast;
    }

    public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
        IOpenSourceCodeModule condSource = condition.getSourceCodeModule();
        return new StringSourceCodeModule("(" + condSource.getCode() + ") == " + condition.getParams()[0].getName(),
            condSource.getUri());
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);
        return new EqualsSelector(condition, value, target, dtparams, env);
    }

    public boolean isIndexed() {
        return true;
    }

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
                    for (Iterator<DecisionTableRuleNodeBuilder> iter = map.values().iterator(); iter.hasNext();) {
                        DecisionTableRuleNodeBuilder dtrnb = iter.next();
                        dtrnb.addRule(i);
                    }
                }
                continue;
            }

            Object value = condition.getParamValue(0, i);
            if (openCast != null) {
                value = openCast.convert(value);
            }
            if (comparatorBasedMap) {
                if (!(value instanceof Comparable<?>)) {
                    throw new IllegalArgumentException("Invalid state! Index based on comparable interface!");
                }
            }
            if (map == null) {
                if (NumberUtils.isFloatPointNumber(value)) {
                    if (value instanceof BigDecimal) {
                        map = new TreeMap<Object, DecisionTableRuleNodeBuilder>();
                        nodeMap = new TreeMap<Object, DecisionTableRuleNode>();
                    } else {
                        map = new TreeMap<Object, DecisionTableRuleNodeBuilder>(FloatTypeComparator.getInstance());
                        nodeMap = new TreeMap<Object, DecisionTableRuleNode>(FloatTypeComparator.getInstance());
                    }
                    comparatorBasedMap = true;
                } else {
                    map = new HashMap<Object, DecisionTableRuleNodeBuilder>();
                    nodeMap = new HashMap<Object, DecisionTableRuleNode>();
                }
            }
            DecisionTableRuleNodeBuilder dtrb = map.get(value);
            if (dtrb == null) {
                dtrb = new DecisionTableRuleNodeBuilder(emptyBuilder);
                map.put(value, dtrb);
            }
            dtrb.addRule(i);

        }
        if (map != null) {
            for (Iterator<Map.Entry<Object, DecisionTableRuleNodeBuilder>> iter = map.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<Object, DecisionTableRuleNodeBuilder> element = iter.next();
                nodeMap.put(element.getKey(), element.getValue().makeNode());
            }
        } else {
            nodeMap = Collections.emptyMap();
        }

        EqualsIndex index = new EqualsIndex(emptyBuilder.makeNode(), nodeMap);

        return index;

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

    protected IDomain<Object> indexedDomain(IBaseCondition condition) {
        int len = condition.getNumberOfRules();
        ArrayList<Object> list = new ArrayList<Object>(len);
        HashSet<Object> set = new HashSet<Object>(len);

        for (int ruleN = 0; ruleN < len; ruleN++) {
            if (condition.isEmpty(ruleN))
                continue;
            Object key = condition.getParamValue(0, ruleN);
            if (key == null)
                continue;
            if (!set.add(key))
                continue;
            list.add(key);
        }

        EnumDomain<Object> ed = new EnumDomain<Object>(list.toArray());

        return ed;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
