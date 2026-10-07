package org.openl.studio.projects.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import org.openl.domain.EnumDomain;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

class ExecutionValueMapperTest {

    /** A value is known by the key a test case refers to it by, so values of one type are told apart unread. */
    @Test
    void aValueIsKnownByTheKeyItIsReferredToBy() {
        var driverClass = JavaOpenClass.getOpenClass(Driver.class);
        var driver = new Driver();
        driver.setName("Sara");
        driver.setLicense(new License());

        var byName = new ParameterWithValueDeclaration("driver", driver, driverClass, driverClass.getField("name"));
        assertEquals("Sara", ExecutionValueMapper.keyOf(byName));
        // A key that is itself a structure names nothing readable, so the value reads by its type alone.
        var byLicense = new ParameterWithValueDeclaration("driver", driver, driverClass, driverClass.getField("license"));
        assertNull(ExecutionValueMapper.keyOf(byLicense));
        // A value no test case refers to by a key has none, whatever fields its type declares first.
        assertNull(ExecutionValueMapper.keyOf(new ParameterWithValueDeclaration("driver", driver, driverClass)));
        // A value that is not there has no key to be found by.
        var absent = new ParameterWithValueDeclaration("driver", null, driverClass, driverClass.getField("name"));
        assertNull(ExecutionValueMapper.keyOf(absent));
    }

    /**
     * A value the object mapper writes plain is written as it stands, whether or not OpenL counts its type as simple.
     * A value that opens into lines is only referred to.
     */
    @Test
    void refersOnlyToAValueThatOpensIntoLines() {
        var objectMapper = new ObjectMapper();
        var mapper = new ExecutionValueMapper(objectMapper,
                new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper), null);
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var driver = new Driver();
        driver.setName("Sara");

        var plain = mapper.writeParameterLazily(new ParameterWithValueDeclaration("id", id), "Id");
        var structured = mapper.writeParameterLazily(new ParameterWithValueDeclaration("driver", driver), "Driver");

        assertEquals(Boolean.FALSE, plain.lazy());
        assertEquals(id.toString(), plain.value().asText());
        assertEquals(Boolean.TRUE, structured.lazy());
        assertNull(structured.value(), "the value is read when it is asked for");
    }

    /** A vocabulary parameter is described by the values it allows, and an array of one by the values of its elements. */
    @Test
    void describesAVocabularyByItsValues() {
        var objectMapper = new ObjectMapper();
        var mapper = new ExecutionValueMapper(objectMapper,
                new ObjectSchemaGeneratorConfiguration().inputSchemaGenerator(objectMapper), null);
        var kind = new DomainOpenClass("Kind", JavaOpenClass.STRING, new EnumDomain<>(new String[]{"bla1", "bla2"}), null, null);
        var level = new DomainOpenClass("Level", JavaOpenClass.INT, new EnumDomain<>(new Integer[]{1, 2}), null, null);

        var scalar = mapper.describeParameter("kind", kind, null).schema();
        assertEquals("string", scalar.get("type").asText());
        assertEquals(List.of("bla1", "bla2"), objectMapper.convertValue(scalar.get("enum"), List.class));

        var array = mapper.describeParameter("kinds", kind.getArrayType(1), null).schema();
        assertEquals("array", array.get("type").asText());
        assertFalse(array.has("enum"));
        assertEquals(List.of("bla1", "bla2"), objectMapper.convertValue(array.get("items").get("enum"), List.class));

        var matrix = mapper.describeParameter("kinds", kind.getArrayType(2), null).schema();
        assertEquals(List.of("bla1", "bla2"), objectMapper.convertValue(matrix.get("items").get("items").get("enum"), List.class));

        var numbers = mapper.describeParameter("level", level, null).schema();
        assertEquals("integer", numbers.get("type").asText());
        assertEquals(List.of(1, 2), objectMapper.convertValue(numbers.get("enum"), List.class));
    }

    public static class License {
    }

    @Getter
    @Setter
    public static class Driver {
        private String name;
        private License license;
    }
}
