package org.openl.rules.spring.openapi.converter;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import org.openl.rules.spring.openapi.service.OpenApiPropertyResolver;
import org.openl.util.StringUtils;

/**
 * Schema customizer. The purpose of this class is to support {@link Deprecated}, {@link Parameter} annotations when
 * they are defined on class properties. Original v3 implementation doesn't support this case. Also, it's used for
 * schema description localization.
 *
 * @author Vladyslav Pikus
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PropertySchemaCustomizingConverter implements ModelConverter {

    private final OpenApiPropertyResolver apiPropertyResolver;
    private final ObjectMapper objectMapper;

    public PropertySchemaCustomizingConverter(OpenApiPropertyResolver apiPropertyResolver) {
        this.apiPropertyResolver = apiPropertyResolver;
        this.objectMapper = ObjectMapperFactory.createJson();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            var resolvedSchema = chain.next().resolve(type, context, chain);
            if (resolvedSchema == null) {
                return null;
            }
            if (resolvedSchema.get$ref() != null || (resolvedSchema.getName() != null && "object".equals(resolvedSchema.getType()))) {
                JavaType javaType;
                if (type.getType() instanceof JavaType) {
                    javaType = (JavaType) type.getType();
                } else {
                    javaType = objectMapper.constructType(type.getType());
                }
                var definition = objectMapper.getSerializationConfig().introspect(javaType);
                BeanDescription deserializationBeanDesc = null;
                Schema definedSchema;
                if (resolvedSchema.get$ref() != null ) {
                    definedSchema = context.getDefinedModels().get(resolvedSchema.get$ref().substring(21));
                } else {
                    definedSchema = resolvedSchema;
                }
                for (final var originalProperty : definition.findProperties()) {
                    var propSchema = findProperty(definedSchema, originalProperty);
                    if (propSchema == null) {
                        continue;
                    }
                    var property = originalProperty;
                    if (originalProperty.getPrimaryMember() == null) {
                        if (deserializationBeanDesc == null) {
                            deserializationBeanDesc = objectMapper.getDeserializationConfig().introspect(javaType);
                        }
                        property = deserializationBeanDesc.findProperties().stream()
                                .filter(p -> p.getName().equals(originalProperty.getName()))
                                .findFirst()
                                .orElse(originalProperty);
                    }
                    var deprecated = findAnnotation(property, Deprecated.class);
                    if (deprecated != null) {
                        propSchema.setDeprecated(Boolean.TRUE);
                    }
                    var paramApi = findAnnotation(property, Parameter.class);
                    if (paramApi != null) {
                        if (StringUtils.isNotBlank(paramApi.description())) {
                            propSchema.setDescription(apiPropertyResolver.resolve(paramApi.description()));
                        }
                        if (StringUtils.isNotBlank(paramApi.example())) {
                            propSchema.setExample(paramApi.example());
                        }
                        var schemaApi = paramApi.schema();
                        if (schemaApi != null && schemaApi.allowableValues().length > 0) {
                            propSchema.setEnum(Arrays.asList(schemaApi.allowableValues()));
                        }
                        if (paramApi.required()) {
                            if (!CollectionUtils.containsInstance(definedSchema.getRequired(), property.getName())) {
                                definedSchema.addRequiredItem(property.getName());
                            }
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(resolvedSchema.getDescription())) {
                resolvedSchema.setDescription(apiPropertyResolver.resolve(resolvedSchema.getDescription()));
            }
            return resolvedSchema;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Schema findProperty(Schema definedSchema, BeanPropertyDefinition propertyDefinition) {
        Schema propSchema = null;
        if (definedSchema.getProperties() != null) {
            propSchema = (Schema) definedSchema.getProperties().get(propertyDefinition.getName());
        }
        if (propSchema == null && definedSchema.getAllOf() != null) {
            for (var allOf : definedSchema.getAllOf()) {
                propSchema = findProperty((Schema) allOf, propertyDefinition);
                if (propSchema != null) {
                    break;
                }
            }
        }
        return propSchema;
    }

    private  <A extends Annotation> A findAnnotation(BeanPropertyDefinition propertyDefinition, Class<A> annotation) {
        return Stream.of(propertyDefinition.getField(), propertyDefinition.getGetter(), propertyDefinition.getSetter())
                .filter(Objects::nonNull)
                .map(member -> member.getAnnotation(annotation))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
