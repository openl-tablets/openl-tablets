package org.openl.rules.dt.algorithm.evaluator;

import org.openl.rules.dt.element.ICondition;

/**
 * A parameter of a condition column, used as a source of the values an index is built from.
 *
 * <p>A condition may look up the values of another column, so the parameter is addressed by the column it belongs
 * to and by its position in that column.
 *
 * @param condition the column that declares the parameter
 * @param index     the position of the parameter in the column
 */
public record ConditionParameter(ICondition condition, int index) {

    /**
     * Returns the value of the parameter in the rule, or {@code null} when the cell of the rule is empty.
     */
    public Object getValue(int ruleN) {
        return condition.getParamValue(index, ruleN);
    }
}
