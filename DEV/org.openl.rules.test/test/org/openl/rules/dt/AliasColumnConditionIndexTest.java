package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenClass;

/**
 * Checks what the functional tests of {@code EPBDS-16540_Alias_Column_Conditions.xlsx} compare.
 *
 * <p>The tables of the workbook come in pairs. The second table of a pair holds a formula in one cell, or checks its
 * columns in its expression, and either keeps it out of the index. Its tests expect both tables of a pair to answer
 * the same. The answers compare an index with the rule by rule evaluation only while the engine gives the first
 * table an indexed evaluator and the second one the default evaluator, which checks the rules one by one.
 */
class AliasColumnConditionIndexTest {

    private static final String SRC = "test-resources/functionality/EPBDS-16540_Alias_Column_Conditions.xlsx";

    private static IOpenClass openClass;

    @BeforeAll
    static void compile() {
        var factory = new RulesEngineFactory<>(SRC);
        factory.setExecutionMode(false);
        openClass = factory.getCompiledOpenClass().getOpenClass();
    }

    @ParameterizedTest
    @ValueSource(strings = { "inputArray", "columnArray", "equals", "simple", "simpleArray", "otherwise",
            "rangeColumns", "rangeExpression", "rangeSingle" })
    void onlyTheFirstTableOfAPairIsIndexed(String pair) {
        assertFalse(evaluatorOf(pair + "ThroughIndex") instanceof DefaultConditionEvaluator,
                pair + "ThroughIndex must be indexed");
        assertTrue(evaluatorOf(pair + "RuleByRule") instanceof DefaultConditionEvaluator,
                pair + "RuleByRule must be evaluated rule by rule");
    }

    /** The evaluator the engine gave the condition of the table. */
    private static IBaseConditionEvaluator evaluatorOf(String table) {
        var decisionTable = (DecisionTable) openClass.getMethods()
                .stream()
                .filter(method -> method.getName().equals(table))
                .findFirst()
                .orElseThrow()
                .getInfo();
        return decisionTable.getConditionRows()[0].getConditionEvaluator();
    }
}
