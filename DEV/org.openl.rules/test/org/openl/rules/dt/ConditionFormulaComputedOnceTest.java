package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;

/**
 * A formula in a condition of a rule is computed once in a call of the table, even when the return value of the rule
 * reads the parameter it gives (EPBDS-11248).
 *
 * @author Yury Molchan
 */
class ConditionFormulaComputedOnceTest {

    public interface Rules {
        Integer returnConditionParam(Integer a);

        Integer[] filterAll(Integer[] xs);
    }

    private static Rules rules;

    @BeforeAll
    static void compile() {
        rules = new RulesEngineFactory<>("test/rules/dt/EPBDS-11248_Condition_Formula_Once.xlsx", Rules.class)
                .newEngineInstance();
    }

    @BeforeEach
    void resetCalls() {
        FormulaCalls.CALLS.set(0);
    }

    @Test
    void theReturnValueTakesTheParameterTheConditionComputed() {
        assertEquals(22, rules.returnConditionParam(2));
        assertEquals(1, FormulaCalls.CALLS.get());
    }

    @Test
    void eachCallComputesTheFormulaAgain() {
        rules.returnConditionParam(2);
        rules.returnConditionParam(2);
        assertEquals(2, FormulaCalls.CALLS.get());
    }

    @Test
    void aRuleThatDoesNotReturnComputesItsFormulaOnce() {
        assertEquals(33, rules.returnConditionParam(3));
        assertEquals(1, FormulaCalls.CALLS.get());
    }

    @Test
    void aTableCalledForEachElementFromARuleComputesTheFormulaForEachElement() {
        assertArrayEquals(new Integer[]{5, null}, rules.filterAll(new Integer[]{5, -3}));
        assertArrayEquals(new Integer[]{null, 5}, rules.filterAll(new Integer[]{-3, 5}));
    }
}
