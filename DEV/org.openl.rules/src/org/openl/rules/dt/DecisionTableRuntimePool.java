package org.openl.rules.dt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * What one call of a decision table has computed already.
 *
 * <p>A condition that uses no parameter of its own gives the same result for every rule, so it is computed once. A
 * formula in a cell of a condition is computed once too: a return value that reads that parameter again, as
 * {@code $C1.param} does, takes the value computed to match the rule.
 */
public class DecisionTableRuntimePool {

    private Map<String, Object> conditionExecutionPool;
    private Map<FormulaValue, @Nullable Object> formulaValues;

    public void pushConditionExecutionResultToPool(String conditionName, Object result) {
        if (conditionExecutionPool == null) {
            conditionExecutionPool = new HashMap<>();
        }
        conditionExecutionPool.put(conditionName, result);
    }

    public Object getConditionExecutionResult(String conditionName) {
        if (conditionExecutionPool == null) {
            return null;
        }
        return conditionExecutionPool.get(conditionName);
    }

    /**
     * The value of a formula of a parameter in a rule, computed at its first use in the call.
     *
     * @param row     the condition the parameter belongs to
     * @param param   the index of the parameter
     * @param rule    the index of the rule
     * @param formula computes the value
     * @return the value, {@code null} included, the formula gave at its first use
     */
    public @Nullable Object formulaValue(Object row, int param, int rule, Supplier<@Nullable Object> formula) {
        if (formulaValues == null) {
            formulaValues = new HashMap<>();
        }
        var key = new FormulaValue(row, param, rule);
        if (formulaValues.containsKey(key)) {
            return formulaValues.get(key);
        }
        var value = formula.get();
        formulaValues.put(key, value);
        return value;
    }

    private record FormulaValue(Object row, int param, int rule) {
    }

}
