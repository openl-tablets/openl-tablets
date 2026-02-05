package org.openl.studio.config;

import java.lang.reflect.Type;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import org.openl.rules.calc.SpreadsheetResultBeanClass;

/**
 * Custom schema definition provider that uses Jackson's BeanDescription to discover properties
 * for classes annotated with {@link SpreadsheetResultBeanClass}.
 * <p>
 * This is needed because these classes have getter methods without backing fields,
 * and the standard victools PLAIN_JSON preset only considers fields.
 * <p>
 * This approach mirrors how Swagger's ModelResolver uses Jackson introspection.
 */
public class JacksonBeanSchemaProvider implements CustomDefinitionProviderV2 {

    private final ObjectMapper objectMapper;

    public JacksonBeanSchemaProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
        Class<?> erasedType = javaType.getErasedType();

        // Only handle classes marked as SpreadsheetResult beans
        if (!erasedType.isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
            return null;
        }

        // Use Jackson's introspection to discover properties
        JavaType jacksonType = objectMapper.constructType(erasedType);
        BeanDescription beanDescription = objectMapper.getSerializationConfig().introspect(jacksonType);

        var properties = beanDescription.findProperties();
        if (properties.isEmpty()) {
            return null;
        }

        // Build the schema
        ObjectNode schema = context.getGeneratorConfig().createObjectNode();
        schema.put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));

        ObjectNode propertiesNode = schema.putObject(context.getKeyword(SchemaKeyword.TAG_PROPERTIES));

        for (BeanPropertyDefinition property : properties) {
            String propertyName = property.getName();
            JavaType propertyType = property.getPrimaryType();

            if (propertyType != null) {
                // Recursively generate schema for property type using victools
                Type rawType = propertyType.getRawClass();
                ObjectNode propertySchema = context.createDefinitionReference(
                        context.getTypeContext().resolve(rawType));
                propertiesNode.set(propertyName, propertySchema);
            }
        }

        return new CustomDefinition(schema);
    }
}
