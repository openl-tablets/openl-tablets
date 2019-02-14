package org.openl.rules.dt.index;

import java.util.*;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator.IndexNode;
import org.openl.rules.dt.type.IRangeAdaptor;

public class RangeAscIndex implements IRuleIndex {

    private final DecisionTableRuleNode emptyNodeStub = new DecisionTableRuleNodeBuilder().makeNode();
    private final DecisionTableRuleNode nextNode;
    private final IRangeAdaptor<IndexNode, ?> adaptor;
    protected final List<IndexNode> index;
    protected Set<Integer> emptyRules;

    public RangeAscIndex(DecisionTableRuleNode nextNode, List<IndexNode> index, IRangeAdaptor<IndexNode, ?> adaptor, int[] emptyRules) {
        this.index = Collections.unmodifiableList(index);
        this.adaptor = adaptor;
        this.nextNode = nextNode;

        Set<Integer> emptyRuleSet = new HashSet<>();
        for (int i : emptyRules) {
            emptyRuleSet.add(i);
        }
        this.emptyRules = Collections.unmodifiableSet(emptyRuleSet);
    }

    private Set<Integer> findRules(Object value) {
        if (value == null || index.isEmpty()) {
            // there is no values in index to compare => no reason to search
            return new HashSet<>(emptyRules);
        }
        // Converts value for binary search in index
        // Because different subclasses of Number are not comparable.
        value = adaptor.adaptValueType(value);
        int idx = Collections.binarySearch(index, (IndexNode) value);
        Set<Integer> rules = retrieveRuleSet(idx);
        if (rules == null) {
            return new HashSet<>(emptyRules);
        }
        rules.addAll(emptyRules);
        return rules;
    }

    protected Set<Integer> retrieveRuleSet(int idx) {
        if (idx >= 0) {
            return getRulesTillTo(idx);
        } else {
            int insertionPoint = -(idx + 1);
            if (insertionPoint <= index.size() && insertionPoint > 0) {
                return getRulesTillTo(insertionPoint - 1);
            }
        }
        return null;
    }

    private Set<Integer> getRulesTillTo(int endIdx) {
        Set<Integer> result = new HashSet<>();
        for (int i = 0; i <= endIdx; i++) {
            result.addAll(index.get(i).getRules());
        }
        return result;
    }

    @Override
    public DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        return new RangeIndexDecisionTableRuleNode(findRules(value, prevResult), nextNode.getNextIndex());
    }

    Set<Integer> findRules(Object value, DecisionTableRuleNode prevResult) {
        if (!(prevResult instanceof RangeIndexDecisionTableRuleNode)) {
            return findRules(value);
        }
        Set<Integer> prevRes = ((RangeIndexDecisionTableRuleNode) prevResult).getRuleSet();
        if (prevRes.isEmpty()) {
            return prevRes;
        }
        Set<Integer> result = findRules(value);
        if (result.size() <= prevRes.size()) {
            result.retainAll(prevRes);
            return result;
        }
        prevRes.retainAll(result);
        return prevRes;
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
