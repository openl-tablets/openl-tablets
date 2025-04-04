package org.openl.rules.dt.index;

import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeDescIndex extends RangeAscIndex {

    public RangeDescIndex(DecisionTableRuleNode nextNode,
                          List<IndexNode> index,
                          IRangeAdaptor<IndexNode, ?> adaptor,
                          int[] emptyRules) {
        super(nextNode, index, adaptor, emptyRules);
    }

    @Override
    protected IndexRange retrieveIndexRange(int idx) {
        if (idx >= 0) {
            return new IndexRange(idx + 1, index.size());
        } else {
            int insertionPoint = -(idx + 1);
            if (insertionPoint < index.size()) {
                return new IndexRange(insertionPoint, index.size());
            }
        }
        return null;
    }

}
