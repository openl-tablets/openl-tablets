package org.openl.rules.testmethod;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.TestUtils;

import static org.junit.Assert.assertEquals;

public class FailedNegativePrecisionTest {

    private static final String FILE_NAME = "test/rules/testmethod/NegativePrecitionInTests_mustBeFailed.xlsx";

    private INegativePrecision instance;

    @Before
    public void setUp() {
        instance = TestUtils.create(FILE_NAME, INegativePrecision.class);
    }

    @Test
    public void testFloatNumbersNegativePrecision1_mustBeFailed() {
        TestUnitsResults result = instance.testFloatNumbersNegativePrecision1_mustBeFailed();
        assertEquals(1, result.getNumberOfFailures());
        assertAllFailed(result.getTestUnits().get(0));
        ITestUnit testUnit = result.getTestUnits().get(0);
        assertEquals(2, testUnit.getComparisonResults().size());
        assertAllFailed(testUnit);
    }

    @Test
    public void testPositivePrecision1_mustBeFailed() {
        TestUnitsResults result = instance.testPositivePrecision1_mustBeFailed();
        assertEquals(1, result.getNumberOfFailures());
        assertAllFailed(result.getTestUnits().get(0));
        ITestUnit testUnit = result.getTestUnits().get(0);
        assertEquals(4, testUnit.getComparisonResults().size());
        assertAllFailed(testUnit);
    }

    @Test
    public void testNegativePrecision1_mustBeFailed() {
        TestUnitsResults result = instance.testNegativePrecision1_mustBeFailed();
        assertEquals(1, result.getNumberOfFailures());
        ITestUnit testUnit = result.getTestUnits().get(0);
        assertEquals(4, testUnit.getComparisonResults().size());
        assertAllFailed(testUnit);
    }

    private static void assertAllFailed(ITestUnit testUnit) {
        assertEquals(testUnit.getNumberOfFailedTests(), testUnit.getComparisonResults().size());
    }

    public interface INegativePrecision {

        TestUnitsResults testFloatNumbersNegativePrecision1_mustBeFailed();

        TestUnitsResults testNegativePrecision1_mustBeFailed();

        TestUnitsResults testPositivePrecision1_mustBeFailed();
    }
}
