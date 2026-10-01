package org.openl.rules.dt.index;

import java.util.List;
import java.util.function.Function;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;

public class RangeDescIndex extends RangeAscIndex {

    public RangeDescIndex(DecisionTableRuleNode nextNode,
                          List<IndexNode> index,
                          Function<Object, IndexNode> toIndexNode,
                          int[] emptyRules) {
        super(nextNode, index, toIndexNode, emptyRules);
    }

    @Override
    protected IndexRange retrieveIndexRange(int idx) {
        if (idx >= 0) {
            return new IndexRange(idx + 1, index.size());
        } else {
            var insertionPoint = -(idx + 1);
            if (insertionPoint < index.size()) {
                return new IndexRange(insertionPoint, index.size());
            }
        }
        return null;
    }

}
