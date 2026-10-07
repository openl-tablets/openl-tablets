package org.openl.rules.ranges;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
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

    private static IOpenClass openClass;

    @BeforeAll
    static void compile() throws RulesInstantiationException, ProjectResolvingException {
        SimpleProjectEngineFactory<?> factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject("test/rules/decisionTableIndexes")
                .setExecutionMode(false)
                .build();

        openClass = factory.getCompiledOpenClass().getOpenClass();
    }

    @Test
    void testEqualsIndexEvaluator() {
        var dt = findDt("SimpleRules_NotDateRange_WhenNoRangesJustSimpleTextDates");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("SimpleRules_NotDateRange_WhenNoRangesJustSimpleDates");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("NotStringRange_WhenJustSimpleStringAndSkippedPatternAreDefined");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("StringRange_WhenAtLeastOneRangeDefined");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluator.class);

        dt = findDt("EqualsIndex_WithStatic");
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluatorV2.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());
    }

    @Test
    void testRangeIndexEvaluator() {
        var dt = findDt("SimpleRules_DateRange_WhenAtLeastOneRangeIsDefined");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], CombinedRangeIndexEvaluator.class);

        dt = findDt("RangeIndex_WithStatic");
        assertEquals(2, dt.getConditionRows().length);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], ARangeIndexEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[1], ARangeIndexEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[1]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[1]).isOptimizedExpression());
    }

    @Test
    void testContainsInArrayIndexEvaluator() {
        var dt = findDt("ContainsInArrayIndex_When_MethodExpr");
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);

        dt = findDt("ContainsInArrayIndex_WithStatic");
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        // the check holds only for an empty cell, so the lookup alone answers the condition
        dt = findDt("ContainsInArrayIndex_EmptyOrContains");
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);

        // the expression of the called table is read in the place of the call and indexed
        dt = findDt("ContainsInArrayIndex_ViaMethodTable");
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);

        dt = findDt("ContainsInArrayIndex_ViaSpreadsheet");
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        dt = findDt("ContainsInArrayIndex_OppositeOrder");
        assertConditionsNumber(dt);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], AContainsInArrayIndexedEvaluator.class);
    }

    @Test
    void testContainsInInputArrayIndexEvaluator() {
        var dt = findDt("ContainsInInputArrayIndex_When_MethodExpr");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithCast");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithPath");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_PrimitiveArray");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithDate");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_WithAlias");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
    }

    @Test
    void testContainsInInputArrayIndexEvaluatorWithStatic() {
        var dt = findDt("ContainsInInputArrayIndex_WithStatic");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        dt = findDt("ContainsInInputArrayIndex_WithStaticAnd");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        dt = findDt("ContainsInInputArrayIndex_WithTernary");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        // the lookup of the ternary may be written as its last part as well
        dt = findDt("ContainsInInputArrayIndex_WithTernaryElse");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertNotNull(((Condition) dt.getConditionRows()[0]).getStaticMethod());
        assertTrue(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());
    }

    @Test
    void testContainsInInputArrayIndexEvaluatorNextToOtherConditions() {
        var dt = findDt("ContainsInInputArrayIndex_ChainOverColumns");
        assertEquals(2, dt.getConditionRows().length);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_AfterEqualsIndex");
        assertEquals(2, dt.getConditionRows().length);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[0], EqualsIndexedEvaluatorV2.class);
        assertConditionEvaluatorClass(dt.getConditionRows()[1], ContainsInInputArrayIndexedEvaluator.class);

        dt = findDt("ContainsInInputArrayIndex_BeforeRangeIndex");
        assertEquals(2, dt.getConditionRows().length);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], ContainsInInputArrayIndexedEvaluator.class);
        assertInstanceConditionEvaluatorClass(dt.getConditionRows()[1], ARangeIndexEvaluator.class);
    }

    @Test
    void testDefaultConditionEvaluator() {
        // the test of the ternary reads the column, so its answer differs from rule to rule and cannot be static
        var dt = findDt("Ternary_TestOverColumn");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);
        assertFalse(((Condition) dt.getConditionRows()[0]).isOptimizedExpression());

        // a table of two lines is written of no single expression
        dt = findDt("Inline_TwoLineMethod");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);

        // the expression reads a name of its own module, which may mean something else in the decision table
        dt = findDt("Inline_ModuleName");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);

        // which version of the called table answers is decided at run time
        dt = findDt("Inline_VersionedTable");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);

        // the expression of the called table calls a table itself
        dt = findDt("Inline_CallOfAnotherTable");
        assertConditionsNumber(dt);
        assertConditionEvaluatorClass(dt.getConditionRows()[0], DefaultConditionEvaluator.class);
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

    private static DecisionTable findDt(String dtName) {
        for (IOpenMethod m : openClass.getMethods()) {
            if (dtName.equals(m.getName())) {
                return (DecisionTable) m.getInfo();
            }
        }
        fail("Cannot find DecisionTable: " + dtName);
        throw new IllegalStateException("Just a stub to make the compiler happy. Should never be reached.");
    }

}
