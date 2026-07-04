package org.openl.studio.projects.service.trace;

import java.util.Arrays;
import java.util.Objects;

import org.openl.rules.dt.IBaseDecisionRow;

/**
 * Result of evaluating one decision-table condition against one or more rules.
 *
 * <p>Captured while the table runs so the table view can color matched conditions green and unmatched
 * ones red. The {@code condition} is held as an opaque reference (an {@code IBaseCondition}); the table
 * renderer resolves its value cells.
 *
 * @param condition  the decision-table condition
 * @param rules      rule indices the result applies to
 * @param successful whether the condition matched
 */
public record ConditionCheck(Object condition, int[] rules, boolean successful) {

    /** The condition's display name (for example {@code MC1}), or its string form if it is unnamed. */
    public String conditionName() {
        return condition instanceof IBaseDecisionRow row ? row.getName() : String.valueOf(condition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ConditionCheck other
                && successful == other.successful
                && Objects.equals(condition, other.condition)
                && Arrays.equals(rules, other.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition, Arrays.hashCode(rules), successful);
    }

    @Override
    public String toString() {
        return "ConditionCheck[condition=" + condition + ", rules=" + Arrays.toString(rules)
                + ", successful=" + successful + ']';
    }
}
