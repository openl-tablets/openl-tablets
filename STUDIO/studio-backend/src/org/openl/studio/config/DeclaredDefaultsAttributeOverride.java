package org.openl.studio.config;

import java.lang.reflect.Modifier;
import jakarta.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanClass;

/**
 * Writes the defaults a datatype declares into its JSON schema.
 *
 * <p>Every field that starts with a value gets that value as the {@code default} of its property. A form built
 * from the schema can then create a nested object the way the datatype declares it.
 *
 * <p>Only a bean generated for an OpenL datatype is described this way. A spreadsheet result and any other Java
 * type keep their schema as it is. A datatype bean that cannot be created keeps its schema without defaults.
 */
@Slf4j
@RequiredArgsConstructor
public class DeclaredDefaultsAttributeOverride implements TypeAttributeOverrideV2 {

    private final ObjectMapper objectMapper;

    @Override
    public void overrideTypeAttributes(ObjectNode node, TypeScope scope, SchemaGenerationContext context) {
        if (!(node.get(context.getKeyword(SchemaKeyword.TAG_PROPERTIES)) instanceof ObjectNode properties)) {
            return;
        }
        var clazz = scope.getType().getErasedType();
        if (!canCreate(clazz)) {
            return;
        }
        var values = defaultsOf(clazz);
        if (values == null) {
            return;
        }
        var defaultKeyword = context.getKeyword(SchemaKeyword.TAG_DEFAULT);
        properties.fields().forEachRemaining(property -> {
            var value = values.get(property.getKey());
            if (value != null && !value.isNull() && property.getValue() instanceof ObjectNode propertySchema) {
                propertySchema.set(defaultKeyword, value);
            }
        });
    }

    private static boolean canCreate(Class<?> clazz) {
        return clazz.isAnnotationPresent(XmlType.class)
                && !clazz.isAnnotationPresent(SpreadsheetResultBeanClass.class)
                && !SpreadsheetResult.class.isAssignableFrom(clazz)
                && !clazz.isEnum()
                && !clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers());
    }

    private JsonNode defaultsOf(Class<?> clazz) {
        try {
            return objectMapper.valueToTree(clazz.getConstructor().newInstance());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            log.debug("The defaults of '{}' are not described: it cannot be created.", clazz.getName(), e);
            return null;
        }
    }
}
