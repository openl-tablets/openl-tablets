package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestUnitsResultsTest {

    private final ITestUnit passed = unit(TestStatus.TR_OK);
    private final ITestUnit failed1 = unit(TestStatus.TR_NEQ);
    private final ITestUnit failed2 = unit(TestStatus.TR_EXCEPTION);
    private final ITestUnit failed3 = unit(TestStatus.TR_NEQ);

    private TestUnitsResults results;

    @BeforeEach
    void setUp() {
        results = new TestUnitsResults(mock(TestSuite.class));
        results.addTestUnit(failed1);
        results.addTestUnit(passed);
        results.addTestUnit(failed2);
        results.addTestUnit(failed3);
    }

    @Test
    void keepsEveryCaseWhenNotOnlyFailures() {
        assertEquals(List.of(failed1, passed, failed2, failed3), results.getFilteredTestUnits(false, 1));
    }

    @Test
    void keepsTheFirstFailuresUpToTheSize() {
        assertEquals(List.of(failed1, failed2), results.getFilteredTestUnits(true, 2));
    }

    @Test
    void keepsEveryFailureForAllFailures() {
        assertEquals(List.of(failed1, failed2, failed3),
                results.getFilteredTestUnits(true, TestUnitsResults.ALL_FAILURES));
    }

    @Test
    void keepsNoFailureForANonPositiveSize() {
        assertEquals(List.of(), results.getFilteredTestUnits(true, 0));
        assertEquals(List.of(), results.getFilteredTestUnits(true, -2));
    }

    @Test
    void countsTheCasesThatRan() {
        assertEquals(4, results.getNumberOfTests());
        assertEquals(3, results.getNumberOfFailures());
    }

    @Test
    void countsEveryDeclaredCaseAsAFailedTestWhenTheTestedRulesDoNotCompile() {
        var suite = mock(TestSuite.class);
        when(suite.getTests()).thenReturn(new TestDescription[]{mock(TestDescription.class),
                mock(TestDescription.class)});
        var notRun = new TestUnitsResults(suite);
        notRun.setTestedRulesHaveErrors(true);

        assertEquals(0, notRun.getNumberOfTestUnits());
        assertEquals(2, notRun.getNumberOfTests());
        assertEquals(2, notRun.getNumberOfFailures());
    }

    private static ITestUnit unit(TestStatus status) {
        var unit = mock(ITestUnit.class);
        when(unit.getResultStatus()).thenReturn(status);
        return unit;
    }
}
