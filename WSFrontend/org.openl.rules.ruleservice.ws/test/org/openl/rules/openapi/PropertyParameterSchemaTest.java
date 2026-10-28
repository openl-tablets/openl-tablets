package org.openl.rules.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Getter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The {@code @Parameter} of a bean property describes the property alone: the elements of a list, an array or a
 * map and the schema of a type the property refers to keep their own description.
 */
class PropertyParameterSchemaTest {

    @SuppressWarnings("rawtypes")
    private static Map<String, Schema> schemas;

    @Getter
    @SuppressWarnings("unused")
    static class Item {
        private String name;
    }

    @Getter
    @SuppressWarnings("unused")
    static class Owner {
        @Parameter(description = "The item")
        private Item item;
        @Parameter(description = "The items")
        private List<Item> items;
        @Parameter(description = "The maps")
        private Map<String, Object>[] maps;
        @Parameter(description = "The count", example = "7")
        private Integer count;
        @Parameter(example = "Alice")
        private String name;
        @Parameter(example = "\"Bob\"")
        private String quoted;
        @Parameter(example = "1.5")
        private double[] single;
        @Parameter(example = "-11")
        private Byte code;
        @Parameter(example = "1,2")
        private byte[] bytes;
        @Parameter(example = "a,b")
        private List<String> tags;
        @Parameter(description = "Generated", example = "1")
        @io.swagger.v3.oas.annotations.media.Schema(description = "Own", example = "2")
        private Integer own;
    }

    @Getter
    @SuppressWarnings("unused")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    @JsonSubTypes(@JsonSubTypes.Type(Child.class))
    static class Parent {
        @Parameter(example = "1.5,2")
        private double[] values;
    }

    @Getter
    @SuppressWarnings("unused")
    static class Child extends Parent {
        private String extra;
    }

    @BeforeAll
    static void resolve() {
        var context = new ModelConverterContextImpl(OpenAPIConfiguration.getConverters(new ObjectMapper()));
        context.resolve(new AnnotatedType(Owner.class));
        context.resolve(new AnnotatedType(Parent.class));
        schemas = context.getDefinedModels();
    }

    private static Schema<?> property(String name) {
        return property(schemas.get("Owner"), name);
    }

    private static Schema<?> property(Schema<?> schema, String name) {
        return schema.getProperties().get(name);
    }

    @Test
    void aReferencedTypeKeepsItsOwnDescription() {
        assertEquals("#/components/schemas/Item", property("item").get$ref());
        assertNull(schemas.get("Item").getDescription());
        assertNull(property(schemas.get("Item"), "name").getDescription());
    }

    @Test
    void theElementsOfAListAreNotDescribedByTheProperty() {
        var items = property("items");

        assertEquals("The items", items.getDescription());
        assertEquals("#/components/schemas/Item", items.getItems().get$ref());
        assertNull(items.getItems().getDescription());
    }

    @Test
    void theElementsOfAnArrayOfMapsAreNotDescribedByTheProperty() {
        var maps = property("maps");

        assertEquals("The maps", maps.getDescription());
        assertNull(maps.getItems().getDescription());
        assertNull(((Schema<?>) maps.getItems().getAdditionalProperties()).getDescription());
    }

    @Test
    void anExampleIsReadAsJsonOrByThePropertyType() {
        var count = property("count");

        assertEquals("The count", count.getDescription());
        assertEquals(7, count.getExample());
        assertEquals("Alice", property("name").getExample());
    }

    @Test
    void aJsonStringIsReadWithoutQuotes() {
        assertEquals("Bob", property("quoted").getExample());
    }

    @Test
    void aSingleValueOfAnArrayIsAnArrayOfOneElement() {
        assertEquals(List.of(1.5), property("single").getExample());
    }

    @Test
    void aBinaryExampleIsWrittenInBase64() {
        assertEquals("LTEx", property("code").getExample());
        assertEquals("AQI=", property("bytes").getExample());
    }

    @Test
    void anExampleThatCannotBeReadIsLeftOut() {
        var tags = property("tags");

        assertNull(tags.getExample());
        assertFalse(tags.getExampleSetFlag());
    }

    @Test
    void theSchemaOfThePropertyWins() {
        var own = property("own");

        assertEquals("Own", own.getDescription());
        assertEquals(2, own.getExample());
    }

    @Test
    void aSubtypeDoesNotRepeatAnInheritedPropertyWithAnArrayExample() {
        assertEquals(List.of(1.5, 2.0), property(schemas.get("Parent"), "values").getExample());

        var own = (Schema<?>) schemas.get("Child").getAllOf().get(1);
        assertEquals(Set.of("extra"), own.getProperties().keySet());
    }
}
