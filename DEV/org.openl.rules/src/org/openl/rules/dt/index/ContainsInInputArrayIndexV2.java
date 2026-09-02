package org.openl.rules.dt.index;

import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.Map;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.IDecisionTableRuleNodeV2;
import org.openl.rules.dt.RangeIndexDecisionTableRuleNode;
import org.openl.rules.dt.element.ConditionCasts;

/**
 * An index for a condition that looks for the condition column value inside an array passed to the decision table,
 * for example {@code contains(codes, code)} where {@code codes} is a table input and {@code code} is a column value.
 *
 * <p>The index maps every column value to the rules that declare it. A lookup takes the whole input array, searches
 * each of its elements and returns the union of the matching rules. Rules with an empty condition cell always match.
 *
 * <p>A null or an empty input array matches nothing but the rules with an empty condition cell. Repeated elements
 * select a rule once.
 *
 * @author Vladyslav Pikus
 */
public class ContainsInInputArrayIndexV2 extends EqualsIndexV2 {

    public ContainsInInputArrayIndexV2(DecisionTableRuleNode nextNode,
                                       Map<Object, int[]> index,
                                       int[] emptyRules,
                                       ConditionCasts conditionCasts) {
        super(nextNode, index, emptyRules, conditionCasts);
    }

    @Override
    protected DecisionTableRuleNode findNode(Object value, DecisionTableRuleNode prevResult) {
        var rules = new BitSet();
        if (value != null) {
            var length = Array.getLength(value);
            for (var i = 0; i < length; i++) {
                for (int ruleN : findIndex(Array.get(value, i))) {
                    rules.set(ruleN);
                }
            }
        }
        for (int ruleN : emptyRules) {
            rules.set(ruleN);
        }
        if (prevResult instanceof IDecisionTableRuleNodeV2 prevResultV2) {
            rules.and(prevResultV2.getRuleSet());
        }
        return new RangeIndexDecisionTableRuleNode(rules, nextNode.getNextIndex());
    }

    public static class Builder extends EqualsIndexV2.Builder {

        @Override
        protected EqualsIndexV2 newIndex(DecisionTableRuleNode nextNode,
                                         Map<Object, int[]> index,
                                         int[] emptyRules,
                                         ConditionCasts conditionCasts) {
            return new ContainsInInputArrayIndexV2(nextNode, index, emptyRules, conditionCasts);
        }
    }
}
