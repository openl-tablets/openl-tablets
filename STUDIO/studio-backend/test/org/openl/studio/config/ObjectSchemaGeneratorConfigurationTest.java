package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import jakarta.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.calc.SpreadsheetResultBeanClass;

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

    @Getter
    @Setter
    public static class Price {
        private BigDecimal amount;
        private BigDecimal[] tiers;
        @XmlElement(nillable = true)
        private BigDecimal discount;
        private Map<String, BigDecimal> rates;
        private Double ratio;
        private double precise;
    }

    @SpreadsheetResultBeanClass
    public static class Totals {
        public BigDecimal getTotal() {
            return null;
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

    @ParameterizedTest
    @MethodSource("generators")
    void a_decimal_is_a_number_of_the_format_decimal(
            BiFunction<ObjectSchemaGeneratorConfiguration, ObjectMapper, SchemaGenerator> generator) {
        var schema = generator.apply(configuration, objectMapper).generateSchema(BigDecimal.class);

        assertEquals("number", schema.get("type").asText());
        assertEquals("decimal", schema.path("format").asText());
    }

    @ParameterizedTest
    @MethodSource("generators")
    void a_decimal_says_it_wherever_it_is_met(
            BiFunction<ObjectSchemaGeneratorConfiguration, ObjectMapper, SchemaGenerator> generator) {
        var properties = generator.apply(configuration, objectMapper).generateSchema(Price.class).get("properties");

        assertEquals("decimal", properties.at("/amount/format").asText());
        assertEquals("decimal", properties.at("/tiers/items/format").asText());
        assertEquals("decimal", properties.at("/rates/additionalProperties/format").asText());
        assertFalse(properties.get("ratio").has("format"));
        assertFalse(properties.get("precise").has("format"));
    }

    @Test
    void a_decimal_that_accepts_null_keeps_the_format() throws Exception {
        var discount = configuration.inputSchemaGenerator(objectMapper).generateSchema(Price.class)
                .at("/properties/discount");

        assertEquals(objectMapper.readTree("[\"number\", \"null\"]"), discount.get("type"));
        assertEquals("decimal", discount.path("format").asText());
    }

    @ParameterizedTest
    @MethodSource("generators")
    void a_decimal_of_a_spreadsheet_result_bean_says_it_too(
            BiFunction<ObjectSchemaGeneratorConfiguration, ObjectMapper, SchemaGenerator> generator) {
        var schema = generator.apply(configuration, objectMapper).generateSchema(Totals.class);

        assertEquals("decimal", schema.at("/properties/total/format").asText());
    }
}
