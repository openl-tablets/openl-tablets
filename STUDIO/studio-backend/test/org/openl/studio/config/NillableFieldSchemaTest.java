package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;

/**
 * A datatype field that declares a default accepts null in the input schema, unless it is a primitive. A missing
 * field takes the default, so null is how a caller asks for no value there.
 */
class NillableFieldSchemaTest {

    private static final String BEAN = "org.openl.generated.beans.NillableTerms";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsNullOnlyWhereItReplacesADefault() throws Exception {
        var generator = new ObjectSchemaGeneratorConfiguration().inputSchemaGenerator(objectMapper);

        var properties = generator.generateSchema(datatypeBean()).get("properties");

        assertEquals(objectMapper.readTree("[\"string\", \"null\"]"), properties.get("grade").get("type"));
        assertEquals("A", properties.get("grade").get("default").asText());
        assertEquals("integer", properties.get("age").get("type").asText(), "a primitive cannot be null");
        assertEquals(30, properties.get("age").get("default").asInt());
        assertEquals("string", properties.get("note").get("type").asText(), "a missing field is null already");
    }

    /** A datatype as OpenL generates it: a text with a default, a primitive with a default and a text without one. */
    private static Class<?> datatypeBean() throws Exception {
        var byteCode = new JavaBeanClassBuilder(BEAN)
                .addFields(Map.of(
                        "grade", new FieldDescription(String.class.getName(), "A", "A", null, false),
                        "age", new FieldDescription(int.class.getName(), 30, "30", null, false),
                        "note", new FieldDescription(String.class.getName())))
                .byteCode();
        var classLoader = new OpenLClassLoader(Thread.currentThread().getContextClassLoader());
        return ClassLoaderUtils.defineClass(BEAN, byteCode, classLoader);
    }
}
