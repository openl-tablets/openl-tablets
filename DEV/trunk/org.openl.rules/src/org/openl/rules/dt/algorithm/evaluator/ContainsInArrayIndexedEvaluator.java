/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public class ContainsInArrayIndexedEvaluator implements IConditionEvaluator {

    public IOpenSourceCodeModule getFormalSourceCode(ICondition condition) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] params, IRuntimeEnv env) {

        Object value = condition.getEvaluator().invoke(target, params, env);

        return new ContainsInArraySelector(condition, value, target, params, env);
    }

    public boolean isIndexed() {
        return true;
    }

    public ARuleIndex makeIndex(Object[][] indexedParams, IIntIterator iterator) {

        if (iterator.size() < 1) {
            return null;
        }

        HashMap<Object, DecisionTableRuleNodeBuilder> map = new HashMap<Object, DecisionTableRuleNodeBuilder>();
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        while (iterator.hasNext()) {

            int i = iterator.nextInt();

            if (indexedParams[i] == null || indexedParams[i][0] == null) {

                emptyBuilder.addRule(i);

                for (Iterator<DecisionTableRuleNodeBuilder> iter = map.values().iterator(); iter.hasNext();) {
                    DecisionTableRuleNodeBuilder builder = iter.next();
                    builder.addRule(i);
                }

                continue;
            }

            Object values = indexedParams[i][0];

            int length = Array.getLength(values);

            for (int j = 0; j < length; j++) {

                Object value = Array.get(values, j);

                DecisionTableRuleNodeBuilder builder = (DecisionTableRuleNodeBuilder) map.get(value);

                if (builder == null) {
                    builder = new DecisionTableRuleNodeBuilder(emptyBuilder);
                    map.put(value, builder);
                }

                builder.addRule(i);
            }
        }

        HashMap<Object, DecisionTableRuleNode> nodeMap = new HashMap<Object, DecisionTableRuleNode>();

        for (Iterator<Map.Entry<Object, DecisionTableRuleNodeBuilder>> iter = map.entrySet().iterator(); iter.hasNext();) {

            Map.Entry<Object, DecisionTableRuleNodeBuilder> element = iter.next();
            nodeMap.put(element.getKey(), ((DecisionTableRuleNodeBuilder) element.getValue()).makeNode(element.getKey()));
        }

        return new EqualsIndex(emptyBuilder.makeNode("Empty"), nodeMap);
    }

}
