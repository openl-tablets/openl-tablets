/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.domain.IntArrayIterator;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.dt.type.BooleanTypeAdaptor;
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
    public IOpenSourceCodeModule getFormalSourceCode(ICondition condition) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {

        Object value = condition.getEvaluator().invoke(target, params, env);

        return new ContainsInOrNotInArraySelector(condition, value, target, params, this.adaptor,env);
    }

    public boolean isIndexed() {
        return true;
    }

    public ARuleIndex makeIndex(Object[][] indexedParams, IIntIterator iterator) {

        if (iterator.size() < 1) {
            return null;
        }

        HashSet<Object> allValues = new HashSet<Object>();
        DecisionTableRuleNodeBuilder copyRules = new DecisionTableRuleNodeBuilder();
        List<Set<?>> valueSets = new ArrayList<Set<?>>();

        while (iterator.hasNext()) {

            int i = iterator.nextInt();
            copyRules.addRule(i);

            if (indexedParams[i] == null || indexedParams[i].length < 2 || indexedParams[i][1] == null) {
                valueSets.add(Collections.EMPTY_SET);
                continue;
            }

            HashSet<Object> values = new HashSet<Object>();
            Object valuesArray = indexedParams[i][1];

            int length = Array.getLength(valuesArray);

            for (int j = 0; j < length; j++) {
                Object value = Array.get(valuesArray, j);
                allValues.add(value);
                values.add(value);
            }

            valueSets.add(values);
        }

        int[] rules = copyRules.makeRulesAry();
        iterator = new IntArrayIterator(rules);

        HashMap<Object, DecisionTableRuleNodeBuilder> map = new HashMap<Object, DecisionTableRuleNodeBuilder>();
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        while (iterator.hasNext()) {

            int i = iterator.nextInt();

            if (indexedParams[i] == null || indexedParams[i].length < 2 || indexedParams[i][1] == null) {

                emptyBuilder.addRule(i);

                for (Iterator<DecisionTableRuleNodeBuilder> iter = map.values().iterator(); iter.hasNext();) {
                    DecisionTableRuleNodeBuilder dtrnb = iter.next();
                    dtrnb.addRule(i);
                }

                continue;
            }

            boolean isIn = indexedParams[i][0] == null || adaptor.extractBooleanValue(indexedParams[i][0]);

            Set<?> values = valueSets.get(i);

            if (isIn) {

                for (Iterator<?> iter = values.iterator(); iter.hasNext();) {

                    Object value = iter.next();
                    DecisionTableRuleNodeBuilder builder = map.get(value);

                    if (builder == null) {
                        builder = new DecisionTableRuleNodeBuilder(emptyBuilder);
                        map.put(value, builder);
                    }

                    builder.addRule(i);
                }
            } else {

                for (Iterator<?> iter = allValues.iterator(); iter.hasNext();) {

                    Object value = iter.next();

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

        HashMap<Object, DecisionTableRuleNode> nodeMap = new HashMap<Object, DecisionTableRuleNode>();

        for (Iterator<Map.Entry<Object, DecisionTableRuleNodeBuilder>> iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Object, DecisionTableRuleNodeBuilder> element = iter.next();
            nodeMap.put(element.getKey(), element.getValue().makeNode(element.getKey()));
        }

        return new EqualsIndex(emptyBuilder.makeNode("Empty"), nodeMap);
    }

    public IDomain getRuleParameterDomain(ICondition condition) throws DomainCanNotBeDefined {
        // TODO Auto-generated method stub
        return null;
    }

    public IDomain getConditionParameterDomain(int paramIdx, ICondition condition) throws DomainCanNotBeDefined {
        // TODO Auto-generated method stub
        return null;
    }

}
