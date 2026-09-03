package org.openl.rules.ranges;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.AContainsInArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ARangeIndexEvaluator;
import org.openl.rules.dt.algorithm.evaluator.CombinedRangeIndexEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ContainsInInputArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.EqualsIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.EqualsIndexedEvaluatorV2;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

class DecisionTableIndexCompilationTest {

    @Test
    void testDecisionTableCompilation_and_ConditionEvaluators() throws RulesInstantiationException,
            ProjectResolvingException {

        SimpleProjectEngineFactory<?> factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject("test/rules/decisionTableIndexes")
                .setExecutionMode(false)
                .build();

        var openClass = factory.getCompiledOpenClass().getOpenClass();

        var dt = findDt("SimpleRules_NotDateRange_WhenNoRangesJustSimpleTextDates", openClass);
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

        dt = findDt("ContainsInArrayIndex_WithStatic", openClass);
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        // the check holds only for an empty cell, so the lookup alone answers the condition
        dt = findDt("ContainsInArrayIndex_EmptyOrContains", openClass);
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);

        dt = findDt("EqualsIndex_WithStatic", openClass);
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluatorV2.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        dt = findDt("ContainsInInputArrayIndex_When_MethodExpr", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithStatic", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        dt = findDt("ContainsInInputArrayIndex_WithStaticAnd", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        dt = findDt("ContainsInInputArrayIndex_WithTernary", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        // the test of the ternary reads the column, so its answer differs from rule to rule and cannot be static
        dt = findDt("Ternary_TestOverColumn", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);
        assertFalse(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        // the expression of the called table is read in the place of the call and indexed
        dt = findDt("ContainsInArrayIndex_ViaMethodTable", openClass);
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);

        dt = findDt("ContainsInArrayIndex_ViaSpreadsheet", openClass);
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        // a table of two lines is written of no single expression
        dt = findDt("Inline_TwoLineMethod", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);

        // the expression reads a name of its own module, which may mean something else in the decision table
        dt = findDt("Inline_ModuleName", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);

        // which version of the called table answers is decided at run time
        dt = findDt("Inline_VersionedTable", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);

        // the expression of the called table calls a table itself
        dt = findDt("Inline_CallOfAnotherTable", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_ChainOverColumns", openClass);
        assertEquals(2, dt.getConditionRows().length);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_AfterEqualsIndex", openClass);
        assertEquals(2, dt.getConditionRows().length);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluatorV2.class);
        assertConditionEvaluatorClass(dt.getConditionRows()[1], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_BeforeRangeIndex", openClass);
        assertEquals(2, dt.getConditionRows().length);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[1], ARangeIndexEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithCast", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithPath", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_PrimitiveArray", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithDate", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithAlias", openClass);
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInArrayIndex_OppositeOrder", openClass);
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);

        dt = findDt("RangeIndex_WithStatic", openClass);
        assertEquals(2, dt.getConditionRows().length);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], ARangeIndexEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[1], ARangeIndexEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[1]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[1]).isOptimizedExpression());
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
