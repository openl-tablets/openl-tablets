package org.openl.rules.spring.openapi.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

class PropertySchemaCustomizingConverterTest {

    @Test
    void removesInferredObjectTypeFromPrimitiveArrayItems() {
        var schema = resolveArraySchema(PrimitiveArray.class);
        var items = schema.getItems();

        assertEquals(1, schema.getMinItems());
        assertNull(items.getType(), "a primitive union must not also require an object");
        assertEquals(List.of("string", "number", "boolean"),
                items.getOneOf().stream().map(io.swagger.v3.oas.models.media.Schema::getType).toList());
    }

    @Test
    void representsNullAsTypedNullableUnionAlternative() {
        var converters = new ModelConverters();
        converters.addConverter(new PropertySchemaCustomizingConverter(value -> value));
        var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(NullablePrimitiveArray.class));
        var array = assertInstanceOf(io.swagger.v3.oas.models.media.ArraySchema.class, resolved.schema);
        var items = array.getItems();

        assertNull(items.getType(), "the item union must not also require an object");
        var nullableAlternative = items.getOneOf().getFirst();
        assertEquals("string", nullableAlternative.getType());
        assertTrue(nullableAlternative.getNullable());
    }

    @Test
    void preservesInferredObjectTypeForObjectArrayItems() {
        var items = resolveArraySchema(ObjectArray.class).getItems();

        assertEquals("object", items.getType());
    }

    @Test
    void removesInferredObjectTypeFromValueUnion() {
        var schema = resolveSchema(ValueHolder.class);
        var properties = schema.getProperties();
        assertNotNull(properties);
        var value = properties.get("value");
        assertNotNull(value);

        assertNull(value.getType(), "a value union must not also require an object");
        assertEquals(List.of("string", "number", "boolean", "array"),
                value.getOneOf().stream()
                        .map(alternative -> ((io.swagger.v3.oas.models.media.Schema<?>) alternative).getType())
                        .toList());
        var nullableAlternative = (io.swagger.v3.oas.models.media.Schema<?>) value.getOneOf().getFirst();
        assertTrue(nullableAlternative.getNullable());
    }

    private static io.swagger.v3.oas.models.media.ArraySchema resolveArraySchema(Class<?> type) {
        return assertInstanceOf(io.swagger.v3.oas.models.media.ArraySchema.class, resolveSchema(type));
    }

    private static io.swagger.v3.oas.models.media.Schema<?> resolveSchema(Class<?> type) {
        var converters = new ModelConverters();
        converters.addConverter(new PropertySchemaCustomizingConverter(value -> value));
        var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(type));
        if (resolved.schema.get$ref() == null) {
            return resolved.schema;
        }
        var schema = resolved.referencedSchemas.get(resolved.schema.get$ref().substring(21));
        assertNotNull(schema);
        return schema;
    }

    @ArraySchema(minItems = 1, schema = @Schema(nullable = true, oneOf = {String.class, Number.class, Boolean.class}))
    private interface PrimitiveArray {
    }

    @ArraySchema(schema = @Schema(oneOf = ObjectValue.class))
    private interface ObjectArray {
    }

    @ArraySchema(schema = @Schema(oneOf = {NullableString.class, Number.class, Boolean.class}))
    private interface NullablePrimitiveArray {
    }

    @Schema(type = "string", nullable = true)
    private interface NullableString {
    }

    private static class ObjectValue {
    }

    private record ValueHolder(
            @Schema(oneOf = {NullableString.class, Number.class, Boolean.class, PrimitiveArray.class}) Object value) {
    }
}
