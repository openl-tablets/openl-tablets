package org.openl.rules.dt.index;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeDescIndex extends RangeAscIndex {

    public RangeDescIndex(DecisionTableRuleNode nextNode, List<IndexNode> index, IRangeAdaptor<IndexNode, ?> adaptor, int[] emptyRules) {
        super(nextNode, index, adaptor, emptyRules);
    }

    @Override
    protected Set<Integer> retrieveRuleSet(int idx) {
        if (idx >= 0) {
            return getRulesStartFrom(idx + 1);
        } else {
            int insertionPoint = -(idx + 1);
            if (insertionPoint < index.size() && insertionPoint >= 0) {
                return getRulesStartFrom(insertionPoint);
            }
        }
        return null;
    }

    private Set<Integer> getRulesStartFrom(int startIdx) {
        Set<Integer> result = new HashSet<>();
        for (int i = startIdx; i < index.size(); i++) {
            result.addAll(index.get(i).getRules());
        }
        return result;
    }
}
