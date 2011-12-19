package org.openl.rules.dt.algorithm;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.index.ARuleIndex;

public class DecisionTableIndexBuilder {
    private IConditionEvaluator[] evaluators;
    
    public DecisionTableIndexBuilder(IConditionEvaluator[] evaluators) {
        this.evaluators = evaluators.clone();
    }
    
    public ARuleIndex buildIndex(ArrayList<Object[][]> indexedParameters) {
        ARuleIndex indexRoot = null;
        
        Object[][] params0 = indexedParameters.get(0);
        indexRoot = evaluators[0].makeIndex(params0, new IntRangeDomain(0, params0.length - 1).intIterator());

        indexNodes(indexRoot, indexedParameters, 1);
        
        return indexRoot;
        
    }
    
    private void indexNodes(ARuleIndex index, ArrayList<Object[][]> params, int level) {

        if (index == null) {
            return;
        }

        if (params.size() <= level) {
            return;
        }

        Iterator<DecisionTableRuleNode> iter = index.nodes();

        while (iter.hasNext()) {

            DecisionTableRuleNode node = iter.next();
            indexNode(node, params, level);
        }

        indexNode(index.getEmptyOrFormulaNodes(), params, level);
    }
    
    private void indexNode(DecisionTableRuleNode node, ArrayList<Object[][]> params, int level) {

        ARuleIndex nodeIndex = evaluators[level].makeIndex(params.get(level), node.getRulesIterator());
        node.setNextIndex(nodeIndex);

        indexNodes(nodeIndex, params, level + 1);
    }    
}
