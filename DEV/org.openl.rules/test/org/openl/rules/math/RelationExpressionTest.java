package org.openl.rules.math;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.TestUtils;

public class RelationExpressionTest {

    private static String SRC = "test/rules/math/RelationExpressionTest.xls";

    @Test
    public void testEqualComparison() {
        ITest instance = TestUtils.create(SRC, ITest.class);
        assertTrue(instance.areEqual1());
        assertTrue(instance.areEqual2());
        assertTrue(instance.areEqual3());
        assertTrue(instance.areEqual4());
        assertTrue(instance.areEqual5());
        assertTrue(instance.areEqual6());
    }

    @Test
    public void testNotEqualComparison() {
        ITest instance = TestUtils.create(SRC, ITest.class);
        assertTrue(instance.areNotEqual1());
        assertTrue(instance.areNotEqual2());
        assertTrue(instance.areNotEqual3());
        assertTrue(instance.areNotEqual4());
        assertTrue(instance.areNotEqual5());
        assertTrue(instance.areNotEqual6());
    }

    @Test
    public void testLessThanComparison() {
        ITest instance = TestUtils.create(SRC, ITest.class);
        assertTrue(instance.areLessThan1());
        assertTrue(instance.areLessThan2());
        assertTrue(instance.areLessThan3());
        assertTrue(instance.areLessThan4());
        assertTrue(instance.areLessThan5());
        assertTrue(instance.areLessThan6());
    }

    @Test
    public void testGreaterThanComparison() {
        ITest instance = TestUtils.create(SRC, ITest.class);
        assertTrue(instance.areGreaterThan1());
        assertTrue(instance.areGreaterThan2());
        assertTrue(instance.areGreaterThan3());
        assertTrue(instance.areGreaterThan4());
        assertTrue(instance.areGreaterThan5());
        assertTrue(instance.areGreaterThan6());
    }

    @Test
    public void testGreaterOrEqualComparison() {
        ITest instance = TestUtils.create(SRC, ITest.class);
        assertTrue(instance.areGreaterOrEqual1());
        assertTrue(instance.areGreaterOrEqual2());
        assertTrue(instance.areGreaterOrEqual3());
        assertTrue(instance.areGreaterOrEqual4());
        assertTrue(instance.areGreaterOrEqual5());
        assertTrue(instance.areGreaterOrEqual6());
    }

    @Test
    public void testLessOrEqualComparison() {
        ITest instance = TestUtils.create(SRC, ITest.class);
        assertTrue(instance.areLessOrEqual1());
        assertTrue(instance.areLessOrEqual2());
        assertTrue(instance.areLessOrEqual3());
        assertTrue(instance.areLessOrEqual4());
        assertTrue(instance.areLessOrEqual5());
        assertTrue(instance.areLessOrEqual6());
    }

    @Test
    public void testIndexedEvaluator() {
        ITest instance = TestUtils.create(SRC, ITest.class);
        assertEquals(1, instance.testEqualIndexedEvaluator(1.3));
        assertEquals(2, instance.testEqualIndexedEvaluator(1.12345678901234));
        assertEquals(3, instance.testEqualIndexedEvaluator(1.1234567890123456789));
        assertEquals(3, instance.testEqualIndexedEvaluator(1.1234567890123456789 + Math.ulp(1.1234567890123456789)));
        assertEquals(0, instance.testEqualIndexedEvaluator(1.1234567890123456789 + 0.000000000000001));

        assertFalse(instance.testRangeIndexedEvaluator(1.0));
        assertTrue(instance.testRangeIndexedEvaluator(1.2));
        assertFalse(instance.testRangeIndexedEvaluator(1.3));
        assertTrue(instance.testRangeIndexedEvaluator(1.3 - Math.ulp(1.3)));
    }

    public interface ITest {
        boolean areEqual1();

        boolean areEqual2();

        boolean areEqual3();

        boolean areEqual4();

        boolean areEqual5();

        boolean areEqual6();

        boolean areNotEqual1();

        boolean areNotEqual2();

        boolean areNotEqual3();

        boolean areNotEqual4();

        boolean areNotEqual5();

        boolean areNotEqual6();

        boolean areLessThan1();

        boolean areLessThan2();

        boolean areLessThan3();

        boolean areLessThan4();

        boolean areLessThan5();

        boolean areLessThan6();

        boolean areGreaterThan1();

        boolean areGreaterThan2();

        boolean areGreaterThan3();

        boolean areGreaterThan4();

        boolean areGreaterThan5();

        boolean areGreaterThan6();

        boolean areLessOrEqual1();

        boolean areLessOrEqual2();

        boolean areLessOrEqual3();

        boolean areLessOrEqual4();

        boolean areLessOrEqual5();

        boolean areLessOrEqual6();

        boolean areGreaterOrEqual1();

        boolean areGreaterOrEqual2();

        boolean areGreaterOrEqual3();

        boolean areGreaterOrEqual4();

        boolean areGreaterOrEqual5();

        boolean areGreaterOrEqual6();

        int testEqualIndexedEvaluator(double value);

        boolean testRangeIndexedEvaluator(double value);
    }

}
