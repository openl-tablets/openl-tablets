/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.IOpenSourceCodeModule;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.ADTRuleIndex.DTRuleNodeBuilder;
import org.openl.util.ArrayTool;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ContainsInAryIndexedEvaluator implements IDTConditionEvaluator {

    static class ContainsInAryIndex extends ADTRuleIndex {

        HashMap valueNodes = new HashMap();

        public ContainsInAryIndex(DTRuleNode emptyOrFormulaNodes, HashMap valueNodes) {
            super(emptyOrFormulaNodes);
            this.valueNodes = valueNodes;
        }

        @Override
        public DTRuleNode findNodeInIndex(Object value) {
            if (value != null) {
                DTRuleNode node = (DTRuleNode) valueNodes.get(value);
                return node;
            }

            return null;
        }

        @Override
        public Iterator nodes() {
            return valueNodes.values().iterator();
        }
    }

    static class ContainsInArySelector implements IIntSelector {
        IDTCondition condition;
        Object value;
        Object target;
        Object[] dtparams;
        IRuntimeEnv env;

        public ContainsInArySelector(IDTCondition condition, Object value, Object target, Object[] dtparams,
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

            if (ruleParams[0] == null) {
                return true;
            }

            return ArrayTool.contains(ruleParams[0], value);
        }
    }

    // TODO fix
    public IOpenSourceCodeModule getFormalSourceCode(IDTCondition condition) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public IIntSelector getSelector(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);
        return new ContainsInArySelector(condition, value, target, dtparams, env);
    }

    public boolean isIndexed() {
        return true;
    }

    public ADTRuleIndex makeIndex(Object[][] indexedparams, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }

        HashMap map = new HashMap();
        DTRuleNodeBuilder emptyBuilder = new DTRuleNodeBuilder();

        for (; it.hasNext();) {
            int i = it.nextInt();

            if (indexedparams[i] == null || indexedparams[i][0] == null) {
                emptyBuilder.addRule(i);

                for (Iterator iter = map.values().iterator(); iter.hasNext();) {
                    DTRuleNodeBuilder dtrnb = (DTRuleNodeBuilder) iter.next();
                    dtrnb.addRule(i);
                }

                continue;
            }

            Object valuesAry = indexedparams[i][0];

            int len = Array.getLength(valuesAry);
            for (int j = 0; j < len; j++) {
                Object value = Array.get(valuesAry, j);
                DTRuleNodeBuilder dtrb = (DTRuleNodeBuilder) map.get(value);
                if (dtrb == null) {
                    dtrb = new DTRuleNodeBuilder(emptyBuilder);
                    map.put(value, dtrb);
                }
                dtrb.addRule(i);
            }

        }

        HashMap nodeMap = new HashMap();

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry element = (Map.Entry) iter.next();

            nodeMap.put(element.getKey(), ((DTRuleNodeBuilder) element.getValue()).makeNode());
        }

        ContainsInAryIndex index = new ContainsInAryIndex(emptyBuilder.makeNode(), nodeMap);

        return index;

    }

}
