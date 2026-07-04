package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Regression test for EPBDS-16160.
 * <p>
 * A spreadsheet whose cell returns a generic {@code SpreadsheetResult} (here {@code = new
 * SpreadsheetResult()}) produces a self-referential generated bean class. The victools JSON schema
 * generator used to recurse on such a class and throw {@link StackOverflowError}, which crashed
 * Trace schema generation. The schema must now be generated with a {@code $ref} for the
 * circular type instead of failing.
 */
class GeneralProjectTraceReproTest {

    private static final String SRC = "test/rules/EPBDS-16160/generalProject.xlsx";

    @Test
    @DisplayName("Trace node schema for a self-referential spreadsheet result is a $ref, not a StackOverflowError")
    void traceParameterSchemaDoesNotStackOverflowOnSelfReferentialSpreadsheet() {
        IOpenClass module = new RulesEngineFactory<>(SRC).getCompiledOpenClass().getOpenClass();
        IOpenMethod myRule = module.getMethod("MyRule", IOpenClass.EMPTY);
        assertNotNull(myRule, "MyRule must compile");
        // The return type is the custom spreadsheet result, whose generated bean class is self-referential.
        IOpenClass returnType = myRule.getType();
        assertInstanceOf(CustomSpreadsheetResultOpenClass.class, returnType,
                "MyRule must return a custom spreadsheet result");

        var objectMapper = new ObjectMapper();
        var schemaGenerator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);
        var mapper = new TraceDebugMapper(objectMapper, schemaGenerator, new TraceParameterRegistry());

        // The schema is generated from the declared type; a non-null placeholder value is enough to
        // drive buildParameterValue down the lazy, schema-generating branch.
        var param = new ParameterWithValueDeclaration("return", new Object(), returnType);

        // Before the fix this threw StackOverflowError from victools schema generation.
        var parameterValue = assertDoesNotThrow(() -> mapper.buildParameterValue(param, true),
                "Schema generation must not overflow on a self-referential bean");
        assertNotNull(parameterValue);
        var schema = parameterValue.schema();
        assertNotNull(schema, "Self-referential bean must still yield a schema");
        assertTrue(schema.toString().contains("$ref"),
                () -> "Circular type must be expressed as a $ref: " + schema);
        assertTrue(parameterValue.lazy(), "Non-simple value must be lazy");

        // The Run/Tests API path builds the schema from the generic instance class
        // (SpreadsheetResult.class), which is not self-referential and must keep working.
        assertDoesNotThrow(() -> schemaGenerator.generateSchema(returnType.getInstanceClass()));
    }
}
