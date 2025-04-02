package org.openl.rules.dt.index;

import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeDescIndex extends RangeAscIndex {

    /**
     * Constructs a new RangeDescIndex instance for descending decision table indexing.
     *
     * <p>This constructor initializes the index by passing the provided decision table node chain,
     * index nodes, range adaptor, and empty rules to the superclass constructor.
     *
     * @param nextNode the next decision table rule node
     * @param index a list of index nodes used for decision table indexing
     * @param adaptor the range adaptor for handling node value conversions
     * @param emptyRules an array representing rules that yield empty results
     */
    public RangeDescIndex(DecisionTableRuleNode nextNode,
                          List<IndexNode> index,
                          IRangeAdaptor<IndexNode, ?> adaptor,
                          int[] emptyRules) {
        super(nextNode, index, adaptor, emptyRules);
    }

    /**
     * Retrieves the index range for decision table indexing in descending order based on the given index.
     * <p>
     * If {@code idx} is non-negative, returns an {@link IndexRange} starting from {@code idx + 1} up to the end
     * of the index list. If {@code idx} is negative, calculates an insertion point as {@code -(idx + 1)} and, if this
     * point is within the index size, returns an {@link IndexRange} from that insertion point to the end. Otherwise,
     * returns {@code null} if no valid range is available.
     * </p>
     *
     * @param idx the index value used to determine the subrange; a non-negative value indicates a found position,
     *            while a negative value implies an insertion point.
     * @return an {@link IndexRange} representing the subrange, or {@code null} if no valid subrange exists.
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
