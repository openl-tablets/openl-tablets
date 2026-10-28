package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;

/**
 * The JSON schema of a generated datatype bean carries the description of a field on the property, never on the
 * elements of an array field.
 */
class FieldDescriptionSchemaTest {

    @Test
    void describesTheFieldOnly() throws Exception {
        var generator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(new ObjectMapper());

        var properties = generator.generateSchema(describedBean()).get("properties");

        assertEquals("The name", properties.get("name").get("description").asText());
        assertEquals("The names", properties.get("names").get("description").asText());
        assertNull(properties.get("names").get("items").get("description"));
    }

    /** A datatype bean with a described field and a described array field, as OpenL generates it. */
    private static Class<?> describedBean() throws Exception {
        var byteCode = new JavaBeanClassBuilder("org.openl.generated.beans.DescribedBean")
                .addFields(Map.of("name", describedField(String.class, "The name"),
                        "names", describedField(String[].class, "The names")))
                .byteCode();
        var classLoader = new OpenLClassLoader(Thread.currentThread().getContextClassLoader());
        return ClassLoaderUtils.defineClass("org.openl.generated.beans.DescribedBean", byteCode, classLoader);
    }

    private static FieldDescription describedField(Class<?> type, String description) {
        return new FieldDescription(type.getName(), null, null, null, null, description, null, null, false, false);
    }
}
