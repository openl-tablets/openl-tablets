/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.IOpenSourceCodeModule;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.domain.IntArrayIterator;
import org.openl.rules.dt.ADTRuleIndex.DTRuleNode;
import org.openl.rules.dt.ADTRuleIndex.DTRuleNodeBuilder;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ContainsInOrNotInAryIndexedEvaluator implements IDTConditionEvaluator {

    static class ContainsInAryIndex extends ADTRuleIndex {

        HashMap<Object, DTRuleNode> valueNodes = new HashMap<Object, DTRuleNode>();

        public ContainsInAryIndex(DTRuleNode emptyOrFormulaNodes, HashMap<Object, DTRuleNode> valueNodes) {
            super(emptyOrFormulaNodes);
            this.valueNodes = valueNodes;
        }

        @Override
        public DTRuleNode findNodeInIndex(Object value) {
            if (value != null) {
                DTRuleNode node = valueNodes.get(value);
                return node;
            }

            return null;
        }

        @Override
        public Iterator<DTRuleNode> nodes() {
            return valueNodes.values().iterator();
        }
    }

    class ContainsInOrNotInArySelector implements IIntSelector {
        IDTCondition condition;
        Object value;
        Object target;
        Object[] dtparams;
        IRuntimeEnv env;

        public ContainsInOrNotInArySelector(IDTCondition condition, Object value, Object target, Object[] dtparams,
                IRuntimeEnv env) {
            this.condition = condition;
            this.value = value;
            this.dtparams = dtparams;
            this.env = env;
            this.target = target;
        }

        public boolean select(int rule) {
            Object[][] params = condition.getParamValues();
            Object[] ruleParams = params[rule];

            if (ruleParams == null) {
                return true;
            }

            Object[] realParams = new Object[params.length];

            FunctionalRow.loadParams(realParams, 0, ruleParams, target, dtparams, env);

            if (ruleParams.length < 2 || ruleParams[1] == null) {
                return true;
            }

            boolean isin = ruleParams[0] == null || adaptor.extractBooleanValue(ruleParams[0]);
            return ArrayTool.contains(ruleParams[1], value) ^ isin;
        }
    }

    BooleanTypeAdaptor adaptor;

    public ContainsInOrNotInAryIndexedEvaluator(BooleanTypeAdaptor adaptor) {
        this.adaptor = adaptor;
    }

    // TODO fix
    public IOpenSourceCodeModule getFormalSourceCode(IDTCondition condition) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public IIntSelector getSelector(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);
        return new ContainsInOrNotInArySelector(condition, value, target, dtparams, env);
    }

    public boolean isIndexed() {
        return true;
    }

    public ADTRuleIndex makeIndex(Object[][] indexedparams, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }

        HashSet<Object> allValues = new HashSet<Object>();
        DTRuleNodeBuilder copyRules = new DTRuleNodeBuilder();
        List<Set<?>> valueSets = new ArrayList<Set<?>>();

        for (; it.hasNext();) {
            int i = it.nextInt();
            copyRules.addRule(i);

            if (indexedparams[i] == null || indexedparams[i].length < 2 || indexedparams[i][1] == null) {
                valueSets.add(Collections.EMPTY_SET);
                continue;
            }
            HashSet<Object> values = new HashSet<Object>();
            Object valuesAry = indexedparams[i][1];

            int len = Array.getLength(valuesAry);
            for (int j = 0; j < len; j++) {
                Object value = Array.get(valuesAry, j);
                allValues.add(value);
                values.add(value);
            }
            valueSets.add(values);
        }

        int[] rules = copyRules.makeRulesAry();
        it = new IntArrayIterator(rules);

        HashMap<Object, DTRuleNodeBuilder> map = new HashMap<Object, DTRuleNodeBuilder>();
        DTRuleNodeBuilder emptyBuilder = new DTRuleNodeBuilder();

        for (; it.hasNext();) {
            int i = it.nextInt();

            if (indexedparams[i] == null || indexedparams[i].length < 2 || indexedparams[i][1] == null) {
                emptyBuilder.addRule(i);

                for (Iterator<DTRuleNodeBuilder> iter = map.values().iterator(); iter.hasNext();) {
                    DTRuleNodeBuilder dtrnb = iter.next();
                    dtrnb.addRule(i);
                }

                continue;
            }

            boolean isin = indexedparams[i][0] == null || adaptor.extractBooleanValue(indexedparams[i][0]);

            Set<?> values = valueSets.get(i);

            if (isin) {
                for (Iterator<?> iter = values.iterator(); iter.hasNext();) {
                    Object value = iter.next();

                    DTRuleNodeBuilder dtrb = map.get(value);
                    if (dtrb == null) {
                        dtrb = new DTRuleNodeBuilder(emptyBuilder);
                        map.put(value, dtrb);
                    }
                    dtrb.addRule(i);
                }
            } else {
                for (Iterator<?> iter = allValues.iterator(); iter.hasNext();) {
                    Object value = iter.next();
                    if (values.contains(value)) {
                        continue;
                    }

                    DTRuleNodeBuilder dtrb = map.get(value);
                    if (dtrb == null) {
                        dtrb = new DTRuleNodeBuilder(emptyBuilder);
                        map.put(value, dtrb);
                    }
                    dtrb.addRule(i);
                }

                emptyBuilder.addRule(i); // !!!!!

            }

        }

        HashMap<Object, DTRuleNode> nodeMap = new HashMap<Object, DTRuleNode>();

        for (Iterator<Map.Entry<Object, DTRuleNodeBuilder>> iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Object, DTRuleNodeBuilder> element = iter.next();

            nodeMap.put(element.getKey(), element.getValue().makeNode());
        }

        ContainsInAryIndex index = new ContainsInAryIndex(emptyBuilder.makeNode(), nodeMap);

        return index;

    }

}
