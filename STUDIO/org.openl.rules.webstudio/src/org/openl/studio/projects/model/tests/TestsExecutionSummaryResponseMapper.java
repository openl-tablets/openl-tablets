package org.openl.studio.projects.model.tests;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;

public class TestsExecutionSummaryResponseMapper {

    private static final double NANOS_IN_MILLISECOND = 1_000_000.0;

    private static final Comparator<TestUnitsResults> TEST_COMPARATOR = Comparator
            .nullsLast(Comparator.comparingInt(TestUnitsResults::getNumberOfFailures).reversed()
                    .thenComparing(TestUnitsResults::getName));

    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;

    public TestsExecutionSummaryResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaGenerator = initSchemaGenerator(this.objectMapper);
    }

    private SchemaGenerator initSchemaGenerator(ObjectMapper objectMapper) {
        var schemaGeneratorConfig = new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new Swagger2Module())
                .build();
        return new SchemaGenerator(schemaGeneratorConfig);
    }

    public TestsExecutionSummary mapExecutionSummary(List<TestUnitsResults> testUnitsResults) {
        var builder = TestsExecutionSummary.builder();
        testUnitsResults.stream()
                .sorted(TEST_COMPARATOR)
                .map(this::mapToTestCaseResult)
                .forEach(builder::putTestCase);
        return builder.build();
    }

    public TestCaseExecutionResult mapToTestCaseResult(TestUnitsResults testCase) {
        var builder = TestCaseExecutionResult.builder()
                .name(TableSyntaxNodeUtils.getTestName(testCase.getTestSuite().getTestSuiteMethod()))
                .tableId(TableUtils.makeTableId(testCase.getTestSuite().getUri()))
                .description(testCase.getTestSuite().getTestSuiteMethod().getSyntaxNode().getTableProperties().getDescription())
                .executionTimeMs(testCase.getExecutionTime() / NANOS_IN_MILLISECOND)
                .numberOfTests(testCase.getNumberOfTestUnits())
                .numberOfFailures(testCase.getNumberOfFailures());

        testCase.getTestUnits().stream()
                .map(tetUnit -> mapToTestUnitResult(testCase, tetUnit))
                .forEach(builder::putTestUnit);
        return builder.build();
    }

    private TestUnitExecutionResult mapToTestUnitResult(TestUnitsResults testCase, ITestUnit testUnit) {
        var builder = TestUnitExecutionResult.builder()
                .id(testUnit.getTest().getId())
                .description(testUnit.getTest().getDescription())
                .status(testUnit.getResultStatus())
                .executionTimeMs(testUnit.getExecutionTime() / 1_000_000.0);

        // Map test assertions
        var results = testUnit.getComparisonResults();
        var resultColumnNames = testUnit.getActualResult() instanceof Exception
                ? testCase.getTestErrorColumnDisplayNames()
                : testCase.getTestResultColumnDisplayNames();
        IntStream.range(0, results.size())
                .mapToObj(i -> mapToTestAssertionResult(results.get(i))
                        .description(resultColumnNames[i])
                        .build())
                .forEach(builder::putTestAssertion);

        // Map execution parameters
        var executionParams = testUnit.getTest().getExecutionParams();
        var executionParamNames = testCase.getTestDataColumnDisplayNames();
        IntStream.range(0, executionParams.length).mapToObj(i -> {
            var executionParam = executionParams[i];
            return TestParameterValue.builder()
                    .name(executionParam.getName())
                    .value(objectMapper.valueToTree(executionParam.getValue()))
                    .schema(schemaGenerator.generateSchema(executionParam.getType().getInstanceClass()))
                    .description(executionParamNames[i])
                    .build();
        }).forEach(builder::putParameter);

        // Map context parameters
        var contextParams = testUnit.getContextParams(testCase);
        var contextParamNames = testCase.getContextColumnDisplayNames();
        IntStream.range(0, contextParams.length).mapToObj(i -> {
            var contextParam = contextParams[i];
            return TestParameterValue.builder()
                    .name(contextParam.getName())
                    .value(objectMapper.valueToTree(contextParam.getValue()))
                    .description(contextParamNames[i])
                    .build();
        }).forEach(builder::putContextParameter);

        testUnit.getErrors().stream()
                .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                .sorted(Comparator.comparing(MessageDescription::getSeverity).thenComparing(MessageDescription::getId))
                .forEach(builder::putErrorMessage);

        return builder.build();
    }

    private TestAssertionExecutionResult.Builder mapToTestAssertionResult(ComparedResult assertion) {
        return TestAssertionExecutionResult.builder()
                .status(assertion.getStatus())
                .actualValue(objectMapper.valueToTree(assertion.getActualValue()))
                .expectedValue(objectMapper.valueToTree(assertion.getExpectedValue()));
    }
}
