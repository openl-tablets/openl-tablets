package org.openl.studio.projects.model.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ParameterValue;

public class RunExecutionResultMapper {

    private static final double NANOS_IN_MILLISECOND = 1_000_000.0;

    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;
    private final SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy;

    public RunExecutionResultMapper(ObjectMapper objectMapper,
                                    SchemaGenerator schemaGenerator,
                                    SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy) {
        this.objectMapper = objectMapper;
        this.schemaGenerator = schemaGenerator;
        this.sprNamingStrategy = sprNamingStrategy;
    }

    public RunExecutionResult mapResult(TestUnitsResults results) {
        var testUnits = results.getTestUnits();
        if (testUnits.isEmpty()) {
            return new RunExecutionResult(
                    results.getName(),
                    TableUtils.makeTableId(results.getTestSuite().getUri()),
                    results.getExecutionTime() / NANOS_IN_MILLISECOND,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        ITestUnit firstUnit = testUnits.getFirst();

        // Convert result the same way as legacy TestDownloadController#manualJson:
        // SpreadsheetResult must be converted to Map/bean with proper naming strategy.
        // getActualResult() returns Throwable when execution fails — skip conversion in that case.
        Object actualResult = firstUnit.getActualResult();
        JsonNode resultValue = null;
        ObjectNode resultSchema = null;
        if (!(actualResult instanceof Throwable)) {
            Object convertedResult = SpreadsheetResult.convertSpreadsheetResult(actualResult, sprNamingStrategy);
            resultValue = convertedResult != null ? objectMapper.valueToTree(convertedResult) : null;
            var actualParam = firstUnit.getActualParam();
            if (actualParam != null) {
                resultSchema = schemaGenerator.generateSchema(actualParam.getType().getInstanceClass());
            }
        }

        // Map input parameters
        var executionParams = firstUnit.getTest().getExecutionParams();
        var executionParamNames = results.getTestDataColumnDisplayNames();
        var parameters = IntStream.range(0, executionParams.length).mapToObj(i -> {
            var param = executionParams[i];
            return ParameterValue.builder()
                    .name(param.getName())
                    .value(objectMapper.valueToTree(param.getValue()))
                    .schema(schemaGenerator.generateSchema(param.getType().getInstanceClass()))
                    .description(executionParamNames[i])
                    .build();
        }).toList();

        // Map context parameters
        var contextParams = firstUnit.getContextParams(results);
        var contextParamNames = results.getContextColumnDisplayNames();
        var contextParameters = IntStream.range(0, contextParams.length).mapToObj(i -> {
            var param = contextParams[i];
            return ParameterValue.builder()
                    .name(param.getName())
                    .value(objectMapper.valueToTree(param.getValue()))
                    .description(contextParamNames[i])
                    .build();
        }).toList();

        // Map errors
        List<MessageDescription> errors = new ArrayList<>();
        firstUnit.getErrors().stream()
                .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                .sorted(Comparator.comparing(MessageDescription::getSeverity).thenComparing(MessageDescription::getId))
                .forEach(errors::add);

        String tableName = results.getTestSuite().getTestSuiteMethod() != null
                ? TableSyntaxNodeUtils.getTestName(results.getTestSuite().getTestSuiteMethod())
                : results.getName();

        return new RunExecutionResult(
                tableName,
                TableUtils.makeTableId(results.getTestSuite().getUri()),
                results.getExecutionTime() / NANOS_IN_MILLISECOND,
                resultValue,
                resultSchema,
                parameters,
                contextParameters,
                errors
        );
    }
}
