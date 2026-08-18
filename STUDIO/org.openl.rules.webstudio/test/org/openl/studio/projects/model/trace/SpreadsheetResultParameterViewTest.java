package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.service.trace.TableInputParserServiceImpl;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IOpenClass;

/**
 * A traced spreadsheet result parameter is written, and described, by the step names of its spreadsheet.
 *
 * <p>An array of spreadsheet results is published through the generated bean class, exactly as a single
 * spreadsheet result is. Before that, an array fell past the check and was written — and described — as the
 * engine's internal row and column tables, which no client can read.
 */
class SpreadsheetResultParameterViewTest {

    private static final String SRC = "test/rules/trace-debug/sprParamProject.xlsx";

    private static XlsModuleOpenClass module;
    private static ObjectMapper projectMapper;

    private final TableInputParserServiceImpl parser = new TableInputParserServiceImpl();

    @BeforeAll
    static void compile() throws ClassNotFoundException {
        module = (XlsModuleOpenClass) new RulesEngineFactory<>(SRC).getCompiledOpenClass().getOpenClass();
        var factory = new ProjectJacksonObjectMapperFactoryBean();
        factory.setXlsModuleOpenClass(module);
        projectMapper = factory.createJacksonObjectMapper();
    }

    private static TraceDebugMapper mapper() {
        var objectMapper = new ObjectMapper();
        return new TraceDebugMapper(objectMapper,
                new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper),
                new TraceParameterRegistry());
    }

    @Test
    @DisplayName("An array of spreadsheet results is written and described by its step names")
    void arrayParameterValueAndSchemaAgree() {
        var ratingDetailsType = module.findType("SpreadsheetResultRatingDetails");
        var target = module.getMethod("TotalOfAll", new IOpenClass[]{ratingDetailsType.getArrayType(1)});
        assertNotNull(target, "TotalOfAll must compile");
        var parameterType = target.getSignature().getParameterType(0);
        var value = parser.parseInput("{\"details\":[{\"Total\":1.0},{\"Total\":2.0}]}", target, projectMapper)
                .params()[0];

        var view = mapper().buildParameterValue(
                new ParameterWithValueDeclaration("details", value, parameterType), false, true);

        // The value carries the step names of the spreadsheet, not the engine's rows and columns.
        assertNotNull(view.value());
        assertTrue(view.value().isArray(), "the value stays an array");
        assertEquals(2.0, view.value().get(1).get("total").asDouble());

        // The schema describes that very shape, element for element.
        assertNotNull(view.schema(), "an array of spreadsheet results is describable");
        assertEquals("array", view.schema().get("type").asText());
        var itemProperties = view.schema().get("items").get("properties");
        assertNotNull(itemProperties, () -> "the element schema must list the step names, but was " + view.schema());
        assertTrue(itemProperties.has("total"), () -> "no 'total' step in " + itemProperties);
    }
}
