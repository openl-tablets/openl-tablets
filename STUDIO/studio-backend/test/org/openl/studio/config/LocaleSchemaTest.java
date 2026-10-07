package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.context.DefaultRulesRuntimeContext;

/**
 * The locale of the runtime context is described as text, the way it is written in JSON, rather than as an object.
 */
class LocaleSchemaTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static SchemaGenerator[] generators() {
        var configuration = new ObjectSchemaGeneratorConfiguration();
        return new SchemaGenerator[]{
                configuration.schemaGenerator(OBJECT_MAPPER),
                configuration.inputSchemaGenerator(OBJECT_MAPPER)
        };
    }

    @ParameterizedTest
    @MethodSource("generators")
    void describesTheLocaleOfTheRuntimeContextAsText(SchemaGenerator generator) {
        var schema = generator.generateSchema(DefaultRulesRuntimeContext.class);

        assertEquals(OBJECT_MAPPER.createObjectNode().put("type", "string"), schema.get("properties").get("locale"));
    }
}
