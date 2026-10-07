package org.openl.studio.projects.service.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;

class TestsRerunTest {

    private final ProjectModel model = mock(ProjectModel.class);
    private final TestUnitsResults results = mock(TestUnitsResults.class);
    private final ITestUnit unit = mock(ITestUnit.class);

    /**
     * A case is run again on its own - the table is run with the case and no other - against what the project was
     * compiled into when the run started.
     */
    @Test
    void runsACaseAgainOnItsOwnAgainstTheProjectAsItWasCompiled() {
        var compiled = mock(CompiledOpenClass.class);
        when(model.getCompiledOpenClass()).thenReturn(compiled);
        var second = mock(TestDescription.class);
        when(second.getIndex()).thenReturn(1);
        var method = methodOf(mock(TestDescription.class), second);
        when(results.getTestUnits()).thenReturn(List.of(unit));
        var ran = new ArrayList<TestSuite>();
        when(model.runTest(any(), eq(false), same(compiled))).thenAnswer(call -> {
            ran.add(call.getArgument(0));
            return results;
        });
        var rerun = TestsRerun.of(model, false);
        // The project is compiled again after the run started.
        var compiledAgain = mock(CompiledOpenClass.class);
        when(model.getCompiledOpenClass()).thenReturn(compiledAgain);

        assertSame(unit, rerun.runCase(method, second));
        assertSame(method, ran.getFirst().getTestSuiteMethod());
        assertArrayEquals(new TestDescription[]{second}, ran.getFirst().getTests());
    }

    @Test
    void runsACaseAgainInTheModuleThatWasOpen() {
        var compiled = mock(CompiledOpenClass.class);
        when(model.getOpenedModuleCompiledOpenClass()).thenReturn(compiled);
        var test = mock(TestDescription.class);
        when(results.getTestUnits()).thenReturn(List.of(unit));
        when(model.runTest(any(), eq(true), same(compiled))).thenReturn(results);

        assertSame(unit, TestsRerun.of(model, true).runCase(methodOf(test), test));
    }

    @Test
    void answersNothingForACaseThatDidNotRun() {
        var test = mock(TestDescription.class);
        when(results.getTestUnits()).thenReturn(List.of());
        when(model.runTest(any(), eq(false), any())).thenReturn(results);

        assertNull(TestsRerun.of(model, false).runCase(methodOf(test), test));
    }

    /** Once the project is compiled again, the model runs nothing, and the case is not had. */
    @Test
    void answersNothingOnceTheProjectWasCompiledAgain() {
        var test = mock(TestDescription.class);

        assertNull(TestsRerun.of(model, false).runCase(methodOf(test), test));
    }

    private static TestSuiteMethod methodOf(TestDescription... tests) {
        var method = mock(TestSuiteMethod.class);
        when(method.getTests()).thenReturn(tests);
        for (var i = 0; i < tests.length; i++) {
            when(method.getTest(i)).thenReturn(tests[i]);
        }
        return method;
    }
}
