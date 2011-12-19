package org.openl.rules.dt.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.vm.IRuntimeEnv;

// TODO: rename to DecisionTableOptimizedAlgorithm
public class DecisionTableTrueOptimizedAlgorithm extends DecisionTableOptimizedAlgorithm {
    
    private ARuleIndex indexRoot;

    public DecisionTableTrueOptimizedAlgorithm(IConditionEvaluator[] evaluators, DecisionTable table) {
        super(evaluators, table);        
    }
    
    public void setIndexRoot(ARuleIndex indexRoot) {
        this.indexRoot = indexRoot;
    }
    
    @Override
    public IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env) {

        // Select rules set using indexed mode
        //
        ICondition[] conditions = getTable().getConditionRows();

        IIntIterator iterator = null;
        int conditionNumber = 0;
        
        ARuleIndex index = indexRoot;

        for (; conditionNumber < getEvaluators().length; conditionNumber++) {

            Object testValue = evaluateTestValue(conditions[conditionNumber], target, params, env);

            DecisionTableRuleNode node = index.findNode(testValue);

            if (!node.hasIndex()) {
                iterator = node.getRulesIterator();
                conditionNumber += 1;
                break;
            }

            index = node.getNextIndex();
        }
        
        return iterator;
    }
    

}
