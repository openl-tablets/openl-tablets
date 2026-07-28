package org.openl.rules.dt.algorithm.evaluator;

import java.util.ArrayList;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.IRuleIndex;
import org.openl.rules.dt.index.RangeAscIndex;
import org.openl.rules.dt.index.RangeDescIndex;
import org.openl.rules.dt.type.IRangeAdaptor;

public class SingleRangeIndexEvaluator extends ARangeIndexEvaluator {

    public SingleRangeIndexEvaluator(IRangeAdaptor<Object, ? extends Comparable<Object>> rangeAdaptor,
                                     ConditionCasts conditionCasts) {
        super(rangeAdaptor, 1, conditionCasts);
    }

    @Override
    public IRuleIndex makeIndex(ICondition condition, IIntIterator it) {
        if (it.size() < 1) {
            return null;
        }
        var nodes = new ArrayList<IndexNode>(it.size());
        var emptyRulesBuilder = new DecisionTableRuleNodeBuilder();
        var nextNodeBuilder = new DecisionTableRuleNodeBuilder();
        var isNaturalOrder = true;
        while (it.hasNext()) {
            var ruleN = it.nextInt();
            nextNodeBuilder.addRule(ruleN);
            if (condition.isEmpty(ruleN)) {
                emptyRulesBuilder.addRule(ruleN);
                continue;
            }

            var origVal = condition.getParamValue(0, ruleN);
            var vFrom = rangeAdaptor.<Object>getMin(origVal);
            var vTo = rangeAdaptor.<Object>getMax(origVal);
            if (vFrom != null) {
                nodes.add(new IndexNode(vFrom, ruleN));
                isNaturalOrder = true;
            } else if (vTo != null) {
                nodes.add(new IndexNode(vTo, ruleN));
                isNaturalOrder = false;
            }
        }

        var result = mergeRulesByValue(nodes);
        var indexNodeAdaptor = new RangeIndexNodeAdaptor(rangeAdaptor);
        var emptyRules = emptyRulesBuilder.makeNode().getRules();
        if (isNaturalOrder) {
            return new RangeAscIndex(nextNodeBuilder.makeNode(), result, indexNodeAdaptor, emptyRules);
        } else {
            return new RangeDescIndex(nextNodeBuilder.makeNode(), result, indexNodeAdaptor, emptyRules);
        }
    }
}
