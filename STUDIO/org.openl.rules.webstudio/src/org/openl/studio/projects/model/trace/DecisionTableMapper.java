package org.openl.studio.projects.model.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.studio.projects.service.trace.ConditionCheck;
import org.openl.studio.projects.service.trace.DebugFrame;

/**
 * Maps a decision table of the debugger's stack to view models: its rules as steps and the explanation of the
 * rules that fired.
 */
final class DecisionTableMapper {

    private DecisionTableMapper() {
    }

    /**
     * Every rule of a decision table as a step. A decision-table frame on the live stack is always
     * mid-firing, so the rule whose action is running is the current one — and the called sub-table nests
     * under it. The rest are still pending and can be armed for a run-to.
     */
    static List<StepValueView> ruleOutline(IDecisionTable decisionTable, int[] firedRuleIndices) {
        var fired = Arrays.stream(firedRuleIndices)
                .mapToObj(decisionTable::getRuleName)
                .collect(Collectors.toSet());
        return ruleNames(decisionTable).stream()
                .map(name -> StepValueView.builder()
                        .ref(name)
                        .label(name)
                        .status(fired.contains(name) ? StepStatus.CURRENT : StepStatus.PENDING)
                        .build())
                .toList();
    }

    /** Every distinct rule name of a decision-table frame, so any rule can be armed; {@code null} otherwise. */
    static @Nullable List<String> ruleNamesFor(DebugFrame frame) {
        return frame.getSource() instanceof IDecisionTable decisionTable ? ruleNames(decisionTable) : null;
    }

    /** Every distinct rule name of a decision table, in rule order. */
    static List<String> ruleNames(IDecisionTable decisionTable) {
        return IntStream.range(0, decisionTable.getNumberOfRules())
                .mapToObj(decisionTable::getRuleName)
                .distinct()
                .toList();
    }

    /** Decision-table outcome explanation, or {@code null} for non-decision-table frames. */
    static @Nullable DecisionView decisionFor(DebugFrame frame) {
        if (!(frame.getSource() instanceof IDecisionTable decisionTable)) {
            return null;
        }
        return buildDecision(decisionTable, frame.getConditionChecks(), firedRuleIndices(frame));
    }

    static int[] firedRuleIndices(DebugFrame frame) {
        return frame.getCurrentStep() instanceof ActionInvoker invoker ? invoker.getRules() : new int[0];
    }

    /**
     * Build the plain-language decision outcome from the rules that fired and the conditions evaluated.
     * Mirrors the green/red table highlight: one entry per condition cell that was checked, so the
     * explanation never claims more than the engine actually evaluated.
     */
    static @Nullable DecisionView buildDecision(IDecisionTable decisionTable, List<ConditionCheck> checks,
                                                int[] firedRules) {
        if (checks.isEmpty() && firedRules.length == 0) {
            return null;
        }
        List<String> fired = Arrays.stream(firedRules).mapToObj(decisionTable::getRuleName).toList();
        var conditions = new ArrayList<DecisionConditionView>();
        for (ConditionCheck check : checks) {
            if (!(check.condition() instanceof IBaseCondition condition)) {
                continue;
            }
            var name = condition.getName();
            for (int rule : check.rules()) {
                conditions.add(new DecisionConditionView(name, decisionTable.getRuleName(rule), check.successful()));
            }
        }
        return new DecisionView(fired, conditions);
    }
}
