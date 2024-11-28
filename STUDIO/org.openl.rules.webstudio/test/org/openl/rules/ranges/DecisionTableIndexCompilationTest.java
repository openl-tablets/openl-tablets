package org.openl.rules.ranges;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.AContainsInArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.CombinedRangeIndexEvaluator;
import org.openl.rules.dt.algorithm.evaluator.EqualsIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class DecisionTableIndexCompilationTest {

    @Test
    public void testDecisionTableCompilation_and_ConditionEvaluators() throws RulesInstantiationException,
            ProjectResolvingException {

        SimpleProjectEngineFactory<?> factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject("test/rules/decisionTableIndexes")
                .setExecutionMode(false)
                .build();

        IOpenClass openClass = factory.getCompiledOpenClass().getOpenClass();

        DecisionTable dt = findDt("SimpleRules_NotDateRange_WhenNoRangesJustSimpleTextDates", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("SimpleRules_NotDateRange_WhenNoRangesJustSimpleDates", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("SimpleRules_DateRange_WhenAtLeastOneRangeIsDefined", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], CombinedRangeIndexEvaluator.class);

        dt = findDt("NotStringRange_WhenJustSimpleStringAndSkippedPatternAreDefined", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("StringRange_WhenAtLeastOneRangeDefined", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("ContainsInArrayIndex_When_MethodExpr", openClass);
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);
    }

    private <T extends IConditionEvaluator> void assertConditionEvaluatorClass(IBaseCondition condition,
                                                                               Class<T> tClass) {
        assertSame(tClass, condition.getConditionEvaluator().getClass());
    }

    private <T extends IConditionEvaluator> void assertInstanceConditionEvaluatorClass(IBaseCondition condition,
                                                                               Class<T> tClass) {
        assertTrue(tClass.isAssignableFrom(condition.getConditionEvaluator().getClass()),
                condition.getConditionEvaluator().getClass() + " must be instance of " + tClass);
    }

    private void assertConditionsNumber(DecisionTable dt) {
        assertEquals(1, dt.getConditionRows().length);
    }

    private DecisionTable findDt(String dtName, IOpenClass openClass) {
        for (IOpenMethod m : openClass.getMethods()) {
            if (dtName.equals(m.getName())) {
                return (DecisionTable) m.getInfo();
            }
        }
        fail("Cannot find DecisionTable: " + dtName);
        throw new IllegalStateException("Just a stub to make the compiler happy. Should never be reached.");
    }

}
