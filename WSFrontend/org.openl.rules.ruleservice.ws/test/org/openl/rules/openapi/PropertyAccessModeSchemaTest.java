package org.openl.rules.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import io.swagger.v3.oas.models.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;

/**
 * The access mode of a bean property is the one swagger-core gives it.
 *
 * <p>The properties of a bean are read once, not once per property: a bean of hundreds of properties took seconds.
 */
class PropertyAccessModeSchemaTest {

    @SuppressWarnings("unused")
    static class Bean {
        @Getter
        @Setter
        private String readWrite;
        @Setter
        private String setterOnly;
        @Getter
        private String readable;
        @Getter
        @Setter
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private String readOnly;
        @Getter
        @Setter
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String writeOnly;
        @Getter
        @Setter
        @JsonProperty(access = JsonProperty.Access.READ_WRITE)
        private String accessReadWrite;
        @Getter
        @Setter
        @io.swagger.v3.oas.annotations.media.Schema(accessMode = AccessMode.READ_ONLY)
        private String annotated;
    }

    @Getter
    @Setter
    @SuppressWarnings("unused")
    static class Plain {
        private String first;
        private Integer second;
        private Double third;
    }

    /**
     * Counts the beans read for deserialization, by their classes.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class CountingIntrospector extends BasicClassIntrospector {
        private final Map<Class<?>, Integer> counts;

        @Override
        public BasicClassIntrospector copy() {
            return new CountingIntrospector(counts);
        }

        @Override
        public BasicBeanDescription forDeserialization(DeserializationConfig config,
                                                       JavaType type,
                                                       ClassIntrospector.MixInResolver r) {
            counts.merge(type.getRawClass(), 1, Integer::sum);
            return super.forDeserialization(config, type, r);
        }
    }

    private static Schema<?> resolve(ModelConverterContextImpl context) {
        return resolve(context, Bean.class);
    }

    private static Schema<?> resolve(ModelConverterContextImpl context, Class<?> type) {
        context.resolve(new AnnotatedType(type));
        return context.getDefinedModels().get(type.getSimpleName());
    }

    @Test
    void accessModesAreTheOnesOfSwaggerCore() {
        var expected = resolve(new ModelConverterContextImpl(new ModelResolver(new ObjectMapper())));
        var actual = resolve(new ModelConverterContextImpl(OpenAPIConfiguration.getConverters(new ObjectMapper())));

        assertEquals(expected.getProperties().keySet(), actual.getProperties().keySet());
        expected.getProperties().forEach((name, property) -> {
            assertEquals(property.getReadOnly(), actual.getProperties().get(name).getReadOnly(), name);
            assertEquals(property.getWriteOnly(), actual.getProperties().get(name).getWriteOnly(), name);
        });
    }

    @Test
    void accessModesOfTheProperties() {
        var properties = resolve(new ModelConverterContextImpl(
                OpenAPIConfiguration.getConverters(new ObjectMapper()))).getProperties();

        assertTrue(properties.get("setterOnly").getWriteOnly());
        assertTrue(properties.get("readOnly").getReadOnly());
        assertTrue(properties.get("annotated").getReadOnly());
        assertNull(properties.get("readWrite").getReadOnly());
        assertNull(properties.get("readWrite").getWriteOnly());
        assertNull(properties.get("accessReadWrite").getReadOnly());
        assertNull(properties.get("accessReadWrite").getWriteOnly());
    }

    @Test
    void aBeanIsReadOnceForAllItsProperties() {
        var counts = new HashMap<Class<?>, Integer>();
        var mapper = new ObjectMapper();
        mapper.setConfig(mapper.getDeserializationConfig().with(new CountingIntrospector(counts)));

        var plain = resolve(new ModelConverterContextImpl(OpenAPIConfiguration.getConverters(mapper)), Plain.class);

        assertEquals(3, plain.getProperties().size());
        assertEquals(1, counts.get(Plain.class));
    }
}
