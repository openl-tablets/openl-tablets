package org.openl.studio.projects.model.run;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        var result = runMyRule();

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

    /**
     * Runs the single spreadsheet of the test project the same way the run API does.
     */
    private static RunExecutionResult runMyRule() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var openClass = compiled.getOpenClassWithErrors();
        var myRule = openClass.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");

        var results = new TestSuite(new TestDescription(myRule, null, new Object[0], null))
                .invokeSequentially(openClass, 1);

        var objectMapper = new ObjectMapper();
        var schemaGenerator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);
        return new RunExecutionResultMapper(objectMapper, schemaGenerator, null).mapResult(results);
    }

    private static List<String> fieldNames(JsonNode node) {
        var names = new ArrayList<String>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
