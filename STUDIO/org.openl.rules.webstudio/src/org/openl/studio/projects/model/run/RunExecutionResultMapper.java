package org.openl.studio.projects.model.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;

import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ExecutionValueMapper;
import org.openl.studio.projects.model.ParameterValue;

public class RunExecutionResultMapper {

    private static final double NANOS_IN_MILLISECOND = 1_000_000.0;

    private final ObjectMapper objectMapper;
    private final ExecutionValueMapper valueMapper;

    public RunExecutionResultMapper(ObjectMapper objectMapper,
                                    SchemaGenerator schemaGenerator,
                                    SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy) {
        this.objectMapper = objectMapper;
        this.valueMapper = new ExecutionValueMapper(objectMapper, schemaGenerator, sprNamingStrategy);
    }

    public RunExecutionResult mapResult(TestUnitsResults results) {
        var testUnits = results.getTestUnits();
        if (testUnits.isEmpty()) {
            return baseResult(results)
                    .parameters(List.of())
                    .contextParameters(List.of())
                    .errors(List.of())
                    .build();
        }

        var firstUnit = testUnits.getFirst();

        // getActualResult() returns a Throwable when execution fails — such a result is reported through the errors.
        var actualResult = firstUnit.getActualResult();
        JsonNode resultValue = null;
        ObjectNode resultSchema = null;
        if (!(actualResult instanceof Throwable)) {
            // The schema describes the value as it is written, so that every property of the result is described.
            var convertedResult = valueMapper.convert(actualResult);
            resultValue = valueMapper.writeConverted(convertedResult);
            resultSchema = valueMapper.schemaOf(convertedResult);
        }

        // Map input parameters
        var executionParams = firstUnit.getTest().getExecutionParams();
        var executionParamNames = results.getTestDataColumnDisplayNames();
        var parameters = IntStream.range(0, executionParams.length)
                .mapToObj(i -> valueMapper.writeParameter(executionParams[i], executionParamNames[i]))
                .toList();

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
        var errors = new ArrayList<MessageDescription>();
        firstUnit.getErrors().stream()
                .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                .sorted(Comparator.comparing(MessageDescription::severity).thenComparing(MessageDescription::id))
                .forEach(errors::add);

        return baseResult(results)
                .result(resultValue)
                .resultSchema(resultSchema)
                .parameters(parameters)
                .contextParameters(contextParameters)
                .errors(errors)
                .build();
    }

    /**
     * Starts a result with the metadata every run carries: the executed table and the execution time.
     */
    private static RunExecutionResult.RunExecutionResultBuilder baseResult(TestUnitsResults results) {
        return RunExecutionResult.builder()
                .tableName(resolveTableName(results))
                .tableId(TableUtils.makeTableId(results.getTestSuite().getUri()))
                .executionTimeMs(results.getExecutionTime() / NANOS_IN_MILLISECOND);
    }

    /**
     * Resolves the name of the executed table.
     *
     * <p>A run wraps the requested table into a virtual test suite, whose own name is a placeholder. The name is
     * therefore taken from the executed method, which is the table the caller asked to run.</p>
     *
     * <p>The name is the table identifier, the same value the tables API reports, so that a caller can look the
     * executed table up by it.</p>
     */
    private static String resolveTableName(TestUnitsResults results) {
        return results.getTestSuite().getTestedMethod().getName();
    }
}
