package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.testmethod.TestSuiteMethod;

/**
 * The trace runs a single test case: several cases just stack in the tree and rarely help, so a
 * requested range or the whole suite collapses to one case.
 */
class TraceDebugServiceImplTest {

    @Test
    void collapsesARangeToItsFirstSelectedCase() {
        TestSuiteMethod method = mock(TestSuiteMethod.class);
        when(method.getIndices("2-4,7")).thenReturn(new int[]{1, 2, 3, 6});

        assertEquals(1, TraceDebugServiceImpl.firstTestIndex(method, "2-4,7"),
                "a range traces only its first selected case");
    }

    @Test
    void tracesTheFirstCaseForTheWholeSuite() {
        TestSuiteMethod method = mock(TestSuiteMethod.class);

        assertEquals(0, TraceDebugServiceImpl.firstTestIndex(method, null),
                "no range traces the suite's first case");
    }

    @Test
    void fallsBackToTheFirstCaseWhenTheRangeMatchesNothing() {
        TestSuiteMethod method = mock(TestSuiteMethod.class);
        when(method.getIndices("999")).thenReturn(new int[0]);

        assertEquals(0, TraceDebugServiceImpl.firstTestIndex(method, "999"),
                "an empty range falls back to the first case");
    }
}
