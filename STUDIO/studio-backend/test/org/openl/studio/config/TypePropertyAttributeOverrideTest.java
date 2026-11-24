package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.openl.rules.serialization.DefaultTypingMode;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;

/**
 * A datatype with a parent or children is read by its type property, so the input schema requires the property
 * with the one value the type is written with.
 */
class TypePropertyAttributeOverrideTest {

    public static class Base {
        public String code;
    }

    public static class Child extends Base {
        public int level;
    }

    public static class Holder {
        public Base base;
    }

    /** A parent datatype with no fields of its own. */
    public static class Marker {
    }

    public static class Tagged extends Marker {
        public String tag;
    }

    public static class Bag {
        public HashMap<String, String> codes;
        public Date since;
    }

    public abstract static class Shape {
        public String name;
    }

    public static class Circle extends Shape {
        public double radius;
    }

    @ParameterizedTest
    @EnumSource(value = JsonTypeInfo.Id.class, names = {"CLASS", "NAME"})
    void requiresTheTypeAsTheMapperWritesIt(JsonTypeInfo.Id typeId) throws Exception {
        var objectMapper = mapper(typeId, Base.class, Child.class);

        var schema = inputSchema(objectMapper, Base.class);

        assertEquals(typeOf(objectMapper, new Base()), schema.at("/properties/@class/const"));
        assertEquals(objectMapper.readTree("[\"@class\"]"), schema.get("required"));
        assertEquals("string", schema.at("/properties/code/type").asText(), "the fields stay as they are");
    }

    @Test
    void describesTheTypeOfANestedField() throws Exception {
        var objectMapper = mapper(JsonTypeInfo.Id.NAME, Base.class, Child.class);

        var holder = inputSchema(objectMapper, Holder.class);

        assertEquals(typeOf(objectMapper, new Base()), holder.at("/properties/base/properties/@class/const"));
        assertFalse(holder.get("properties").has("@class"), "a datatype without a parent or children has no type");
        assertNull(holder.get("required"));
    }

    @Test
    void describesTheTypeOfADatatypeWithNoFields() throws Exception {
        var objectMapper = mapper(JsonTypeInfo.Id.NAME, Marker.class, Tagged.class);

        var marker = inputSchema(objectMapper, Marker.class);

        assertEquals("object", marker.get("type").asText());
        assertEquals(typeOf(objectMapper, new Marker()), marker.at("/properties/@class/const"));
        assertEquals(objectMapper.readTree("[\"@class\"]"), marker.get("required"));
    }

    @Test
    void leavesAMapAndAPlainValueAsTheyAre() throws Exception {
        // Every type that is not final is written with its type here, a map and a date among them.
        var factory = new JacksonObjectMapperFactoryBean();
        factory.setDefaultTypingMode(DefaultTypingMode.NON_FINAL);
        var objectMapper = factory.createJacksonObjectMapper();

        var bag = inputSchema(objectMapper, Bag.class);

        assertEquals(typeOf(objectMapper, new Bag()), bag.at("/properties/@class/const"));
        assertFalse(bag.at("/properties/codes").has("properties"), "a map holds entries, not fields");
        assertEquals("string", bag.at("/properties/since/type").asText());
        assertFalse(bag.at("/properties/since").has("properties"));
    }

    @Test
    void describesTheRestOfATypeWhoseWayOfBeingWrittenCannotBeFound() throws Exception {
        var objectMapper = spy(mapper(JsonTypeInfo.Id.NAME, Base.class, Child.class));
        doThrow(new IllegalStateException("broken mix-in")).when(objectMapper).getSerializerProviderInstance();

        var schema = inputSchema(objectMapper, Base.class);

        assertEquals("string", schema.at("/properties/code/type").asText());
        assertFalse(schema.get("properties").has("@class"));
    }

    @Test
    void leavesAnAbstractTypeAsItIs() throws Exception {
        var objectMapper = mapper(JsonTypeInfo.Id.NAME, Shape.class, Circle.class);

        var shape = inputSchema(objectMapper, Shape.class);

        assertFalse(shape.get("properties").has("@class"), "no value is of an abstract type itself");
        assertEquals(typeOf(objectMapper, new Circle()),
                inputSchema(objectMapper, Circle.class).at("/properties/@class/const"));
    }

    @Test
    void leavesTheSchemaOfPublishedValuesAsItIs() throws Exception {
        var objectMapper = mapper(JsonTypeInfo.Id.NAME, Base.class, Child.class);

        var schema = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper).generateSchema(Base.class);

        assertFalse(schema.get("properties").has("@class"));
    }

    /** The object mapper of a project whose datatypes extend one another, the way OpenL Studio builds it. */
    private static ObjectMapper mapper(JsonTypeInfo.Id typeId, Class<?>... datatypes) throws Exception {
        var factory = new JacksonObjectMapperFactoryBean();
        factory.setJsonTypeInfoId(typeId);
        factory.setOverrideClasses(Set.of(datatypes));
        return factory.createJacksonObjectMapper();
    }

    /** The type property of a value, as the mapper writes it. */
    private static JsonNode typeOf(ObjectMapper objectMapper, Object value) {
        return objectMapper.valueToTree(value).get("@class");
    }

    private static JsonNode inputSchema(ObjectMapper objectMapper, Class<?> type) {
        return new ObjectSchemaGeneratorConfiguration().inputSchemaGenerator(objectMapper).generateSchema(type);
    }
}
