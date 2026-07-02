package org.openl.rules.webstudio.web.trace.debug;

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
}
