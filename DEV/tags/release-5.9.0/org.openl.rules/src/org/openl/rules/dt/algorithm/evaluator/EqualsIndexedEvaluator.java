/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class EqualsIndexedEvaluator extends AConditionEvaluator implements IConditionEvaluator {

  
    public IOpenSourceCodeModule getFormalSourceCode(ICondition condition) {
        IOpenSourceCodeModule condSource = condition.getSourceCodeModule();
        return new StringSourceCodeModule("(" + condSource.getCode() + ") == " + condition.getParams()[0].getName(),
                condSource.getUri(0));
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);
        return new EqualsSelector(condition, value, target, dtparams, env);
    }

    public boolean isIndexed() {
        return true;
    }

    public ARuleIndex makeIndex(Object[][] indexedparams, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }

        HashMap<Object, DecisionTableRuleNodeBuilder> map = new HashMap<Object, DecisionTableRuleNodeBuilder>();
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        for (; it.hasNext();) {
            int i = it.nextInt();

            if (indexedparams[i] == null || indexedparams[i][0] == null) {
                emptyBuilder.addRule(i);

                for (Iterator<DecisionTableRuleNodeBuilder> iter = map.values().iterator(); iter.hasNext();) {
                    DecisionTableRuleNodeBuilder dtrnb = iter.next();
                    dtrnb.addRule(i);
                }

                continue;
            }

            Object value = indexedparams[i][0];
            DecisionTableRuleNodeBuilder dtrb = map.get(value);
            if (dtrb == null) {
                dtrb = new DecisionTableRuleNodeBuilder(emptyBuilder);
                map.put(value, dtrb);
            }
            dtrb.addRule(i);

        }

        HashMap<Object, DecisionTableRuleNode> nodeMap = new HashMap<Object, DecisionTableRuleNode>();

        for (Iterator<Map.Entry<Object, DecisionTableRuleNodeBuilder>> iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Object, DecisionTableRuleNodeBuilder> element = iter.next();

            nodeMap.put(element.getKey(), element.getValue().makeNode(element.getKey()));
        }

        EqualsIndex index = new EqualsIndex(emptyBuilder.makeNode("Empty"), nodeMap);

        return index;

    }



    protected IDomain<Object> indexedDomain(ICondition condition) {
        Object[][] params = condition.getParamValues();
        int len = params.length;
        ArrayList<Object> list = new ArrayList<Object>(len);
        HashSet<Object> set = new HashSet<Object>(len);
        
        for (int i = 0; i < params.length; i++) {
            Object[] pp = params[i];
            if (pp == null)
                continue;
            Object key = pp[0];
            if (key == null)
                continue;
            if (!set.add(key))
                continue;
            list.add(key);
        }
        
        EnumDomain<Object> ed = new EnumDomain<Object>(list.toArray()); 
        
        return ed;
    }
    
    
}
