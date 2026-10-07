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
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ExecutionValueMapper;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.model.SpreadsheetResultView;
import org.openl.studio.projects.service.trace.TableInputParserService;

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

    /**
     * Writes what a run returned.
     *
     * <p>The value is written the way OpenL Rule Services publishes it, with the schema that describes it. A
     * spreadsheet result can also be laid out by its rows and columns, for a caller that shows it as the table
     * it comes from; that layout repeats the values, so it is written only when it is asked for.
     *
     * <p>The runtime context the table was run with is written next to the parameters, with the fields that were
     * entered.
     *
     * @param results         what the run produced
     * @param withSpreadsheet whether to lay a spreadsheet result out by its rows and columns
     * @return the result of the run
     */
    public RunExecutionResult mapResult(TestUnitsResults results, boolean withSpreadsheet) {
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
        SpreadsheetResultView resultSpreadsheet = null;
        if (!(actualResult instanceof Throwable)) {
            // The schema describes the value as it is written, so that every property of the result is described.
            var convertedResult = valueMapper.convert(actualResult);
            resultValue = valueMapper.writeConverted(convertedResult);
            resultSchema = valueMapper.schemaOf(convertedResult);
            // A spreadsheet is also laid out by its rows and columns, so that it reads as the table it comes from.
            resultSpreadsheet = withSpreadsheet ? valueMapper.spreadsheetOf(actualResult) : null;
        }

        // Map input parameters
        var executionParams = firstUnit.getTest().getExecutionParams();
        var executionParamNames = results.getTestDataColumnDisplayNames();
        var parameters = IntStream.range(0, executionParams.length)
                .mapToObj(i -> valueMapper.writeParameter(executionParams[i], executionParamNames[i]))
                .toList();

        // Map errors
        var errors = new ArrayList<MessageDescription>();
        firstUnit.getErrors().stream()
                .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                .sorted(Comparator.comparing(MessageDescription::severity).thenComparing(MessageDescription::id))
                .forEach(errors::add);

        return baseResult(results)
                .result(resultValue)
                .resultSchema(resultSchema)
                .resultSpreadsheet(resultSpreadsheet)
                .parameters(parameters)
                .contextParameters(enteredContext(firstUnit.getTest()))
                .errors(errors)
                .build();
    }

    /**
     * The runtime context the table was run with.
     *
     * <p>The context is one value, written under the key the input carries it under. It holds only the fields that
     * were set, so it reads as what was entered.
     *
     * <p>A run given no context, or a context with no field set, has none.
     */
    private List<ParameterValue> enteredContext(TestDescription test) {
        if (!test.isRuntimeContextDefined()) {
            return List.of();
        }
        ObjectNode fields = objectMapper.valueToTree(test.getRuntimeContext());
        fields.properties().removeIf(field -> field.getValue().isNull());
        return fields.isEmpty()
                ? List.of()
                : List.of(ParameterValue.builder().name(TableInputParserService.RUNTIME_CONTEXT).value(fields).build());
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
