package org.openl.studio.projects.model.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.types.IOpenClass;

/**
 * Validates the run result of a spreadsheet table against a real execution.
 */
class RunExecutionResultMapperSpreadsheetTest {

    private static final String SRC = "test/rules/EPBDS-16160/generalProject.xlsx";

    /**
     * A spreadsheet result is written as the bean class generated for the spreadsheet, so the schema must describe
     * that bean. Describing the raw value instead documents the engine's internal row and column tables, which share
     * no property with the written result.
     */
    @Test
    void resultSchemaDescribesTheWrittenSpreadsheetResult() {
        var result = runMyRule(true);

        assertNotNull(result.result(), "MyRule returns a spreadsheet result");
        assertNotNull(result.resultSchema(), "a spreadsheet result is described by a schema");

        var schemaProperties = result.resultSchema().get("properties");
        assertNotNull(schemaProperties, "the schema of a spreadsheet result lists its steps");
        assertFalse(fieldNames(result.result()).isEmpty(), "MyRule has at least one step");
        assertTrue(fieldNames(schemaProperties).containsAll(fieldNames(result.result())),
                "every property of the result is described by the schema");
        assertFalse(fieldNames(schemaProperties).contains("results"),
                "the schema describes the spreadsheet steps, not the engine's internal tables");
    }

    /** The layout repeats the values of the result, so a caller that does not ask for it is not sent it. */
    @Test
    void resultSpreadsheetIsWrittenOnlyWhenItIsAskedFor() {
        var result = runMyRule(false);

        assertNotNull(result.result(), "MyRule returns a spreadsheet result");
        assertNull(result.resultSpreadsheet(), "the table it was calculated by was not asked for");
    }

    /**
     * A spreadsheet result is also laid out by the rows and the columns of the spreadsheet it was calculated by, so
     * that a client can show it as the table its author wrote instead of a flat list of properties.
     */
    @Test
    void resultSpreadsheetLaysOutTheCalculatedTable() {
        var result = runMyRule(true);
        var spreadsheet = result.resultSpreadsheet();

        assertNotNull(spreadsheet, "a spreadsheet result carries the table it was calculated by");
        assertFalse(spreadsheet.rows().isEmpty(), "MyRule has at least one step");
        assertFalse(spreadsheet.columns().isEmpty(), "MyRule has at least one column");
        assertEquals(spreadsheet.rows().size(), spreadsheet.cells().size(), "every step is a row of values");
        spreadsheet.cells().forEach(row ->
                assertEquals(spreadsheet.columns().size(), row.size(), "every row holds a value per column"));

        var cells = spreadsheet.cells().stream().flatMap(List::stream).toList();
        fieldNames(result.result()).forEach(step ->
                assertTrue(cells.contains(result.result().get(step)),
                        "the value of " + step + " stands in the cell it was calculated in"));
    }

    /**
     * Runs the single spreadsheet of the test project the same way the run API does.
     */
    private static RunExecutionResult runMyRule(boolean withSpreadsheet) {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var openClass = compiled.getOpenClassWithErrors();
        var myRule = openClass.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        var results = new TestSuite(new TestDescription(myRule, null, new Object[0], null))
                .invokeSequentially(openClass, 1);

        var objectMapper = new ObjectMapper();
        var schemaGenerator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);
        return new RunExecutionResultMapper(objectMapper, schemaGenerator, null).mapResult(results, withSpreadsheet);
    }

    private static List<String> fieldNames(JsonNode node) {
        var names = new ArrayList<String>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
