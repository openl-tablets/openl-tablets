package org.openl.rules.ranges;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.CombinedRangeIndexEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ContainsInArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.EqualsIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class RangeCompilationTest {

    @Test
    public void testDecisionTableCompilation_and_ConditionEvaluators() throws RulesInstantiationException,
                                                                       ProjectResolvingException,
                                                                       ClassNotFoundException {

        SimpleProjectEngineFactory<?> factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
            .setProject("test/rules/ranges")
            .setExecutionMode(false)
            .build();

        IOpenClass openClass = factory.getCompiledOpenClass().getOpenClass();

        DecisionTable dt = findDt("SimpleRules_NotDateRange_WhenNoRangesJustSimpleTextDates", openClass);
        assertConditionsNumber(dt, 1);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("SimpleRules_NotDateRange_WhenNoRangesJustSimpleDates", openClass);
        assertConditionsNumber(dt, 1);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInArrayIndexedEvaluator.class);

        dt = findDt("SimpleRules_DateRange_WhenAtLeastOneRangeIsDefined", openClass);
        assertConditionsNumber(dt, 1);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], CombinedRangeIndexEvaluator.class);

        dt = findDt("NotStringRange_WhenJustSimpleStringAndSkippedPatternAreDefined", openClass);
        assertConditionsNumber(dt, 1);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("StringRange_WhenAtLeastOneRangeDefined", openClass);
        assertConditionsNumber(dt, 1);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], CombinedRangeIndexEvaluator.class);
    }

    private <T extends IConditionEvaluator> void assertConditionEvaluatorClass(IBaseCondition condition,
            Class<T> tClass) {
        assertSame(tClass, condition.getConditionEvaluator().getClass());
    }

    private void assertConditionsNumber(DecisionTable dt, int expectedConditionCount) {
        assertEquals(expectedConditionCount, dt.getConditionRows().length);
    }

    private DecisionTable findDt(String dtName, IOpenClass openClass) {
        for (IOpenMethod m : openClass.getMethods()) {
            if (dtName.equals(m.getName())) {
                return (DecisionTable) m.getInfo();
            }
        }
        fail("Cannot find DecisionTable: " + dtName);
        return null;
    }

}
