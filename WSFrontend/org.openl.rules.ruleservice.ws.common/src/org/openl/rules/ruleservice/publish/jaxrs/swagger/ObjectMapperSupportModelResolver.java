package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import static io.swagger.v3.core.util.RefUtils.constructRef;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

public class ObjectMapperSupportModelResolver extends ModelResolver {
    public ObjectMapperSupportModelResolver(ObjectMapper mapper) {
        super(mapper);
    }

    @SuppressWarnings("rawtypes")
    protected void resolveDiscriminatorProperty(JavaType type, ModelConverterContext context, Schema model) {
        final BeanDescription beanDesc = _mapper.getSerializationConfig().introspect(type);
        JsonTypeInfo typeInfo = beanDesc.getClassAnnotations().get(JsonTypeInfo.class);
        if (typeInfo != null) {
            if (beanDesc.getBeanClass().getSuperclass() != null) {
                final BeanDescription superBeanDesc = _mapper.getSerializationConfig()
                        .introspect(TypeFactory.defaultInstance().constructType(beanDesc.getBeanClass().getSuperclass()));
                JsonTypeInfo superJsonTypeInfo = superBeanDesc.getClassInfo().getAnnotation(JsonTypeInfo.class);
                JsonSubTypes jsonSubTypes = superBeanDesc.getClassInfo().getAnnotation(JsonSubTypes.class);
                if (jsonSubTypes != null) {
                    for (JsonSubTypes.Type subType : jsonSubTypes.value()) {
                        if (subType.value() == type.getRawClass()) {
                            if (Objects.equals(superJsonTypeInfo.property(), typeInfo.property())) {
                                return;
                            }
                            break;
                        }
                    }
                }
            }
            String typeInfoProp = typeInfo.property();
            if (StringUtils.isNotBlank(typeInfoProp)) {
                Schema modelToUpdate = model;
                if (StringUtils.isNotBlank(model.get$ref())) {
                    modelToUpdate = context.getDefinedModels().get(model.get$ref().substring(21));
                }
                if (modelToUpdate.getProperties() == null || !modelToUpdate.getProperties().containsKey(typeInfoProp)) {
                    Schema discriminatorSchema = new StringSchema().name(typeInfoProp);
                    modelToUpdate.addProperties(typeInfoProp, discriminatorSchema);
                    if (modelToUpdate.getRequired() == null || !modelToUpdate.getRequired().contains(typeInfoProp)) {
                        modelToUpdate.addRequiredItem(typeInfoProp);
                    }
                }
            }
        }
    }

    protected Discriminator resolveDiscriminator(JavaType type, ModelConverterContext context) {

        io.swagger.v3.oas.annotations.media.Schema declaredSchemaAnnotation = AnnotationsUtils
                .getSchemaDeclaredAnnotation(type.getRawClass());

        String disc = (declaredSchemaAnnotation == null) ? "" : declaredSchemaAnnotation.discriminatorProperty();
        boolean avoidDisc = false;
        if (disc.isEmpty()) {
            final BeanDescription beanDesc = _mapper.getSerializationConfig().introspect(type);
            Annotated a = beanDesc.getClassInfo();
            // longer method would involve AnnotationIntrospector.findTypeResolver(...) but:
            JsonTypeInfo typeInfo = a.getAnnotation(JsonTypeInfo.class);
            if (typeInfo != null) {
                disc = typeInfo.property();
            }

            if (StringUtils.isNotBlank(disc) && beanDesc.getBeanClass().getSuperclass() != null) {
                final BeanDescription superBeanDesc = _mapper.getSerializationConfig()
                        .introspect(TypeFactory.defaultInstance().constructType(beanDesc.getBeanClass().getSuperclass()));
                JsonTypeInfo superJsonTypeInfo = superBeanDesc.getClassInfo().getAnnotation(JsonTypeInfo.class);
                JsonSubTypes jsonSubTypes = superBeanDesc.getClassInfo().getAnnotation(JsonSubTypes.class);
                if (jsonSubTypes != null) {
                    for (JsonSubTypes.Type subType : jsonSubTypes.value()) {
                        if (subType.value() == type.getRawClass()) {
                            avoidDisc = true;
                            break;
                        }
                    }
                }
                avoidDisc = avoidDisc && superJsonTypeInfo != null && Objects.equals(superJsonTypeInfo.property(),
                        disc);
            }
        }
        if (StringUtils.isNotBlank(disc) && !avoidDisc) {
            Discriminator discriminator = new Discriminator().propertyName(disc);
            if (declaredSchemaAnnotation != null) {
                DiscriminatorMapping[] mappings = declaredSchemaAnnotation.discriminatorMapping();
                if (mappings.length > 0) {
                    for (DiscriminatorMapping mapping : mappings) {
                        if (!mapping.value().isEmpty() && !mapping.schema().equals(Void.class)) {
                            discriminator.mapping(mapping.value(),
                                    constructRef(context.resolve(new AnnotatedType().type(mapping.schema())).getName()));
                        }
                    }
                }
            }
            return discriminator;
        }
        return null;
    }

}
