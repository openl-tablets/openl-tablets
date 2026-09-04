package org.openl.studio.projects.model.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.Pageable;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;

/**
 * Validates the test results of a spreadsheet table against a real execution.
 *
 * <p>The test project runs {@code PolicyOverrideCalculationTest}, which passes a spreadsheet result as an argument,
 * and {@code WrapperTest}, which asserts a step whose value is a spreadsheet result.
 */
class TestsExecutionSummaryResponseMapperSpreadsheetTest {

    private static final String SRC = "test/rules/EPBDS-16463/sprTests.xlsx";

    private TestsExecutionSummary summary;

    @BeforeEach
    void runTests() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var openClass = compiled.getOpenClassWithErrors();

        List<TestUnitsResults> results = openClass.getMethods().stream()
                .filter(TestSuiteMethod.class::isInstance)
                .map(TestSuiteMethod.class::cast)
                .map(testMethod -> new TestSuite(testMethod).invokeSequentially(openClass, 1))
                .toList();
        assertEquals(2, results.size(), "both test tables must compile and run");

        var objectMapper = new ObjectMapper();
        var schemaGenerator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);
        var mapper = new TestsExecutionSummaryResponseMapper(objectMapper, schemaGenerator, null);
        summary = mapper.mapExecutionSummary(results, new TestExecutionSummaryQuery(false, 5), Pageable.unpaged());
    }

    /**
     * A spreadsheet result argument is echoed — and described — through the bean class generated for it. Written as
     * it stands it comes out as the engine's internal row and column tables, which no client can read back.
     */
    @Test
    void parameterOfSpreadsheetResultTypeIsWrittenAsItsBean() {
        var parameter = onlyTestUnit("PolicyOverrideCalculationTest").parameters().getFirst();

        assertEquals("ratingDetails", parameter.name());
        assertNotNull(parameter.value(), "a spreadsheet result argument carries its value");
        assertTrue(fieldNames(parameter.value()).containsAll(List.of("plan", "total")),
                "the argument is written with the steps of the spreadsheet it belongs to");

        assertNotNull(parameter.schema(), "a spreadsheet result argument is described by a schema");
        var schemaProperties = parameter.schema().get("properties");
        assertNotNull(schemaProperties, "the schema of a spreadsheet result lists its steps");
        assertTrue(fieldNames(schemaProperties).containsAll(fieldNames(parameter.value())),
                "every property of the argument is described by the schema");
    }

    /**
     * An assertion on a step whose value is a spreadsheet result reports that value in the same published shape.
     */
    @Test
    void assertionOfSpreadsheetResultValueIsWrittenAsItsBean() {
        var assertion = onlyTestUnit("WrapperTest").testAssertions().stream()
                .filter(a -> a.actualValue() != null && a.actualValue().isObject())
                .findFirst()
                .orElseThrow(() -> new AssertionError("WrapperTest asserts a spreadsheet result step"));

        var actualFields = fieldNames(assertion.actualValue());
        assertFalse(actualFields.isEmpty(), "a spreadsheet result assertion carries the steps of the spreadsheet");
        assertFalse(actualFields.contains("results"),
                "the assertion reports the spreadsheet steps, not the engine's internal tables");
    }

    private TestUnitExecutionResult onlyTestUnit(String testName) {
        var testCase = summary.getContent().stream()
                .filter(t -> testName.equals(t.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(testName + " must be executed"));
        assertEquals(1, testCase.testUnits().size(), testName + " has a single test case");
        return testCase.testUnits().getFirst();
    }

    private static List<String> fieldNames(JsonNode node) {
        var names = new ArrayList<String>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
