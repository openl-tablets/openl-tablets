package org.openl.studio.projects.model.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.RequiredArgsConstructor;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.common.utils.SpreadsheetResultBean;
import org.openl.studio.config.SafeSchemaGenerator;
import org.openl.studio.projects.model.ParameterValue;

@RequiredArgsConstructor
public class RunExecutionResultMapper {

    private static final double NANOS_IN_MILLISECOND = 1_000_000.0;

    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;
    private final SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy;

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

        // Convert result the same way as legacy TestDownloadController#manualJson:
        // SpreadsheetResult must be converted to Map/bean with proper naming strategy.
        // getActualResult() returns Throwable when execution fails — skip conversion in that case.
        var actualResult = firstUnit.getActualResult();
        JsonNode resultValue = null;
        ObjectNode resultSchema = null;
        if (!(actualResult instanceof Throwable)) {
            Object convertedResult = SpreadsheetResult.convertSpreadsheetResult(actualResult, sprNamingStrategy);
            resultValue = convertedResult != null ? objectMapper.valueToTree(convertedResult) : null;
            var actualParam = firstUnit.getActualParam();
            if (actualParam != null) {
                resultSchema = SafeSchemaGenerator.generate(schemaGenerator, actualParam.getType().getInstanceClass());
            }
        }

        // Map input parameters
        var executionParams = firstUnit.getTest().getExecutionParams();
        var executionParamNames = results.getTestDataColumnDisplayNames();
        var parameters = IntStream.range(0, executionParams.length).mapToObj(i -> {
            var param = executionParams[i];
            // A spreadsheet result argument is echoed — and described — through the bean class generated for it,
            // the same shape the result itself is written in. Written as it stands it comes out as the engine's
            // internal row/column tables, which no client can read back.
            var spreadsheetResult = SpreadsheetResultBean.of(param.getType());
            var value = spreadsheetResult == null
                    ? param.getValue()
                    : SpreadsheetResult.convertSpreadsheetResult(param.getValue(), spreadsheetResult.beanClass(),
                            param.getType(), sprNamingStrategy);
            return ParameterValue.builder()
                    .name(param.getName())
                    .value(objectMapper.valueToTree(value))
                    .schema(SafeSchemaGenerator.generate(schemaGenerator,
                            spreadsheetResult != null
                                    ? spreadsheetResult.beanClass()
                                    : param.getType().getInstanceClass()))
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
