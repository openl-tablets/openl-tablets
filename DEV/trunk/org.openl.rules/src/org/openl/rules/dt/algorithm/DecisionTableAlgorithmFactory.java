package org.openl.rules.dt.algorithm;

import java.util.ArrayList;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.syntax.exception.SyntaxNodeException;

public class DecisionTableAlgorithmFactory {
    
    private DecisionTableAlgorithmFactory(){}
    
    public static DecisionTableOptimizedAlgorithm getAlgorithm(IConditionEvaluator[] evaluators, DecisionTable decisionTable) 
            throws SyntaxNodeException {
        ArrayList<Object[][]> indexedParameters = DecisionTableAlgorithmHelper.getIndexedParameters(evaluators, decisionTable);
        
        if (indexedParameters.size() > 0) {
            ARuleIndex indexRoot = new DecisionTableIndexBuilder(evaluators).buildIndex(indexedParameters);
            
            DecisionTableTrueOptimizedAlgorithm dtOptimizedAlgorithm = new DecisionTableTrueOptimizedAlgorithm(evaluators, decisionTable);
            dtOptimizedAlgorithm.setIndexRoot(indexRoot);
            return dtOptimizedAlgorithm;
        }
        return new DecisionTableOptimizedAlgorithm(evaluators, decisionTable);
    }
}
