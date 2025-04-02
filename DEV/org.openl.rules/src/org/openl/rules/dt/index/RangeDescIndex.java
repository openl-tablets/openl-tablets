package org.openl.rules.dt.index;

import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeDescIndex extends RangeAscIndex {

    /**
     * Constructs a RangeDescIndex for descending order decision table indexing.
     *
     * <p>This constructor initializes the RangeDescIndex instance by passing the specified decision table rule node,
     * index list, range adaptor, and empty rule indices array to its superclass.</p>
     *
     * @param nextNode  the decision table rule node representing the subsequent rule chain segment
     * @param index     the list of index nodes used for decision table indexing
     * @param adaptor   the range adaptor for managing range operations on the index nodes
     * @param emptyRules the array of rule identifiers corresponding to empty rules
     */
    public RangeDescIndex(DecisionTableRuleNode nextNode,
                          List<IndexNode> index,
                          IRangeAdaptor<IndexNode, ?> adaptor,
                          int[] emptyRules) {
        super(nextNode, index, adaptor, emptyRules);
    }

    /**
     * Retrieves an index range from the underlying index list based on the given search index.
     *
     * <p>If the provided index is non-negative, the method returns an {@code IndexRange} starting
     * from the element immediately after the given index up to the end of the list. If the index is negative,
     * the negative value is used to compute the insertion point (as {@code -(idx + 1)}). If this insertion
     * point is within the bounds of the list, an {@code IndexRange} is returned starting at the insertion point;
     * otherwise, {@code null} is returned.</p>
     *
     * @param idx the index from a search operation (or its negative counterpart if not found)
     * @return an {@code IndexRange} from either {@code idx + 1} or the computed insertion point to the end of the list,
     *         or {@code null} if the insertion point is beyond the end of the list
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
