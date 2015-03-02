package org.openl.rules.dt2.algorithm;

import java.util.Iterator;

import org.openl.rules.dt2.DecisionTable;
import org.openl.rules.dt2.DecisionTableRuleNode;
import org.openl.rules.dt2.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt2.element.ICondition;
import org.openl.rules.dt2.index.ARuleIndex;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.syntax.exception.SyntaxNodeException;

public class DecisionTableAlgorithmBuilder {
	
	private IndexInfo baseInfo;
	private DecisionTable table;
    private IConditionEvaluator[] evaluators;
	
	public DecisionTableAlgorithmBuilder(IndexInfo info, IConditionEvaluator[] evaluators)
	{
		this.baseInfo = info;
		this.table = info.table;
		this.evaluators = evaluators;
		
	}

	
	
	

	
    protected ARuleIndex buildIndex(IndexInfo info) throws SyntaxNodeException {

        int first = info.fromCondition;
        IBaseCondition[] cc = table.getConditionRows();
        
        if (cc.length <= first || first > info.toCondition)
        	return null;
        
        ICondition firstCondition =  (ICondition)cc[first];

        if (!canIndex(evaluators[first], firstCondition))
        	return null; 

        ARuleIndex indexRoot = evaluators[first].makeIndex(firstCondition, info.makeRuleIterator());
//        indexRoot.setHasMetaInfo(saveRulesMetaInfo);

        indexNodes(indexRoot, first + 1, info);
        
        return indexRoot;
    }


    
    
    
    
    private void indexNodes(ARuleIndex index, int condN, IndexInfo info) {
    	
    	if (index == null || condN > info.toCondition)
    		return;
    	
        if (!canIndex(evaluators[condN], table.getCondition(condN))) {
            return;
        }
        
        Iterator<DecisionTableRuleNode> iter = index.nodes();
        while (iter.hasNext()) {
            DecisionTableRuleNode node = iter.next();
            indexNode(node,  condN, info);
        }
        indexNode(index.getEmptyOrFormulaNodes(),  condN, info);
    }

    private void indexNode(DecisionTableRuleNode node, int condN, IndexInfo info) {

        ARuleIndex nodeIndex = evaluators[condN].makeIndex(table.getCondition(condN), node.getRulesIterator());
//        node.setSaveRulesMetaInfo(saveRulesMetaInfo);
        node.setNextIndex(nodeIndex);

        indexNodes(nodeIndex, condN + 1, info);
    }
    

	private boolean canIndex(IConditionEvaluator evaluator,
			ICondition condition) {
		return evaluator.isIndexed() && !condition.hasFormulasInStorage();
	}


	public IDecisionTableAlgorithm buildAlgorithm() throws SyntaxNodeException {
		if (isTwoDimentional(table))
		{
			DecisionTableOptimizedAlgorithm va = makeVerticalAlgorithm();
			DecisionTableOptimizedAlgorithm ha = makeHorizontalAlgorithm();
			TwoDimensionalAlgorithm twoD = new TwoDimensionalAlgorithm(va, ha);
			return twoD;
		}	

		return makeFullAlgorithm();
		
	}

	
	
	



	private DecisionTableOptimizedAlgorithm makeHorizontalAlgorithm() throws SyntaxNodeException {
		
		IndexInfo hInfo = baseInfo.makeHorizontalalInfo();
		
		ARuleIndex index = buildIndex(hInfo);
		DecisionTableOptimizedAlgorithm alg = new DecisionTableOptimizedAlgorithm(evaluators, table, hInfo, index);
		
		return alg;
	}






	private IDecisionTableAlgorithm makeFullAlgorithm() throws SyntaxNodeException {
		ARuleIndex index = buildIndex(baseInfo);
		DecisionTableOptimizedAlgorithm alg = new DecisionTableOptimizedAlgorithm(evaluators, table, baseInfo, index);
		
		return alg;
	}




	private DecisionTableOptimizedAlgorithm makeVerticalAlgorithm() throws SyntaxNodeException {
		
		IndexInfo vInfo = baseInfo.makeVerticalInfo();
		
		ARuleIndex index = buildIndex(vInfo);
		DecisionTableOptimizedAlgorithm alg = new DecisionTableOptimizedAlgorithm(evaluators, table, vInfo, index);
		
		return alg;
	}




	private boolean isTwoDimentional(DecisionTable table2) {
		return table.getDtInfo().getNumberHConditions() > 0;
	}

}
