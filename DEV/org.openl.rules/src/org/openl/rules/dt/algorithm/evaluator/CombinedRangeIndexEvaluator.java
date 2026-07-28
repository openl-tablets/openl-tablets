package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;
import java.util.List;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.CombinedRangeIndex;
import org.openl.rules.dt.index.IRuleIndex;
import org.openl.rules.dt.index.RangeAscIndex;
import org.openl.rules.dt.index.RangeDescIndex;
import org.openl.rules.dt.type.IRangeAdaptor;

public class CombinedRangeIndexEvaluator extends ARangeIndexEvaluator {

    public CombinedRangeIndexEvaluator(IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor,
                                       int nparams,
                                       ConditionCasts conditionCasts) {
        super(rangeAdaptor, nparams, conditionCasts);
    }

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }
        final var nextNodeBuilder = new DecisionTableRuleNodeBuilder();
        var emptyRulesBuilder = new DecisionTableRuleNodeBuilder();
        var minIndexNodes = collectMinIndexNodes(condition, it, nextNodeBuilder, emptyRulesBuilder);
        final var nextNode = nextNodeBuilder.makeNode();
        final var indexNodeAdaptor = new RangeIndexNodeAdaptor(rangeAdaptor);
        var minIndex = new RangeAscIndex(nextNode,
                minIndexNodes,
                indexNodeAdaptor,
                emptyRulesBuilder.makeNode().getRules());

        it.reset();
        emptyRulesBuilder = new DecisionTableRuleNodeBuilder();
        var maxIndexNodes = collectMaxIndexNodes(condition, it, emptyRulesBuilder);
        var maxIndex = new RangeDescIndex(nextNode,
                maxIndexNodes,
                indexNodeAdaptor,
                emptyRulesBuilder.makeNode().getRules());

        return new CombinedRangeIndex(minIndex,
                maxIndex,
                nextNode,
                nparams == 2 ? conditionCasts.getCastToConditionType() : null);
    }

    @SuppressWarnings("unchecked")
    private List<IndexNode> collectMinIndexNodes(ICondition condition,
                                                 IIntIterator it,
                                                 DecisionTableRuleNodeBuilder nextNodeBuilder,
                                                 DecisionTableRuleNodeBuilder emptyRulesBuilder) {

        var nodes = new ArrayList<IndexNode>(it.size());
        while (it.hasNext()) {
            var ruleN = it.nextInt();
            nextNodeBuilder.addRule(ruleN);
            var origVal = condition.getParamValue(0, ruleN);
            origVal = conditionCasts.castToInputType(origVal);
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

        var nodes = new ArrayList<IndexNode>(it.size());
        final int paramN = nparams == 2 ? 1 : 0;
        while (it.hasNext()) {
            var ruleN = it.nextInt();
            var origVal = condition.getParamValue(paramN, ruleN);
            origVal = conditionCasts.castToInputType(origVal);
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
