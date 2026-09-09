package org.openl.studio.projects.model.tests;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.studio.projects.model.ExecutionValueMapper;
import org.openl.studio.projects.model.ParameterValue;

public class TestsExecutionSummaryResponseMapper {

    private static final double NANOS_IN_MILLISECOND = 1_000_000.0;

    /** The name the whole returned value of a case is written under. */
    private static final String RESULT_NAME = "result";

    private static final Comparator<TestUnitsResults> TEST_COMPARATOR = Comparator
            .nullsLast(Comparator.comparingInt(TestUnitsResults::getNumberOfFailures).reversed()
                    .thenComparing(TestUnitsResults::getName));

    private final ObjectMapper objectMapper;
    private final ExecutionValueMapper valueMapper;

    public TestsExecutionSummaryResponseMapper(ObjectMapper objectMapper,
                                               SchemaGenerator schemaGenerator,
                                               SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy) {
        this.objectMapper = objectMapper;
        this.valueMapper = new ExecutionValueMapper(objectMapper, schemaGenerator, sprNamingStrategy);
    }

    public TestsExecutionSummary mapExecutionSummary(List<TestUnitsResults> testUnitsResults, TestExecutionSummaryQuery query, Pageable page) {
        var builder = TestsExecutionSummary.builder().page(page).total(testUnitsResults.size());
        calculateSummaryStats(testUnitsResults, builder);
        testUnitsResults.stream()
                .sorted(TEST_COMPARATOR)
                .skip(page.getOffset())
                .limit(page.getPageSize())
                .map(testCase -> mapToTestCaseResult(testCase, query))
                .forEach(builder::putTestCase);
        return builder.build();
    }

    public void calculateSummaryStats(List<TestUnitsResults> testUnitsResults, TestsExecutionSummary.Builder builder) {
        var executionTimeMs = 0D;
        var numberOfTests = 0;
        var numberOfFailures = 0;
        for (TestUnitsResults testCase : testUnitsResults) {
            executionTimeMs += testCase.getExecutionTime() / NANOS_IN_MILLISECOND;
            numberOfTests += testCase.getNumberOfTestUnits();
            numberOfFailures += testCase.getNumberOfFailures();
        }
        builder.executionTimeMs(executionTimeMs)
                .numberOfTests(numberOfTests)
                .numberOfFailures(numberOfFailures);
    }

    public TestCaseExecutionResult mapToTestCaseResult(TestUnitsResults testCase, TestExecutionSummaryQuery query) {
        var builder = TestCaseExecutionResult.builder()
                .name(TableSyntaxNodeUtils.getTestName(testCase.getTestSuite().getTestSuiteMethod()))
                .tableId(TableUtils.makeTableId(testCase.getTestSuite().getUri()))
                .description(testCase.getTestSuite().getTestSuiteMethod().getSyntaxNode().getTableProperties().getDescription())
                .executionTimeMs(testCase.getExecutionTime() / NANOS_IN_MILLISECOND)
                .numberOfTests(testCase.getNumberOfTestUnits())
                .numberOfFailures(testCase.getNumberOfFailures())
                // A Run table states no expected values, so its cases neither pass nor fail.
                .runTable(testCase.getTestSuite().getTestSuiteMethod().isRunMethod() ? Boolean.TRUE : null);

        testCase.getFilteredTestUnits(query.failedOnly(), query.failures()).stream()
                .map(tetUnit -> mapToTestUnitResult(testCase, tetUnit, query))
                .forEach(builder::testUnit);
        return builder.build();
    }

    /**
     * Writes one case of a test table: what it was given, what came out and how the two compare.
     *
     * @param testCase the results of the test table the case belongs to
     * @param testUnit the case
     * @param query    what the summary is asked for
     */
    public TestUnitExecutionResult mapToTestUnitResult(TestUnitsResults testCase,
                                                       ITestUnit testUnit,
                                                       TestExecutionSummaryQuery query) {
        var builder = TestUnitExecutionResult.builder()
                .id(testUnit.getTest().getId())
                .description(testUnit.getTest().getDescription())
                .status(testUnit.getResultStatus())
                .executionTimeMs(testUnit.getExecutionTime() / 1_000_000.0)
                .result(query.compoundResult() ? wholeResult(testUnit, query) : null);

        // Map test assertions. Skip them for TR_EXCEPTION (unexpected exception thrown by the test)
        // to mirror the legacy RichFaces UI (test.xhtml renders only #{testCase.errors} when
        // compareResult == 'TR_EXCEPTION'). Such tests have no _error_ columns so column display
        // names are empty, which previously caused ArrayIndexOutOfBoundsException.
        if (testUnit.getResultStatus() != TestStatus.TR_EXCEPTION) {
            var results = testUnit.getComparisonResults();
            var resultColumnNames = testUnit.getActualResult() instanceof Exception
                    ? testCase.getTestErrorColumnDisplayNames()
                    : testCase.getTestResultColumnDisplayNames();
            IntStream.range(0, results.size())
                    .mapToObj(i -> mapToTestAssertionResult(results.get(i),
                            i < resultColumnNames.length
                                    ? resultColumnNames[i]
                                    : null))
                    .forEach(builder::testAssertion);
        }

        // Map execution parameters
        var executionParams = testUnit.getTest().getExecutionParams();
        var executionParamNames = testCase.getTestDataColumnDisplayNames();
        IntStream.range(0, executionParams.length)
                .mapToObj(i -> query.lazyValues()
                        ? valueMapper.writeParameterLazily(executionParams[i], executionParamNames[i])
                        : valueMapper.writeParameter(executionParams[i], executionParamNames[i]))
                .forEach(builder::parameter);

        // Map context parameters
        var contextParams = testUnit.getContextParams(testCase);
        var contextParamNames = testCase.getContextColumnDisplayNames();
        IntStream.range(0, contextParams.length).mapToObj(i -> {
            var contextParam = contextParams[i];
            return ParameterValue.builder()
                    .name(contextParam.getName())
                    .value(objectMapper.valueToTree(contextParam.getValue()))
                    .description(contextParamNames[i])
                    .build();
        }).forEach(builder::contextParameter);

        testUnit.getErrors().stream()
                .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                .sorted(Comparator.comparing(MessageDescription::severity).thenComparing(MessageDescription::id))
                .forEach(builder::error);

        return builder.build();
    }

    /**
     * Writes a compared value in the shape it is published in.
     *
     * <p>A test that states no expectation reports it as an explicit {@code null}, which tells the reader that the
     * assertion has no expected value rather than that the field is missing.
     */
    private JsonNode writeAssertionValue(@Nullable Object value) {
        return objectMapper.valueToTree(valueMapper.convert(value));
    }

    /**
     * The whole value the tested rule returned, for a summary asked with {@code compoundResult}.
     *
     * <p>A test that ended in an exception returns the exception itself, which the errors of the unit report
     * instead.
     *
     * <p>A summary asked for lazy values refers to a value with inner structure instead of writing it, and
     * carries no schema: the screen that asks for them only reads the values.
     */
    private @Nullable ParameterValue wholeResult(ITestUnit testUnit, TestExecutionSummaryQuery query) {
        var actualResult = testUnit.getActualResult();
        if (actualResult instanceof Throwable) {
            return null;
        }
        if (query.lazyValues()) {
            // A returned value with inner structure is the largest thing a case carries; it waits to be asked for.
            return valueMapper.writeParameterLazily(testUnit.getActualParam(), null)
                    .toBuilder()
                    .name(RESULT_NAME)
                    .build();
        }
        var converted = valueMapper.convert(actualResult);
        return ParameterValue.builder()
                .name(RESULT_NAME)
                .value(valueMapper.writeConverted(converted))
                .schema(valueMapper.schemaOf(converted))
                .build();
    }

    private TestAssertionExecutionResult mapToTestAssertionResult(ComparedResult assertion, String description) {
        return TestAssertionExecutionResult.builder()
                .status(assertion.getStatus())
                .actualValue(writeAssertionValue(assertion.getActualValue()))
                .expectedValue(writeAssertionValue(assertion.getExpectedValue()))
                .description(description)
                .build();
    }
}
