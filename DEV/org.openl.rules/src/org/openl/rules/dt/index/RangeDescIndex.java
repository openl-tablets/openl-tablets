package org.openl.rules.dt.index;

import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeDescIndex extends RangeAscIndex {

    /**
     * Constructs a new RangeDescIndex instance for descending order decision table indexing.
     *
     * This constructor initializes the index by passing the decision table rule node, the list of index nodes,
     * the range adaptor, and the empty rules array to its superclass, configuring the instance for handling
     * descending index ranges.
     *
     * @param nextNode the decision table rule node representing the next index in the decision table structure
     * @param index the list of index nodes used to build the index range
     * @param adaptor the range adaptor that facilitates index node comparisons and range operations
     * @param emptyRules an array of rule indices that represent empty decision outcomes
     */
    public RangeDescIndex(DecisionTableRuleNode nextNode,
                          List<IndexNode> index,
                          IRangeAdaptor<IndexNode, ?> adaptor,
                          int[] emptyRules) {
        super(nextNode, index, adaptor, emptyRules);
    }

    /**
     * Computes the applicable index range for descending order decision table indexing.
     *
     * If the provided index is non-negative, the range starts at (idx + 1) and extends to the end of the index list.
     * For a negative index (typically a binary search result), the insertion point is computed as -(idx + 1). If this
     * insertion point is within the bounds of the index list, the range starts at the insertion point and extends to its end.
     * Returns null if no valid range is determined.
     *
     * @param idx the index value or binary search result where negative values indicate an insertion point
     * @return the computed IndexRange or null if the insertion point is out of bounds
     */
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
