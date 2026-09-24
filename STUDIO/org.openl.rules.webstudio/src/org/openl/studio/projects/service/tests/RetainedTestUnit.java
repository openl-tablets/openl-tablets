package org.openl.studio.projects.service.tests;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLMessage;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.TestUtils;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.studio.common.exception.NotFoundException;

/**
 * A case of a test run, the way the run keeps it for its results to be read.
 *
 * <p>The case reports everything the engine reported: its status, what it compared, the errors it ended with.
 *
 * <p>A value with inner structure is held softly: the value the tested rule returned, and a compared value such as
 * the whole result a {@code _res_} column compares. It is the heavy part of a case: a spreadsheet result holds every
 * step it calculated. When the application runs short of memory, the value is given back, and the case goes on
 * reporting the rest. A plain value and an error cost little and are kept.
 *
 * <p>A value that was given back is answered by {@link #getActualResult()}, or by the actual value of a comparison,
 * as a stand-in that {@link #isReleased(Object)} tells apart. The case has such a value again by running again,
 * see {@link #again()}; a workbook written from the case runs it by itself.
 */
public final class RetainedTestUnit implements ITestUnit {

    /** What a case answers for a value once the value was given back. */
    private static final Object RELEASED = new Object() {
        @Override
        public String toString() {
            return "Released to free memory";
        }
    };

    @Getter
    private final TestDescription test;
    @Getter
    private final @Nullable Object expectedResult;
    @Getter
    private final long executionTime;
    @Getter
    private final TestStatus resultStatus;
    @Getter
    private final List<ComparedResult> comparisonResults;
    @Getter
    private final int numberOfFailedTests;
    @Getter
    private final List<OpenLMessage> errors;
    @Getter
    private final String description;
    private final Kept actualResult;
    /** The test table the case belongs to. */
    private final TestSuiteMethod method;
    private final TestsRerun rerun;

    /**
     * Copies what a case reports, holding a value with inner structure through the reference the holder makes.
     *
     * <p>A run holds the value softly; see {@link #retain(TestUnitsResults, TestsRerun)}.
     *
     * @param unit   the case the engine reported
     * @param method the test table the case belongs to
     * @param rerun  runs the case again for a value it gave back
     * @param holder makes the reference a value with inner structure is held by
     */
    public RetainedTestUnit(ITestUnit unit,
                            TestSuiteMethod method,
                            TestsRerun rerun,
                            Function<Object, Reference<Object>> holder) {
        test = unit.getTest();
        expectedResult = unit.getExpectedResult();
        executionTime = unit.getExecutionTime();
        resultStatus = unit.getResultStatus();
        comparisonResults = unit.getComparisonResults().stream()
                .map(compared -> retain(compared, holder))
                .toList();
        numberOfFailedTests = unit.getNumberOfFailedTests();
        errors = unit.getErrors();
        description = unit.getDescription();
        actualResult = new Kept(unit.getActualResult(), holder);
        this.method = method;
        this.rerun = rerun;
    }

    /**
     * Keeps the cases of a test table the way a run keeps them, in place of the ones the engine reported.
     *
     * <p>The engine's cases hold every value the cases returned and compared, so they are let go. The table
     * reports the same, and the heavy values are held softly. A case that gave a value back runs again for it the
     * given way.
     *
     * @param results the results of a test table that has just run
     * @param rerun   runs the cases of the run again
     * @return the same results
     */
    public static TestUnitsResults retain(TestUnitsResults results, TestsRerun rerun) {
        var method = results.getTestSuite().getTestSuiteMethod();
        results.getTestUnits().replaceAll(unit -> new RetainedTestUnit(unit, method, rerun, SoftReference::new));
        return results;
    }

    /**
     * Runs the case again, for a value it gave back to free memory.
     *
     * <p>The case runs alone, against the project as it was compiled for the run. What it returns goes to the
     * caller, and this case keeps what it kept.
     *
     * @return the case as it ran again
     * @throws NotFoundException when the case cannot run again: the project was compiled again since the run
     */
    public ITestUnit again() {
        var ranAgain = rerun.runCase(method, test);
        if (ranAgain == null) {
            throw new NotFoundException("tests.execution.values.released.message");
        }
        return ranAgain;
    }

    /**
     * The values a case holds: what it returned, and what it compared.
     *
     * <p>A value held softly stays while the list is in use, so a case read through it cannot give the value back
     * half way.
     */
    public static List<@Nullable Object> heldValues(ITestUnit unit) {
        return Stream.concat(Stream.of(unit.getActualResult()),
                        unit.getComparisonResults().stream().map(ComparedResult::getActualValue))
                .toList();
    }

    /**
     * Whether what a case answered for a value stands for one that was given back to free memory.
     *
     * <p>Ask with what the case answered, and use the same answer afterwards: a value held softly can be given back
     * between two calls.
     */
    public static boolean isReleased(@Nullable Object value) {
        return value == RELEASED;
    }

    /** The error the case ended with, or else the value it returned. */
    @Override
    public @Nullable Object getActualResult() {
        return actualResult.get();
    }

    /** What the case returned, as a Run table writes it. A value the case gave back is had by running it again. */
    @Override
    public ParameterWithValueDeclaration getActualParam() {
        var actual = getActualResult();
        return isReleased(actual) ? again().getActualParam() : new ParameterWithValueDeclaration("actual", actual);
    }

    @Override
    public ParameterWithValueDeclaration[] getContextParams(TestUnitsResults objTestResult) {
        return TestUtils.getContextParams(objTestResult.getTestSuite(), test);
    }

    /**
     * The comparisons as a workbook writes them, every value named after the field it was compared in.
     *
     * <p>They are written the way the engine's case writes them, and only when they are asked for. A value the case
     * gave back is had by running it again.
     */
    @Override
    public List<ComparedResult> getResultParams() {
        var params = new ArrayList<ComparedResult>(comparisonResults.size());
        var released = false;
        for (var compared : comparisonResults) {
            // Read once: a value held softly can be given back to free memory between two reads.
            var actual = compared.getActualValue();
            released |= isReleased(actual);
            params.add(new ComparedResult(compared.getFieldName(),
                    declared(compared.getFieldName(), "expectedResult", compared.getExpectedValue()),
                    declared(compared.getFieldName(), "actualResult", actual),
                    compared.getStatus()));
        }
        return released ? again().getResultParams() : params;
    }

    private static ParameterWithValueDeclaration declared(@Nullable String fieldName,
                                                          String defaultName,
                                                          @Nullable Object value) {
        return new ParameterWithValueDeclaration(fieldName == null ? defaultName : fieldName, value);
    }

    /** A comparison as a run keeps it: an actual value with inner structure is held softly. */
    private static ComparedResult retain(ComparedResult compared, Function<Object, Reference<Object>> holder) {
        return Kept.costsLittle(compared.getActualValue()) ? compared : new RetainedComparedResult(compared, holder);
    }

    /** A comparison whose actual value has inner structure, held softly. */
    private static final class RetainedComparedResult extends ComparedResult {

        private final Kept actualValue;

        RetainedComparedResult(ComparedResult compared, Function<Object, Reference<Object>> holder) {
            super(compared.getFieldName(), compared.getExpectedValue(), null, compared.getStatus());
            actualValue = new Kept(compared.getActualValue(), holder);
        }

        @Override
        public @Nullable Object getActualValue() {
            return actualValue.get();
        }
    }

    /** A value as a run keeps it: as it is when it costs little, and softly when it has inner structure. */
    private static final class Kept {

        private final @Nullable Object value;
        private final @Nullable Reference<Object> reference;

        Kept(@Nullable Object value, Function<Object, Reference<Object>> holder) {
            if (costsLittle(value)) {
                this.value = value;
                this.reference = null;
            } else {
                this.value = null;
                this.reference = holder.apply(value);
            }
        }

        /** A plain value, an error, or nothing at all. */
        static boolean costsLittle(@Nullable Object value) {
            return value == null || value instanceof Throwable
                    || ParameterWithValueDeclaration.getParamType(value).isSimple();
        }

        @Nullable Object get() {
            if (reference == null) {
                return value;
            }
            var held = reference.get();
            return held == null ? RELEASED : held;
        }
    }
}
