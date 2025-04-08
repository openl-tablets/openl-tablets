package org.openl.rules.openapi;

import static io.swagger.v3.core.util.RefUtils.constructRef;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.commons.lang3.StringUtils;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.util.JAXBUtils;

class ObjectMapperSupportModelResolver extends ModelResolver {
    public ObjectMapperSupportModelResolver(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    protected Object resolveDefaultValue(Annotated a,
                                         Annotation[] annotations,
                                         io.swagger.v3.oas.annotations.media.Schema schema) {
        Object defaultValue = super.resolveDefaultValue(a, annotations, schema);
        if (defaultValue instanceof String) {
            Class<?> t = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(a.getRawType());
            Class<?> rawType = t == null ? a.getRawType() : t;
            try {
                IString2DataConvertor<?> convertor = String2DataConvertorFactory.getConvertor(rawType);
                if (convertor != null) {
                    return convertor.parse((String) defaultValue, null);
                }
            } catch (Exception ignore) {
                return null;
            }
        }
        return defaultValue;
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
                for (DiscriminatorMapping mapping : mappings) {
                    if (!mapping.value().isEmpty() && !mapping.schema().equals(Void.class)) {
                        discriminator.mapping(mapping.value(),
                                constructRef(context.resolve(new AnnotatedType().type(mapping.schema())).getName()));
                    }
                }
            }
            return discriminator;
        }
        return null;
    }

    @Override
    protected void applyBeanValidatorAnnotations(Schema property, Annotation[] annotations, Schema parent, boolean applyNotNullAnnotations) {
        super.applyBeanValidatorAnnotations(property, annotations, parent, applyNotNullAnnotations);
        String propertyName = property.getName();
        if (propertyName != null && (propertyName.startsWith("get") || propertyName.startsWith("is"))) {

            // datatype with incorrect field is generated if property name looks like a getter method
            property.setName(propertyName.substring(propertyName.startsWith("get") ? 3 : 2));

            if (annotations != null) {
                // Some properties are generated with wrong property name in an OpenAPI schema.
                // For example "aBC" field is represented as "getaBC" in the OpenAPI schema.
                Arrays.stream(annotations)
                        .filter(e -> e instanceof XmlElement)
                        .map(x -> ((XmlElement) x).name())
                        .findFirst()
                        .or(() -> Arrays.stream(annotations)
                                .filter(e -> e instanceof XmlAttribute)
                                .map(x -> ((XmlAttribute) x).name())
                                .findFirst()
                        ).ifPresent(property::setName);
            }
        }
    }

    @Override
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {
        if (annotatedType == null) {
            return null;
        }
        if (this.shouldIgnoreClass(annotatedType.getType())) {
            return null;
        }

        var name = resolveSchemaName(annotatedType);
        if (name != null && context.getDefinedModels().containsKey(name)) {
            return context.getDefinedModels().get(name);
        } else {
            return super.resolve(annotatedType, context, next);
        }
    }

    /**
     * Resolves the schema name for the given annotated type.
     *
     * @param annotatedType the annotated type
     * @return the resolved schema name
     * @see ModelResolver#resolve(AnnotatedType, ModelConverterContext, Iterator)
     */
    private String resolveSchemaName(AnnotatedType annotatedType) {
        final JavaType type;
        if (annotatedType.getType() instanceof JavaType) {
            type = (JavaType) annotatedType.getType();
        } else {
            type = _mapper.constructType(annotatedType.getType());
        }

        final Annotation resolvedSchemaOrArrayAnnotation = AnnotationsUtils.mergeSchemaAnnotations(annotatedType.getCtxAnnotations(), type);
        final io.swagger.v3.oas.annotations.media.Schema resolvedSchemaAnnotation =
                resolvedSchemaOrArrayAnnotation == null ?
                        null :
                        resolvedSchemaOrArrayAnnotation instanceof io.swagger.v3.oas.annotations.media.ArraySchema ?
                                ((io.swagger.v3.oas.annotations.media.ArraySchema) resolvedSchemaOrArrayAnnotation).schema() :
                                (io.swagger.v3.oas.annotations.media.Schema) resolvedSchemaOrArrayAnnotation;

        final BeanDescription beanDesc;
        {
            BeanDescription recurBeanDesc = _mapper.getSerializationConfig().introspect(type);

            HashSet<String> visited = new HashSet<>();
            JsonSerialize jsonSerialize = recurBeanDesc.getClassAnnotations().get(JsonSerialize.class);
            while (jsonSerialize != null && !Void.class.equals(jsonSerialize.as())) {
                String asName = jsonSerialize.as().getName();
                if (visited.contains(asName)) break;
                visited.add(asName);

                recurBeanDesc = _mapper.getSerializationConfig().introspect(
                        _mapper.constructType(jsonSerialize.as())
                );
                jsonSerialize = recurBeanDesc.getClassAnnotations().get(JsonSerialize.class);
            }
            beanDesc = recurBeanDesc;
        }


        String name = annotatedType.getName();
        if (StringUtils.isBlank(name)) {
            // allow override of name from annotation
            if (!annotatedType.isSkipSchemaName() && resolvedSchemaAnnotation != null && !resolvedSchemaAnnotation.name().isEmpty()) {
                name = resolvedSchemaAnnotation.name();
            }
            if (StringUtils.isBlank(name) && (type.isEnumType() || !ReflectionUtils.isSystemType(type))) {
                name = _typeName(type, beanDesc);
            }
        }

        return decorateModelName(annotatedType, name);
    }
}
