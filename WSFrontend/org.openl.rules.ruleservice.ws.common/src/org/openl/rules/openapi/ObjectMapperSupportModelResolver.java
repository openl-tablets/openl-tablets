package org.openl.rules.openapi;

import static io.swagger.v3.core.util.RefUtils.constructRef;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.util.JAXBUtils;

class ObjectMapperSupportModelResolver extends ModelResolver {

    private static final String ARRAY_TYPE = "array";
    private static final String BYTE_FORMAT = "byte";
    private static final ObjectMapper EXAMPLE_MAPPER = ObjectMapperFactory.buildStrictGenericObjectMapper();

    // Concurrent: the converters of a build are global, so another thread may resolve a schema with this resolver
    private final Map<JavaType, Map<String, BeanPropertyDefinition>> deserializedProperties = new ConcurrentHashMap<>();
    private final Map<JavaType, BeanDescription> serializedTypes = new ConcurrentHashMap<>();

    public ObjectMapperSupportModelResolver(ObjectMapper mapper) {
        super(mapper);
    }

    /**
     * Resolves the access mode of a property as the parent does.
     *
     * <p>The parent reads all the properties of the type again for every property it resolves, which makes a type
     * with hundreds of properties take seconds. Here the properties of a type are read once per resolver.
     */
    @Override
    protected @Nullable AccessMode resolveAccessMode(@Nullable BeanPropertyDefinition propDef,
                                                     JavaType type,
                                                     io.swagger.v3.oas.annotations.media.@Nullable Schema schema) {
        if (propDef == null || schema != null
                && (schema.accessMode() != AccessMode.AUTO || schema.readOnly() || schema.writeOnly())) {
            return super.resolveAccessMode(propDef, type, schema);
        }
        var access = findAccess(propDef);
        var hasGetter = propDef.hasGetter();
        var hasSetter = propDef.hasSetter();
        var hasConstructorParameter = propDef.hasConstructorParameter();
        var hasField = propDef.hasField();
        var deserialized = access == null ? findDeserializedProperty(type, propDef.getInternalName()) : null;
        if (deserialized != null) {
            access = findAccess(deserialized);
            hasGetter |= deserialized.hasGetter();
            hasSetter |= deserialized.hasSetter();
            hasConstructorParameter |= deserialized.hasConstructorParameter();
            hasField |= deserialized.hasField();
        }
        if (access != null) {
            return switch (access) {
                case READ_ONLY -> AccessMode.READ_ONLY;
                case READ_WRITE -> AccessMode.READ_WRITE;
                case WRITE_ONLY -> AccessMode.WRITE_ONLY;
                case AUTO -> AccessMode.AUTO;
            };
        }
        return !hasGetter && !hasField && (hasConstructorParameter || hasSetter) ? AccessMode.WRITE_ONLY : null;
    }

    private static JsonProperty.@Nullable Access findAccess(BeanPropertyDefinition propDef) {
        return propDef instanceof POJOPropertyBuilder builder ? builder.findAccess() : null;
    }

    private @Nullable BeanPropertyDefinition findDeserializedProperty(JavaType type, @Nullable String internalName) {
        if (StringUtils.isBlank(internalName)) {
            return null;
        }
        return deserializedProperties.computeIfAbsent(type, this::readDeserializedProperties).get(internalName);
    }

    /**
     * Reads the properties the type is deserialized with, by their internal names. The first property of a name
     * wins.
     */
    private Map<String, BeanPropertyDefinition> readDeserializedProperties(JavaType type) {
        var properties = new HashMap<String, BeanPropertyDefinition>();
        for (var prop : _mapper.getDeserializationConfig().introspect(type).findProperties()) {
            if (StringUtils.isNotBlank(prop.getInternalName())) {
                properties.putIfAbsent(prop.getInternalName(), prop);
            }
        }
        return properties;
    }

    private Object stringToRawType(Annotated a, String value) {
        var t = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(a.getRawType());
        Class<?> rawType = t == null ? a.getRawType() : t;
        try {
            var convertor = String2DataConvertorFactory.getConvertor(rawType);
            if (convertor != null) {
                return convertor.parse(value, null);
            }
        } catch (Exception ignore) {
            return null;
        }
        return value;
    }

    @Override
    protected Object resolveExample(Annotated a, Annotation[] annotations, io.swagger.v3.oas.annotations.media.Schema schema) {
        var exampleValue = super.resolveExample(a, annotations, schema);
        if (exampleValue instanceof String string) {
            return stringToRawType(a, string);
        }
        return exampleValue;
    }

    @Override
    protected Object resolveDefaultValue(Annotated a,
                                         Annotation[] annotations,
                                         io.swagger.v3.oas.annotations.media.Schema schema) {
        var defaultValue = super.resolveDefaultValue(a, annotations, schema);
        if (defaultValue instanceof String string) {
            return stringToRawType(a, string);
        }
        return defaultValue;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void resolveDiscriminatorProperty(JavaType type, ModelConverterContext context, Schema model) {
        final var beanDesc = _mapper.getSerializationConfig().introspect(type);
        var typeInfo = beanDesc.getClassAnnotations().get(JsonTypeInfo.class);
        if (typeInfo != null) {
            if (beanDesc.getBeanClass().getSuperclass() != null) {
                final var superBeanDesc = _mapper.getSerializationConfig()
                        .introspect(TypeFactory.defaultInstance().constructType(beanDesc.getBeanClass().getSuperclass()));
                var superJsonTypeInfo = superBeanDesc.getClassInfo().getAnnotation(JsonTypeInfo.class);
                var jsonSubTypes = superBeanDesc.getClassInfo().getAnnotation(JsonSubTypes.class);
                if (isListedSubtype(jsonSubTypes, type)
                        && Objects.equals(superJsonTypeInfo.property(), typeInfo.property())) {
                    return;
                }
            }
            var typeInfoProp = typeInfo.property();
            if (StringUtils.isNotBlank(typeInfoProp)) {
                addDiscriminatorProperty(context, model, typeInfoProp);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void addDiscriminatorProperty(ModelConverterContext context, Schema model, String typeInfoProp) {
        var modelToUpdate = model;
        if (StringUtils.isNotBlank(model.get$ref())) {
            modelToUpdate = context.getDefinedModels().get(model.get$ref().substring(21));
        }
        if (modelToUpdate.getProperties() == null || !modelToUpdate.getProperties().containsKey(typeInfoProp)) {
            var discriminatorSchema = new StringSchema().name(typeInfoProp);
            modelToUpdate.addProperties(typeInfoProp, discriminatorSchema);
            if (modelToUpdate.getRequired() == null || !modelToUpdate.getRequired().contains(typeInfoProp)) {
                modelToUpdate.addRequiredItem(typeInfoProp);
            }
        }
    }

    private static boolean isListedSubtype(@Nullable JsonSubTypes jsonSubTypes, JavaType type) {
        if (jsonSubTypes != null) {
            for (JsonSubTypes.Type subType : jsonSubTypes.value()) {
                if (subType.value() == type.getRawClass()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Discriminator resolveDiscriminator(JavaType type, ModelConverterContext context) {

        io.swagger.v3.oas.annotations.media.Schema declaredSchemaAnnotation = AnnotationsUtils
                .getSchemaDeclaredAnnotation(type.getRawClass());

        String disc = declaredSchemaAnnotation == null ? "" : declaredSchemaAnnotation.discriminatorProperty();
        var avoidDisc = false;
        if (disc.isEmpty()) {
            final var beanDesc = _mapper.getSerializationConfig().introspect(type);
            var a = beanDesc.getClassInfo();
            // longer method would involve AnnotationIntrospector.findTypeResolver(...) but:
            var typeInfo = a.getAnnotation(JsonTypeInfo.class);
            if (typeInfo != null) {
                disc = typeInfo.property();
            }

            if (StringUtils.isNotBlank(disc) && beanDesc.getBeanClass().getSuperclass() != null) {
                final var superBeanDesc = _mapper.getSerializationConfig()
                        .introspect(TypeFactory.defaultInstance().constructType(beanDesc.getBeanClass().getSuperclass()));
                var superJsonTypeInfo = superBeanDesc.getClassInfo().getAnnotation(JsonTypeInfo.class);
                var jsonSubTypes = superBeanDesc.getClassInfo().getAnnotation(JsonSubTypes.class);
                avoidDisc = isListedSubtype(jsonSubTypes, type) && superJsonTypeInfo != null
                        && Objects.equals(superJsonTypeInfo.property(), disc);
            }
        }
        if (StringUtils.isNotBlank(disc) && !avoidDisc) {
            var discriminator = new Discriminator().propertyName(disc);
            if (declaredSchemaAnnotation != null) {
                addDiscriminatorMappings(discriminator, declaredSchemaAnnotation, context);
            }
            return discriminator;
        }
        return null;
    }

    private static void addDiscriminatorMappings(Discriminator discriminator,
                                                 io.swagger.v3.oas.annotations.media.Schema declaredSchemaAnnotation,
                                                 ModelConverterContext context) {
        var mappings = declaredSchemaAnnotation.discriminatorMapping();
        for (DiscriminatorMapping mapping : mappings) {
            if (!mapping.value().isEmpty() && !mapping.schema().equals(Void.class)) {
                discriminator.mapping(mapping.value(),
                        constructRef(context.resolve(new AnnotatedType().type(mapping.schema())).getName()));
            }
        }
    }

    /**
     * Describes a property by the {@link Parameter} of its field, getter or setter: its description and example.
     *
     * <p>They describe the property only. The elements of a list or a map and the schema of a datatype the
     * property refers to keep their own description.
     *
     * <p>A description or an example the {@code @Schema} of the property gives, as a MixIn does, wins. An example
     * that cannot be read as the property type is left out. A property that refers to a datatype is written as a
     * bare reference, so its description is not shown.
     */
    @Override
    protected boolean applyBeanValidatorAnnotations(BeanPropertyDefinition propDef,
                                                    Schema property,
                                                    Annotation[] annotations,
                                                    Schema parent,
                                                    boolean applyNotNullAnnotations) {
        var modified = super.applyBeanValidatorAnnotations(propDef,
                property,
                annotations,
                parent,
                applyNotNullAnnotations);
        var parameter = findAnnotation(propDef, Parameter.class);
        if (parameter == null) {
            return modified;
        }
        var schema = findAnnotation(propDef, io.swagger.v3.oas.annotations.media.Schema.class);
        if (StringUtils.isNotBlank(parameter.description()) && (schema == null || schema.description().isEmpty())) {
            property.setDescription(parameter.description());
        }
        if (StringUtils.isNotBlank(parameter.example()) && (schema == null || schema.example().isEmpty())) {
            var value = toExample(property, parseExample(property, propDef.getPrimaryMember(), parameter.example()));
            if (value != null) {
                property.setExample(value);
            }
        }
        return true;
    }

    /**
     * Finds an annotation declared on the property itself. The annotations of a property also carry the
     * {@code @Parameter} of the property that holds the bean, which must not describe this one.
     */
    private static <A extends Annotation> @Nullable A findAnnotation(BeanPropertyDefinition propDef, Class<A> type) {
        return Stream.of(propDef.getField(), propDef.getGetter(), propDef.getSetter())
                .filter(Objects::nonNull)
                .map(member -> member.getAnnotation(type))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Stores an example the way the schema of the property compares and writes it.
     *
     * <p>An array schema holds a list, so the same example read for a parent type and for its subtype is equal and
     * the subtype does not repeat the property it inherits. A binary schema holds the bytes of the value in base64.
     */
    private static @Nullable Object toExample(Schema property, @Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (ARRAY_TYPE.equals(property.getType())) {
            return toList(value);
        }
        if (BYTE_FORMAT.equals(property.getFormat())) {
            var binary = new ByteArraySchema();
            binary.setExample(value);
            return binary.getExample() instanceof byte[] bytes ? Base64.getEncoder().encodeToString(bytes) : null;
        }
        return value;
    }

    /**
     * Reads an example: a JSON value as it is, any other text by the property type.
     *
     * <p>A JSON string is read by the property type without its quotes. A single JSON value of an array property
     * is read by the property type too, as an array of one element.
     */
    private @Nullable Object parseExample(Schema property, @Nullable Annotated member, String example) {
        var text = example;
        try {
            var node = EXAMPLE_MAPPER.readTree(example);
            if (node.isTextual()) {
                text = node.textValue();
            } else if (!node.isNull() && (node.isContainerNode() || !ARRAY_TYPE.equals(property.getType()))) {
                return node;
            }
        } catch (IOException e) {
            // not JSON: converted by the property type below
        }
        return member == null ? text : stringToRawType(member, text);
    }

    private static @Nullable Object toList(@Nullable Object value) {
        if (value == null || !value.getClass().isArray()) {
            return value;
        }
        var length = Array.getLength(value);
        var list = new ArrayList<@Nullable Object>(length);
        for (var i = 0; i < length; i++) {
            list.add(toList(Array.get(value, i)));
        }
        return list;
    }

    @Override
    protected boolean applyBeanValidatorAnnotations(Schema property, Annotation[] annotations, Schema parent, boolean applyNotNullAnnotations) {
        var modified = super.applyBeanValidatorAnnotations(property, annotations, parent, applyNotNullAnnotations);
        var propertyName = property.getName();
        if (propertyName != null && (propertyName.startsWith("get") || propertyName.startsWith("is"))) {
            modified = true;
            // datatype with incorrect field is generated if property name looks like a getter method
            property.setName(propertyName.substring(propertyName.startsWith("get") ? 3 : 2));

            if (annotations != null) {
                // Some properties are generated with wrong property name in an OpenAPI schema.
                // For example "aBC" field is represented as "getaBC" in the OpenAPI schema.
                Arrays.stream(annotations)
                        .filter(XmlElement.class::isInstance)
                        .map(x -> ((XmlElement) x).name())
                        .findFirst()
                        .or(() -> Arrays.stream(annotations)
                                .filter(XmlAttribute.class::isInstance)
                                .map(x -> ((XmlAttribute) x).name())
                                .findFirst()
                        ).ifPresent(property::setName);
            }
        }
        return modified;
    }

    @Override
    public @Nullable Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {
        if (annotatedType == null) {
            return null;
        }
        if (this.shouldIgnoreClass(annotatedType.getType())) {
            return null;
        }

        var name = resolveSchemaName(annotatedType);
        if (name != null && context.getDefinedModels().containsKey(name)) {
            return context.getDefinedModels().get(name);
        }
        return super.resolve(annotatedType, context, next);
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
        if (annotatedType.getType() instanceof JavaType javaType) {
            type = javaType;
        } else {
            type = _mapper.constructType(annotatedType.getType());
        }

        final Annotation resolvedSchemaOrArrayAnnotation = AnnotationsUtils.mergeSchemaAnnotations(annotatedType.getCtxAnnotations(), type);
        final io.swagger.v3.oas.annotations.media.Schema resolvedSchemaAnnotation =
                resolvedSchemaOrArrayAnnotation instanceof ArraySchema as
                        ? as.schema()
                        : (io.swagger.v3.oas.annotations.media.Schema) resolvedSchemaOrArrayAnnotation;

        final var beanDesc = serializedTypes.computeIfAbsent(type, this::introspectSerializedType);

        var name = annotatedType.getName();
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

    /**
     * Describes the type that values of the given type are serialized as.
     *
     * <p>A type annotated with {@link JsonSerialize#as()} is serialized as the type the annotation names, which may
     * carry the annotation in turn. The chain is followed to its end; a type that repeats ends it.
     */
    private BeanDescription introspectSerializedType(JavaType type) {
        var recurBeanDesc = _mapper.getSerializationConfig().introspect(type);

        var visited = new HashSet<String>();
        var jsonSerialize = recurBeanDesc.getClassAnnotations().get(JsonSerialize.class);
        while (jsonSerialize != null && !Void.class.equals(jsonSerialize.as())) {
            var asName = jsonSerialize.as().getName();
            if (visited.contains(asName)) {
                break;
            }
            visited.add(asName);

            recurBeanDesc = _mapper.getSerializationConfig().introspect(
                    _mapper.constructType(jsonSerialize.as())
            );
            jsonSerialize = recurBeanDesc.getClassAnnotations().get(JsonSerialize.class);
        }
        return recurBeanDesc;
    }
}
