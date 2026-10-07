package org.openl.studio.projects.service.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.studio.common.exception.NotFoundException;

class RetainedTestUnitTest {

    /** Makes the reference a value with inner structure is held by, and remembers it. */
    private final List<Reference<Object>> held = new ArrayList<>();
    private final Function<Object, Reference<Object>> holder = value -> {
        var reference = new SoftReference<>(value);
        held.add(reference);
        return reference;
    };
    private final TestSuiteMethod method = mock(TestSuiteMethod.class);
    /** The cases run again, and what running a case again returns. */
    private final List<TestDescription> ranAgain = new ArrayList<>();
    private ITestUnit again;
    private final TestsRerun rerun = (table, test) -> {
        assertSame(method, table, "a case runs again in the table it belongs to");
        ranAgain.add(test);
        return again;
    };

    @Test
    void reportsWhatTheCaseReported() {
        var test = mock(TestDescription.class);
        var compared = List.of(new ComparedResult("premium", 100, 150, TestStatus.TR_NEQ));
        var errors = List.of(new OpenLMessage("Division by zero", Severity.ERROR));
        var unit = mock(ITestUnit.class);
        when(unit.getTest()).thenReturn(test);
        when(unit.getExpectedResult()).thenReturn(100);
        when(unit.getExecutionTime()).thenReturn(42L);
        when(unit.getResultStatus()).thenReturn(TestStatus.TR_NEQ);
        when(unit.getComparisonResults()).thenReturn(compared);
        when(unit.getNumberOfFailedTests()).thenReturn(1);
        when(unit.getErrors()).thenReturn(errors);
        when(unit.getDescription()).thenReturn("A driver of 17");

        var retained = retained(unit);

        assertSame(test, retained.getTest());
        assertEquals(100, retained.getExpectedResult());
        assertEquals(42L, retained.getExecutionTime());
        assertEquals(TestStatus.TR_NEQ, retained.getResultStatus());
        assertEquals(compared, retained.getComparisonResults());
        assertEquals(1, retained.getNumberOfFailedTests());
        assertSame(errors, retained.getErrors());
        assertEquals("A driver of 17", retained.getDescription());
    }

    /**
     * A workbook reads the comparisons with every value named after the field it was compared in, the way the
     * engine's case writes them; a comparison of the whole result is named after what it compares.
     */
    @Test
    void writesTheComparisonsForAWorkbookAsTheEngineDoes() {
        var unit = returning(150);
        when(unit.getComparisonResults()).thenReturn(List.of(
                new ComparedResult("premium", 100, 150, TestStatus.TR_NEQ),
                new ComparedResult(null, 100, 150, TestStatus.TR_NEQ)));

        var written = retained(unit).getResultParams();

        assertEquals("premium", written.get(0).getFieldName());
        assertEquals(TestStatus.TR_NEQ, written.get(0).getStatus());
        var expected = assertInstanceOf(ParameterWithValueDeclaration.class, written.get(0).getExpectedValue());
        var actual = assertInstanceOf(ParameterWithValueDeclaration.class, written.get(0).getActualValue());
        assertEquals("premium", expected.getName());
        assertEquals(100, expected.getValue());
        assertEquals("premium", actual.getName());
        assertEquals(150, actual.getValue());
        assertEquals("expectedResult", ((ParameterWithValueDeclaration) written.get(1).getExpectedValue()).getName());
        assertEquals("actualResult", ((ParameterWithValueDeclaration) written.get(1).getActualValue()).getName());
    }

    /**
     * A {@code _res_} column compares the whole result, so the value compared is the one the rule returned: it is
     * held softly as well, or it would keep the whole result alive. A plain compared value is kept.
     */
    @Test
    void holdsAComparedValueWithInnerStructureSoftly() {
        var premium = Map.of("premium", 150);
        var unit = returning(premium);
        when(unit.getComparisonResults()).thenReturn(List.of(
                new ComparedResult("_res_", Map.of("premium", 150), premium, TestStatus.TR_OK),
                new ComparedResult("total", 150, 150, TestStatus.TR_OK)));

        var retained = retained(unit);
        var whole = retained.getComparisonResults().getFirst();
        assertSame(premium, whole.getActualValue());

        held.forEach(Reference::clear);

        assertTrue(RetainedTestUnit.isReleased(whole.getActualValue()));
        assertTrue(RetainedTestUnit.isReleased(retained.getActualResult()));
        assertEquals("_res_", whole.getFieldName());
        assertEquals(TestStatus.TR_OK, whole.getStatus());
        assertEquals(150, retained.getComparisonResults().get(1).getActualValue());
    }

    @Test
    void keepsAPlainValueAndAnError() {
        var error = new IllegalStateException("Division by zero");

        assertEquals(150.0, retained(returning(150.0)).getActualResult());
        assertSame(error, retained(returning(error)).getActualResult());
        assertNull(retained(returning(null)).getActualResult());
        assertTrue(held.isEmpty(), "what costs little is kept, not held softly");
    }

    @Test
    void holdsAValueWithInnerStructureSoftly() {
        var premium = Map.of("premium", 150);

        var retained = retained(returning(premium));

        assertSame(premium, retained.getActualResult());
        assertSame(premium, retained.getActualParam().getValue());
        assertInstanceOf(SoftReference.class, held.getFirst());
        assertTrue(ranAgain.isEmpty(), "a case that holds its value runs nothing");
    }

    @Test
    void saysAValueGivenBackToFreeMemoryIsReleased() {
        var retained = retained(returning(Map.of("premium", 150)));
        assertFalse(RetainedTestUnit.isReleased(retained.getActualResult()));

        held.getFirst().clear();

        assertTrue(RetainedTestUnit.isReleased(retained.getActualResult()));
        // The case goes on reporting the rest.
        assertEquals(TestStatus.TR_OK, retained.getResultStatus());
    }

    /** The values a case holds are what it returned and what it compared, in that order. */
    @Test
    void listsTheValuesTheCaseHolds() {
        var premium = Map.of("premium", 150);
        var unit = returning(premium);
        when(unit.getComparisonResults()).thenReturn(List.of(new ComparedResult("total", 150, 140, TestStatus.TR_NEQ)));

        assertEquals(List.of(premium, 140), RetainedTestUnit.heldValues(retained(unit)));
    }

    @Test
    void keepsTheCasesOfATestTableInPlaceOfTheEngines() {
        var suite = mock(TestSuite.class);
        when(suite.getTestSuiteMethod()).thenReturn(method);
        var results = new TestUnitsResults(suite);
        var failed = returning(Map.of("premium", 150));
        when(failed.getResultStatus()).thenReturn(TestStatus.TR_NEQ);
        results.getTestUnits().addAll(List.of(returning(100.0), failed));

        assertSame(results, RetainedTestUnit.retain(results, rerun));

        assertTrue(results.getTestUnits().stream().allMatch(RetainedTestUnit.class::isInstance));
        assertEquals(2, results.getNumberOfTestUnits());
        assertEquals(1, results.getNumberOfFailures());
    }

    /** A case runs again on its own, in the table it belongs to, and keeps what it kept. */
    @Test
    void runsTheCaseAgain() {
        var test = mock(TestDescription.class);
        var unit = returning(Map.of("premium", 150));
        when(unit.getTest()).thenReturn(test);
        var retained = retained(unit);
        held.getFirst().clear();
        again = returning(Map.of("premium", 150));

        assertSame(again, retained.again());
        assertEquals(List.of(test), ranAgain);
        assertTrue(RetainedTestUnit.isReleased(retained.getActualResult()), "the case keeps what it kept");
    }

    /** Once the project is compiled again, a case that gave its value back cannot have it, and says so. */
    @Test
    void cannotRunAgainOnceTheProjectWasCompiledAgain() {
        var retained = retained(returning(Map.of("premium", 150)));
        held.getFirst().clear();

        var refusal = assertThrows(NotFoundException.class, retained::again);
        assertEquals("openl.error.404.tests.execution.values.released.message", refusal.getErrorCode());
        // A workbook written from the case is not written without the value.
        assertThrows(NotFoundException.class, retained::getActualParam);
    }

    /**
     * A workbook is written from a case that gave its values back by running the case again, while the values are
     * written. What running again returns goes to the workbook, and the case keeps what it kept.
     */
    @Test
    void writesAReleasedValueFromTheCaseRunAgain() {
        var test = mock(TestDescription.class);
        var unit = returning(Map.of("premium", 150));
        when(unit.getTest()).thenReturn(test);
        when(unit.getComparisonResults()).thenReturn(
                List.of(new ComparedResult("_res_", Map.of("premium", 150), Map.of("premium", 150), TestStatus.TR_OK)));
        var retained = retained(unit);
        // The case gives back what it returned and what it compared.
        held.forEach(Reference::clear);
        again = returning(Map.of("premium", 150));
        var againParams = List.of(new ComparedResult("_res_", null, null, TestStatus.TR_OK));
        when(again.getResultParams()).thenReturn(againParams);
        when(again.getActualParam()).thenReturn(new ParameterWithValueDeclaration("actual", Map.of("premium", 150)));

        assertSame(againParams, retained.getResultParams());
        assertEquals(Map.of("premium", 150), retained.getActualParam().getValue());
        assertEquals(List.of(test, test), ranAgain);
        assertTrue(RetainedTestUnit.isReleased(retained.getActualResult()), "the case keeps what it kept");
    }

    /** A case run again holds again what it gave back, and keeps what it still holds. */
    @Test
    void holdsAgainTheValuesOfTheCaseRunAgain() {
        var kept = Map.of("total", 150);
        var unit = returning(Map.of("premium", 150));
        when(unit.getComparisonResults()).thenReturn(List.of(
                new ComparedResult("_res_", null, Map.of("premium", 150), TestStatus.TR_OK),
                new ComparedResult("summary", null, kept, TestStatus.TR_OK)));
        var retained = retained(unit);
        // What it returned and the first compared value are given back; the second is still held.
        held.get(0).clear();
        held.get(2).clear();
        var premium = Map.of("premium", 150);
        var secondRun = returning(premium);
        when(secondRun.getComparisonResults()).thenReturn(List.of(
                new ComparedResult("_res_", null, premium, TestStatus.TR_OK),
                new ComparedResult("summary", null, Map.of("total", 150), TestStatus.TR_OK)));

        retained.holdAgain(secondRun);

        assertEquals(5, held.size(), "what was given back is held again, the way it was held first");
        assertSame(premium, retained.getActualResult());
        assertSame(premium, retained.getComparisonResults().getFirst().getActualValue());
        assertSame(kept, retained.getComparisonResults().getLast().getActualValue());
    }

    /** A plain value is kept as it is: a case run again leaves it. */
    @Test
    void keepsAPlainValueWhenTheCaseRunsAgain() {
        var retained = retained(returning(150));

        retained.holdAgain(returning(170));

        assertEquals(150, retained.getActualResult());
        assertTrue(held.isEmpty(), "a plain value is held by no reference");
    }

    /** The case as a run keeps it: in the table of {@link #method}, running again through {@link #rerun}. */
    private RetainedTestUnit retained(ITestUnit unit) {
        return new RetainedTestUnit(unit, method, rerun, holder);
    }

    /** A case that passed, having returned the given value. */
    private static ITestUnit returning(Object value) {
        var unit = mock(ITestUnit.class);
        when(unit.getActualResult()).thenReturn(value);
        when(unit.getResultStatus()).thenReturn(TestStatus.TR_OK);
        return unit;
    }
}
