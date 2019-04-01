package org.openl.rules.dt.index;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeAscIndex implements IRuleIndex {

    protected final List<IndexNode> index;
    private final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    private final DecisionTableRuleNode nextNode;
    private final IRangeAdaptor<IndexNode, ?> adaptor;
    private final Set<Integer> emptyRules;

    public RangeAscIndex(DecisionTableRuleNode nextNode,
            List<IndexNode> index,
            IRangeAdaptor<IndexNode, ?> adaptor,
            int[] emptyRules) {
        this.index = Collections.unmodifiableList(index);
        this.adaptor = adaptor;
        this.nextNode = nextNode;

        Set<Integer> emptyRuleSet = new HashSet<>();
        for (int i : emptyRules) {
            emptyRuleSet.add(i);
        }
        this.emptyRules = Collections.unmodifiableSet(emptyRuleSet);
    }

    private Pair<Integer, Integer> findIndexRange(Object value) {
        if (value == null || index.isEmpty()) {
            // there is no values in index to compare => no reason to search
            return null;
        }
        // Converts value for binary search in index
        // Because different subclasses of Number are not comparable.
        value = adaptor.adaptValueType(value);
        int idx = Collections.binarySearch(index, (IndexNode) value);
        return retrieveIndexRange(idx);
    }

    protected Pair<Integer, Integer> retrieveIndexRange(int idx) {
        if (idx >= 0) {
            return Pair.of(0, idx + 1);
        } else {
            int insertionPoint = -(idx + 1);
            if (insertionPoint <= index.size() && insertionPoint > 0) {
                return Pair.of(0, insertionPoint);
            }
        }
        return null;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new RangeIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    Set<Integer> findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof RangeIndexDecisionTableRuleNode)) {
            Pair<Integer, Integer> range = findIndexRange(value);
            Set<Integer> result = new HashSet<>(emptyRules);
            if (range != null) {
                for (int i = range.getLeft(); i < range.getRight(); i++) {
                    result.addAll(index.get(i).getRules());
                }
            }
            return result;
        }
        Set<Integer> prevRes = ((RangeIndexDecisionTableRuleNode) prevResult).getRuleSet();
        if (prevRes.isEmpty()) {
            return prevRes;
        }
        Pair<Integer, Integer> range = findIndexRange(value);
        Set<Integer> result = new HashSet<>();
        retainAll(emptyRules, prevRes, result);
        if (range != null) {
            for (int i = range.getLeft(); i < range.getRight(); i++) {
                retainAll(index.get(i).getRules(), prevRes, result);
            }
        }
        return result;
    }

    private void retainAll(Set<Integer> a, Set<Integer> b, Set<Integer> result) {
        if (a.size() > b.size()) {
            Set<Integer> tmp = a;
            a = b;
            b = tmp;
        }
        for (Integer ruleN : a) {
            if (b.contains(ruleN)) {
                result.add(ruleN);
            }
        }
    }

    @Override
    public Iterable<? extends DecisionTableRuleNode> nodes() {
        return Collections.singletonList(nextNode);
    }

    @Override
    public DecisionTableRuleNode getEmptyOrFormulaNodes() {
        return emptyNodeStub;
    }

    @Override
    public int[] collectRules() {
        List<Integer> rules = new ArrayList<>(emptyRules);
        for (IndexNode indexNode : index) {
            rules.addAll(indexNode.getRules());
        }
        int[] result = new int[rules.size()];
        int i = 0;
        for (Integer ruleN : rules) {
            result[i++] = ruleN;
        }
        return result;
    }
}
