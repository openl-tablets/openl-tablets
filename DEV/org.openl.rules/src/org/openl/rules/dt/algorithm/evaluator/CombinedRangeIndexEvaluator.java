package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.List;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.CombinedRangeIndex;
import org.openl.rules.dt.index.IRuleIndex;
import org.openl.rules.dt.index.RangeAscIndex;
import org.openl.rules.dt.index.RangeDescIndex;
import org.openl.rules.dt.type.IRangeAdaptor;

public class CombinedRangeIndexEvaluator extends ARangeIndexEvaluator {

    public CombinedRangeIndexEvaluator(IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor, int paramsN) {
        super(rangeAdaptor, paramsN);
    }

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }
        final DecisionTableRuleNodeBuilder nextNodeBuilder = new DecisionTableRuleNodeBuilder();
        DecisionTableRuleNodeBuilder emptyRulesBuilder = new DecisionTableRuleNodeBuilder();
        List<IndexNode> minIndexNodes = collectMinIndexNodes(condition, it, nextNodeBuilder, emptyRulesBuilder);
        final RangeIndexNodeAdaptor indexNodeAdaptor = new RangeIndexNodeAdaptor(rangeAdaptor);
        RangeAscIndex minIndex = new RangeAscIndex(null,
            minIndexNodes,
            indexNodeAdaptor,
            emptyRulesBuilder.makeNode().getRules());

        it.reset();
        emptyRulesBuilder = new DecisionTableRuleNodeBuilder();
        List<IndexNode> maxIndexNodes = collectMaxIndexNodes(condition, it, emptyRulesBuilder);
        RangeDescIndex maxIndex = new RangeDescIndex(null,
            maxIndexNodes,
            indexNodeAdaptor,
            emptyRulesBuilder.makeNode().getRules());

        return new CombinedRangeIndex(minIndex, maxIndex, nextNodeBuilder.makeNode());
    }

    @SuppressWarnings("unchecked")
    private List<IndexNode> collectMinIndexNodes(ICondition condition,
            IIntIterator it,
            DecisionTableRuleNodeBuilder nextNodeBuilder,
            DecisionTableRuleNodeBuilder emptyRulesBuilder) {

        List<IndexNode> nodes = new ArrayList<>(it.size());
        while (it.hasNext()) {
            int ruleN = it.nextInt();
            nextNodeBuilder.addRule(ruleN);
            Object origVal = condition.getParamValue(0, ruleN);
            if (origVal == null) {
                emptyRulesBuilder.addRule(ruleN);
                continue;
            }
            Comparable<Object> vFrom = rangeAdaptor == null ? (Comparable<Object>) origVal
                                                            : rangeAdaptor.getMin(origVal);
            nodes.add(new IndexNode(vFrom, ruleN));
        }

        return mergeRulesByValue(nodes);
    }

    @SuppressWarnings("unchecked")
    private List<IndexNode> collectMaxIndexNodes(ICondition condition,
            IIntIterator it,
            DecisionTableRuleNodeBuilder emptyRulesBuilder) {

        List<IndexNode> nodes = new ArrayList<>(it.size());
        final int paramN = nparams == 2 ? 1 : 0;
        while (it.hasNext()) {
            int ruleN = it.nextInt();
            Object origVal = condition.getParamValue(paramN, ruleN);
            if (origVal == null) {
                emptyRulesBuilder.addRule(ruleN);
                continue;
            }
            Comparable<Object> vTo = rangeAdaptor == null ? (Comparable<Object>) origVal : rangeAdaptor.getMax(origVal);
            nodes.add(new IndexNode(vTo, ruleN));
        }

        return mergeRulesByValue(nodes);
    }
}
