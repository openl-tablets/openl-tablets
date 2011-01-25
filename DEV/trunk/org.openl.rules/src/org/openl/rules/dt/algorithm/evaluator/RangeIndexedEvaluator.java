/**
 * Created Jul 22, 2007
 */
package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.util.IntervalMap;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class RangeIndexedEvaluator implements IConditionEvaluator {

    private IRangeAdaptor<Object, Object> adaptor;

    public RangeIndexedEvaluator(IRangeAdaptor<Object, Object> adaptor) {
        this.adaptor = adaptor;
    }

    public IOpenSourceCodeModule getFormalSourceCode(ICondition condition) {

        IOpenSourceCodeModule conditionSource = condition.getSourceCodeModule();
        String name = condition.getParams()[1].getName();
        String code = String.format("%1$s<=(%2$s) && (%2$s) < %1$s", name, conditionSource.getCode());

        return new StringSourceCodeModule(code, conditionSource.getUri(0));
    }

    public IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env) {
        Object value = condition.getEvaluator().invoke(target, dtparams, env);
        if (value instanceof Number) {            
            return new RangeSelector(condition, (Number)value, target, dtparams, adaptor, env);    
        } 
        String errorMessage = String.format("Evaluation result for condition %s in method %s must be a numeric value", 
            condition.getName(), condition.getMethod().getName());
        throw new OpenLRuntimeException(errorMessage);
        
    }

    public boolean isIndexed() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public ARuleIndex makeIndex(Object[][] indexedparams, IIntIterator iterator) {

        if (iterator.size() < 1) {
            return null;
        }

        IntervalMap<Object, Integer> map = new IntervalMap<Object, Integer>();
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();

        while (iterator.hasNext()) {

            int i = iterator.nextInt();

            if (indexedparams[i] == null || indexedparams[i][0] == null) {

                emptyBuilder.addRule(i);
                continue;
            }

            Comparable<Object> vFrom = null;
            Comparable<Object> vTo = null;

            if (adaptor == null) {
                vFrom = (Comparable<Object>) indexedparams[i][0];
                vTo = (Comparable<Object>) indexedparams[i][1];
            } else {
                vFrom = adaptor.getMin(indexedparams[i][0]);
                vTo = adaptor.getMax(indexedparams[i][0]);
            }

            // TODO fix null from and To, last could be replaces with very big
            // value
            // TODO DoubleRange

            map.putInterval(vFrom, vTo, new Integer(i));
        }

        TreeMap<Comparable<Object>, List<Integer>> treeMap = map.treeMap();

        List<Comparable<?>> index = new ArrayList<Comparable<?>>();
        List<DecisionTableRuleNode> rules = new ArrayList<DecisionTableRuleNode>();

        int i = 0;

        DecisionTableRuleNode emptyNode = emptyBuilder.makeNode("Empty");

        for (Iterator<Map.Entry<Comparable<Object>, List<Integer>>> iter = treeMap.entrySet().iterator(); iter.hasNext(); ++i) {

            Map.Entry<Comparable<Object>, List<Integer>> element = iter.next();
            Comparable<?> indexObj = element.getKey();
            List<Integer> list = element.getValue();

            if (emptyNode.getRules().length > 0)
                list = merge(list, emptyNode.getRules());

            int[] idxAry = new int[list.size()];

            for (int j = 0; j < idxAry.length; j++) {
                idxAry[j] = list.get(j);
            }

            rules.add(new DecisionTableRuleNode(idxAry));
            index.add(indexObj);
        }

        return new RangeIndex(emptyNode, index.toArray(new Comparable[0]), rules.toArray(new DecisionTableRuleNode[0]), adaptor);
    }

    private List<Integer> merge(List<Integer> list, int[] rules) {

        int idx1 = 0;
        int idx2 = 0;

        int N = list.size() + rules.length;
        List<Integer> newList = new ArrayList<Integer>(N);

        for (int i = 0; i < N; i++) {
            if (idx1 == list.size()) {
                newList.add(rules[idx2++]);
                continue;
            }
            if (idx2 == rules.length) {
                newList.add(list.get(idx1++));
                continue;
            }
            int i1 = list.get(idx1);
            int i2 = rules[idx2];

            if (i1 < i2) {
                newList.add(i1);
                idx1++;
            } else {
                newList.add(i2);
                idx2++;
            }
        }

        return newList;
    }

}
