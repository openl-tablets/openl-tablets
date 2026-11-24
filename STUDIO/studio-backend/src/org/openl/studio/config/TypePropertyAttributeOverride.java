package org.openl.studio.config;

import java.lang.reflect.Modifier;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Describes the type property a value of a type is read by.
 *
 * <p>The object mapper of a project writes the type of a datatype that has a parent or children into a property of
 * the value, {@code @class} unless the project names another one, and reads it back from there. A value without it
 * cannot be read. The schema of such a type requires the property and gives it the one value the type is written
 * with, as its {@code const}. A datatype with no fields of its own gets that property too. A form built from the
 * schema can then create a nested object the run reads.
 *
 * <p>A type the mapper writes without a type property keeps its schema as it is. So does an interface and an
 * abstract class: no value is of that type itself. A map, a list and a plain value keep theirs as well. So does a
 * type whose way of being written cannot be found: the rest of its schema is still described.
 */
@Slf4j
@RequiredArgsConstructor
class TypePropertyAttributeOverride implements TypeAttributeOverrideV2 {

    private final ObjectMapper objectMapper;

    @Override
    public void overrideTypeAttributes(ObjectNode node, TypeScope scope, SchemaGenerationContext context) {
        if (!holdsFields(node, context)) {
            return;
        }
        var clazz = scope.getType().getErasedType();
        var typeSerializer = typeSerializerOf(clazz);
        if (typeSerializer == null || typeSerializer.getTypeInclusion() != JsonTypeInfo.As.PROPERTY) {
            return;
        }
        var property = typeSerializer.getPropertyName();
        node.withObjectProperty(context.getKeyword(SchemaKeyword.TAG_PROPERTIES))
                .putObject(property)
                .put(context.getKeyword(SchemaKeyword.TAG_CONST),
                        typeSerializer.getTypeIdResolver().idFromValueAndType(null, clazz));
        node.withArrayProperty(context.getKeyword(SchemaKeyword.TAG_REQUIRED)).add(property);
    }

    /**
     * Whether the schema describes an object of named fields, one with no fields included. A map, a list and a plain
     * value are written otherwise, and take no type property beside their values.
     */
    private static boolean holdsFields(ObjectNode node, SchemaGenerationContext context) {
        var isObject = context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT)
                .equals(node.path(context.getKeyword(SchemaKeyword.TAG_TYPE)).asText());
        var isMap = node.has(context.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES));
        return node.has(context.getKeyword(SchemaKeyword.TAG_PROPERTIES)) || isObject && !isMap;
    }

    private @Nullable TypeSerializer typeSerializerOf(Class<?> clazz) {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return null;
        }
        try {
            return objectMapper.getSerializerProviderInstance().findTypeSerializer(objectMapper.constructType(clazz));
        } catch (JsonMappingException | RuntimeException | LinkageError e) {
            log.debug("The type property of '{}' is not described.", clazz.getName(), e);
            return null;
        }
    }
}
