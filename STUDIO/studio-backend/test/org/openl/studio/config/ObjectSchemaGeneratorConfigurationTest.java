package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * What a map is described as — see {@link MapEntriesAttributeOverride} for why it matters.
 */
class ObjectSchemaGeneratorConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectSchemaGeneratorConfiguration configuration = new ObjectSchemaGeneratorConfiguration();

    /** Stands in for a bean holding a map. */
    public static class Basket {
        private Map<String, Integer> limits;

        public Map<String, Integer> getLimits() {
            return limits;
        }

        public void setLimits(Map<String, Integer> limits) {
            this.limits = limits;
        }
    }

    /** Both generators are built from one configuration, and describe a map the same way. */
    static Stream<BiFunction<ObjectSchemaGeneratorConfiguration, ObjectMapper, SchemaGenerator>> generators() {
        return Stream.of(
                ObjectSchemaGeneratorConfiguration::inputSchemaGenerator,
                ObjectSchemaGeneratorConfiguration::schemaGenerator);
    }

    @ParameterizedTest
    @MethodSource("generators")
    void a_map_says_what_its_entries_hold(
            BiFunction<ObjectSchemaGeneratorConfiguration, ObjectMapper, SchemaGenerator> generator) {
        var schema = generator.apply(configuration, objectMapper)
                .generateSchema(Map.class, String.class, Integer.class);

        assertEquals("integer", schema.at("/additionalProperties/type").asText());
    }

    @Test
    void a_map_a_datatype_declares_says_it_too() {
        var schema = configuration.inputSchemaGenerator(objectMapper).generateSchema(Basket.class);

        assertEquals("integer", schema.at("/properties/limits/additionalProperties/type").asText());
    }

    @Test
    void a_map_that_says_nothing_about_its_values_says_at_least_that_it_takes_them() {
        // What OpenL declares: a datatype has no types for the values of a map.
        var schema = configuration.inputSchemaGenerator(objectMapper).generateSchema(Map.class);

        assertEquals("true", schema.at("/additionalProperties").asText());
    }
}
