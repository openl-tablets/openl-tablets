package org.openl.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.gen.ByteCodeAnnotations.list;
import static org.openl.gen.ByteCodeAnnotations.map;
import static org.openl.gen.ByteCodeAnnotations.of;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.datatype.gen.JavaBeanClassBuilder;

/**
 * The OpenAPI annotations a generated bean carries for a field: the description and the example in a
 * {@code @Parameter} of the field, the values of a vocabulary type on the field itself for a scalar, on the
 * elements for an array, and none for an array of arrays.
 */
class OpenApiSchemaAnnotationsTest {

    private static final String PARAMETER = "Lio/swagger/v3/oas/annotations/Parameter;";
    private static final String SCHEMA = "Lio/swagger/v3/oas/annotations/media/Schema;";
    private static final String ARRAY_SCHEMA = "Lio/swagger/v3/oas/annotations/media/ArraySchema;";
    private static final String[] VALUES = {"bla1", "bla2", "bla3"};

    private static Map<String, Map<String, Map<String, Object>>> beanWith(String type,
                                                                          String description,
                                                                          String[] values) {
        return beanWith(type, description, values, null);
    }

    private static Map<String, Map<String, Map<String, Object>>> beanWith(String type,
                                                                          String description,
                                                                          String[] values,
                                                                          String example) {
        var field = new FieldDescription(type, null, null, null, null, description, values, example, false, false);
        var byteCode = new JavaBeanClassBuilder("org.openl.generated.Vocabulary").addField("kind", field).byteCode();
        return ByteCodeAnnotations.read(byteCode);
    }

    @Test
    void aScalarFieldCarriesTheValuesInItsSchema() {
        var members = beanWith(String.class.getName(), "The kind", VALUES);

        var schema = of(members, "kind", SCHEMA);
        assertEquals(List.of(VALUES), list(schema.get("allowableValues")));
        assertNull(schema.get("description"));
        assertEquals("The kind", of(members, "kind", PARAMETER).get("description"));
        assertNull(of(members, "kind", ARRAY_SCHEMA));
    }

    @Test
    void anArrayFieldCarriesTheValuesOnItsElements() {
        var members = beanWith(String[].class.getName(), "The kinds", VALUES);

        // A schema on the array itself would restrict the array to one of the words.
        assertNull(of(members, "kind", SCHEMA));
        var arraySchema = of(members, "kind", ARRAY_SCHEMA);
        assertNull(arraySchema.get("arraySchema"));
        assertEquals(List.of(VALUES), list(map(arraySchema.get("schema")).get("allowableValues")));
        assertEquals("The kinds", of(members, "kind", PARAMETER).get("description"));
    }

    @Test
    void anArrayFieldWithoutADescriptionDescribesTheElementsOnly() {
        var members = beanWith(String[].class.getName(), null, VALUES);

        var arraySchema = of(members, "kind", ARRAY_SCHEMA);
        assertNotNull(arraySchema);
        assertEquals(List.of(VALUES), list(map(arraySchema.get("schema")).get("allowableValues")));
        assertNull(of(members, "kind", PARAMETER));
    }

    @Test
    void anArrayOfArraysKeepsTheErasedElementSchema() {
        var members = beanWith(String[][].class.getName(), "The grid", VALUES);

        assertNull(of(members, "kind", ARRAY_SCHEMA));
        assertNull(of(members, "kind", SCHEMA));
        assertEquals("The grid", of(members, "kind", PARAMETER).get("description"));
    }

    @Test
    void aFieldOfAnotherTypeIsDescribedByItsParameterOnly() {
        // A schema would describe the referenced type too.
        var members = beanWith("org.openl.generated.Other", "The other", null, "{}");

        var parameter = of(members, "kind", PARAMETER);
        assertEquals("The other", parameter.get("description"));
        assertEquals("{}", parameter.get("example"));
        assertNull(of(members, "kind", SCHEMA));
        assertNull(of(members, "kind", ARRAY_SCHEMA));
    }

    @Test
    void aFieldWithNothingToSayCarriesNoSchema() {
        var members = beanWith(String[].class.getName(), null, null);

        assertNull(of(members, "kind", PARAMETER));
        assertNull(of(members, "kind", SCHEMA));
        assertNull(of(members, "kind", ARRAY_SCHEMA));
    }
}
