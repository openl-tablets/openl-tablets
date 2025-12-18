package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * OpenAPI RequestBody service helps to parse and build OpenAPI request bodies from API annotation and Spring
 * declaration
 *
 * @author Vladyslav Pikus
 */
@Component
public class OpenApiRequestServiceImpl implements OpenApiRequestService {

    private final OpenApiParameterService apiParameterService;
    private final OpenApiPropertyResolver propertyResolver;

    public OpenApiRequestServiceImpl(OpenApiParameterService apiParameterService,
                                     OpenApiPropertyResolver propertyResolver) {
        this.apiParameterService = apiParameterService;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public RequestBody generateRequestBody(OpenApiContext apiContext,
                                           MethodInfo methodInfo,
                                           List<ParameterInfo> formParameters,
                                           ParameterInfo requestBodyParam) {

        RequestBody requestBody = null;
        if (methodInfo.getOperationAnnotation() != null) {
            requestBody = parseRequestBody(methodInfo.getOperationAnnotation().requestBody(),
                    methodInfo,
                    apiContext.getComponents());
        }
        if (requestBody == null) {
            var apiRequestBody = ReflectionUtils.getAnnotation(methodInfo.getMethod(),
                    io.swagger.v3.oas.annotations.parameters.RequestBody.class);
            requestBody = parseRequestBody(apiRequestBody, methodInfo, apiContext.getComponents());
        }

        RequestBody springRequestBody = requestBodyParam != null ? parseSpringRequestBody(requestBodyParam,
                apiContext.getComponents()) : parseFormRequestBody(formParameters, methodInfo, apiContext.getComponents());

        return merge(requestBody, springRequestBody);
    }

    private RequestBody merge(RequestBody first, RequestBody second) {
        if (first == null) {
            return second;
        } else if (second == null) {
            return first;
        }
        if (first.getContent() == null) {
            first.setContent(second.getContent());
        } else if (second.getContent() != null) {
            var currentContent = first.getContent();
            for (var entry : second.getContent().entrySet()) {
                var currentMediaType = currentContent.get(entry.getKey());
                if (currentMediaType == null) {
                    currentContent.addMediaType(entry.getKey(), entry.getValue());
                } else if (currentMediaType.getSchema() == null) {
                    currentMediaType.setSchema(entry.getValue().getSchema());
                }
            }
        }
        return first;
    }

    private RequestBody parseFormRequestBody(List<ParameterInfo> formParamInfos,
                                             MethodInfo methodInfo,
                                             Components components) {
        var objectSchema = new ObjectSchema();
        Map<String, Encoding> encodingMap = new LinkedHashMap<>();

        for (var paramInfo : formParamInfos) {
            // Check if this is a ModelAttribute - if so, expand its fields
            var modelAttribute = paramInfo.getParameterAnnotation(ModelAttribute.class);
            if (modelAttribute != null) {
                expandModelAttributeFields(paramInfo, objectSchema, components);
                continue;
            }

            var requestParam = paramInfo.getParameterAnnotation(RequestParam.class);
            var requestPart = paramInfo.getParameterAnnotation(RequestPart.class);
            var nameRef = new Object() {
                String name = null;
            };
            boolean required;
            if (requestParam != null) {
                if (StringUtils.isNotBlank(requestParam.name())) {
                    nameRef.name = requestParam.name();
                }
                required = requestParam.required();
            } else {
                if (StringUtils.isNotBlank(requestPart.name())) {
                    nameRef.name = requestPart.name();
                }
                required = requestPart.required();
            }
            var apiParameter = paramInfo.getParameter();
            if (apiParameter != null) {
                if (StringUtils.isNotBlank(apiParameter.name())) {
                    nameRef.name = apiParameter.name();
                }
                required = apiParameter.required();
            }
            if (nameRef.name == null) {
                nameRef.name = "arg" + paramInfo.getIndex();
            }
            if (required) {
                addRequiredItemIfAbsent(objectSchema, nameRef.name);
            }

            var parameterType = ParameterProcessor.getParameterType(paramInfo.getParameter(), true);
            if (parameterType == null) {
                parameterType = paramInfo.getType();
            }
            var schema = apiParameterService.resolveSchema(parameterType, components, paramInfo.getJsonView());
            if (apiParameter != null) {
                if (StringUtils.isNotBlank(apiParameter.description())) {
                    schema.setDescription(propertyResolver.resolve(apiParameter.description()));
                }
                if (apiParameter.schema() != null && StringUtils.isNotBlank(apiParameter.schema().defaultValue())) {
                    schema.setDefault(apiParameter.schema().defaultValue());
                }
                for (var content : apiParameter.content()) {
                    Stream.of(content.encoding()).map(apiEncoding -> {
                        var encoding = new Encoding();
                        if (StringUtils.isNotBlank(apiEncoding.contentType())) {
                            encoding.contentType(apiEncoding.contentType());
                        }
                        if (StringUtils.isNotBlank(apiEncoding.style())) {
                            encoding.style(Encoding.StyleEnum.valueOf(apiEncoding.style()));
                        }
                        if (apiEncoding.explode()) {
                            encoding.setExplode(Boolean.TRUE);
                        }
                        if (apiEncoding.allowReserved()) {
                            encoding.setAllowReserved(Boolean.TRUE);
                        }
                        AnnotationsUtils.getHeaders(apiEncoding.headers(), null).ifPresent(encoding::setHeaders);
                        encoding.setExtensions(AnnotationsUtils.getExtensions(apiEncoding.extensions()));
                        return encoding;
                    }).forEach(encoding -> encodingMap.put(nameRef.name, encoding));
                }
            }
            apiParameterService.applyValidationAnnotations(paramInfo, schema);
            objectSchema.addProperty(nameRef.name, schema);
        }

        RequestBody requestBody = null;
        if (CollectionUtils.isNotEmpty(objectSchema.getProperties())) {
            var content = new Content();
            for (String consume : methodInfo.getConsumes()) {
                var mediaType = new MediaType().schema(objectSchema);
                if (!encodingMap.isEmpty()) {
                    mediaType.encoding(encodingMap);
                }
                content.addMediaType(consume, mediaType);
            }
            requestBody = new RequestBody().content(content);
        }
        return requestBody;
    }

    private RequestBody parseSpringRequestBody(ParameterInfo requestBodyParam, Components components) {
        var methodInfo = requestBodyParam.getMethodInfo();
        var requestBodyAnno = requestBodyParam
                .getParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class);
        var apiParameter = requestBodyParam.getParameter();
        var requestBody = new RequestBody();
        if (requestBodyAnno != null && requestBodyAnno.required()) {
            requestBody.setRequired(Boolean.TRUE);
        }
        if (apiParameter != null) {
            if (StringUtils.isNotBlank(apiParameter.ref())) {
                requestBody.set$ref(apiParameter.ref());
            }
            if (StringUtils.isNotBlank(apiParameter.description())) {
                requestBody.setDescription(propertyResolver.resolve(apiParameter.description()));
            }
            if (apiParameter.required()) {
                requestBody.setRequired(Boolean.TRUE);
            }
            if (apiParameter.extensions().length > 0) {
                AnnotationsUtils.getExtensions(apiParameter.extensions()).forEach(requestBody::addExtension);
            }
        }
        var parameterType = ParameterProcessor.getParameterType(requestBodyParam.getParameter(), true);
        if (parameterType == null) {
            parameterType = requestBodyParam.getType();
            if (isRequestBodyType(parameterType)) {
                parameterType = ((ParameterizedType) parameterType).getActualTypeArguments()[0];
            }
        }
        String[] consumes = resolveConsumes(methodInfo,
                (Class<?>) (parameterType instanceof ParameterizedType ? ((ParameterizedType) parameterType).getRawType()
                        : parameterType));
        var parameter = ParameterProcessor.applyAnnotations(null,
                parameterType,
                apiParameter != null ? List.of(apiParameter) : Collections.emptyList(),
                components,
                new String[0],
                consumes,
                requestBodyParam.getJsonView());
        if (parameter.getContent() != null && !parameter.getContent().isEmpty()) {
            requestBody.setContent(parameter.getContent());
        } else if (parameter.getSchema() != null) {
            var content = new Content();
            Stream.of(consumes).forEach(consume -> {
                var mediaTypeObject = new MediaType();
                mediaTypeObject.setSchema(parameter.getSchema());
                content.addMediaType(consume, mediaTypeObject);
            });
            requestBody.setContent(content);
        }
        return requestBody;
    }

    private String[] resolveConsumes(MethodInfo methodInfo, Class<?> cl) {
        String[] consumes = methodInfo.getConsumes();
        if (consumes == MethodInfo.ALL_MEDIA_TYPES) {
            String[] possibleConsumes = apiParameterService.getMediaTypesForType(cl);
            if (possibleConsumes.length > 0) {
                consumes = possibleConsumes;
            }
        }
        return consumes;
    }

    private RequestBody parseRequestBody(io.swagger.v3.oas.annotations.parameters.RequestBody apiRequestBody,
                                         MethodInfo methodInfo,
                                         Components components) {
        if (apiRequestBody == null) {
            return null;
        }
        RequestBody requestBody = new RequestBody();
        boolean empty = true;

        if (StringUtils.isNotBlank(apiRequestBody.ref())) {
            requestBody.set$ref(apiRequestBody.ref());
            empty = false;
        }
        if (StringUtils.isNotBlank(apiRequestBody.description())) {
            requestBody.setDescription(apiRequestBody.description());
            empty = false;
        }
        if (apiRequestBody.required()) {
            requestBody.setRequired(true);
            empty = false;
        }
        if (apiRequestBody.extensions().length > 0) {
            AnnotationsUtils.getExtensions(apiRequestBody.extensions()).forEach(requestBody::addExtension);
            empty = false;
        }

        if (apiRequestBody.content().length > 0) {
            empty = false;
        }

        if (empty) {
            return null;
        }
        AnnotationsUtils
                .getContent(apiRequestBody
                        .content(), new String[0], methodInfo.getConsumes(), null, components, methodInfo.getJsonView())
                .ifPresent(requestBody::setContent);
        return requestBody;
    }

    /**
     * Merges source RequestBody to target
     *
     * @param target target request body
     * @param source source request body
     */
    @Override
    public void mergeRequestBody(RequestBody source, RequestBody target) {
        var targetContent = target.getContent();
        if (targetContent == null) {
            target.setContent(source.getContent());
        } else if (source.getContent() != null) {
            for (var entry : source.getContent().entrySet()) {
                targetContent.addMediaType(entry.getKey(), entry.getValue());
            }
        }
        if (StringUtils.isNotBlank(source.getDescription())) {
            target.description(source.getDescription());
        }
    }

    /**
     * Check if current parameter is RequestBody
     *
     * @param paramInfo method parameter
     * @return is request body or not
     */
    @Override
    public boolean isRequestBody(ParameterInfo paramInfo) {
        return paramInfo.hasAnnotation(org.springframework.web.bind.annotation.RequestBody.class) || isRequestBodyType(
                paramInfo.getType());
    }

    private boolean isRequestBodyType(Type type) {
        if (type instanceof ParameterizedType) {
            var rawType = ((ParameterizedType) type).getRawType();
            return rawType == HttpEntity.class || rawType == RequestEntity.class;
        }
        return false;
    }

    /**
     * Expands @ModelAttribute parameter fields into form parameters.
     * <p>
     * This method introspects the model attribute class and creates a form parameter for each field.
     * It supports:
     * <ul>
     *   <li>Simple types (String, Integer, etc.)</li>
     *   <li>Complex types and nested objects (resolved via schema resolution)</li>
     *   <li>Collections (List, Set, etc.) including complex element types</li>
     *   <li>File uploads (MultipartFile fields)</li>
     *   <li>@Schema annotations on fields for documentation</li>
     *   <li>@Parameter annotations on fields (takes precedence over @Schema)</li>
     *   <li>Jakarta validation annotations (@NotNull, @Size, @Min, @Max, @Pattern, etc.)</li>
     * </ul>
     *
     * @param paramInfo    model attribute parameter info
     * @param objectSchema schema to add properties to
     * @param components   OpenAPI components for schema resolution
     */
    private void expandModelAttributeFields(ParameterInfo paramInfo,
                                            ObjectSchema objectSchema,
                                            Components components) {
        Type paramType = paramInfo.getType();
        Class<?> modelClass = paramType instanceof Class ? (Class<?>) paramType :
                paramType instanceof ParameterizedType ?
                        (Class<?>) ((ParameterizedType) paramType).getRawType() : null;

        if (modelClass == null) {
            return;
        }

        // Get all fields including inherited ones
        for (Field field : modelClass.getDeclaredFields()) {
            // Skip synthetic fields
            if (field.isSynthetic()) {
                continue;
            }

            String fieldName = field.getName();
            Type fieldType = field.getGenericType();

            // Check for @Schema annotation on field
            var schemaAnnotation = field.getAnnotation(Schema.class);

            // Check for @Parameter annotation on field (for additional metadata)
            var parameterAnnotation = field.getAnnotation(Parameter.class);

            // Resolve schema for the field
            var schema = apiParameterService.resolveSchema(fieldType, components, paramInfo.getJsonView());

            // Apply @Parameter annotation if present (takes precedence)
            if (parameterAnnotation != null) {
                if (StringUtils.isNotBlank(parameterAnnotation.description())) {
                    schema.setDescription(propertyResolver.resolve(parameterAnnotation.description()));
                }
                if (parameterAnnotation.required()) {
                    addRequiredItemIfAbsent(objectSchema, fieldName);
                }
                // Apply parameter schema properties
                if (parameterAnnotation.schema() != null) {
                    if (StringUtils.isNotBlank(parameterAnnotation.schema().defaultValue())) {
                        schema.setDefault(parameterAnnotation.schema().defaultValue());
                    }
                    if (StringUtils.isNotBlank(parameterAnnotation.schema().example())) {
                        schema.setExample(parameterAnnotation.schema().example());
                    }
                }
            }
            // Apply @Schema annotation properties if present (if @Parameter wasn't used)
            else if (schemaAnnotation != null) {
                if (StringUtils.isNotBlank(schemaAnnotation.description())) {
                    schema.setDescription(propertyResolver.resolve(schemaAnnotation.description()));
                }
                if (StringUtils.isNotBlank(schemaAnnotation.example())) {
                    schema.setExample(schemaAnnotation.example());
                }
                if (StringUtils.isNotBlank(schemaAnnotation.defaultValue())) {
                    schema.setDefault(schemaAnnotation.defaultValue());
                }

                // Handle required mode
                if (schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED) {
                    addRequiredItemIfAbsent(objectSchema, fieldName);
                }
            }

            // Apply validation annotations from field
            applyFieldValidationAnnotations(field, schema, objectSchema, fieldName);

            // Add the field as a property
            objectSchema.addProperty(fieldName, schema);
        }
    }

    private void addRequiredItemIfAbsent(ObjectSchema objectSchema, String fieldName) {
        List<String> required = objectSchema.getRequired();
        if (required == null || !required.contains(fieldName)) {
            objectSchema.addRequiredItem(fieldName);
        }
    }

    /**
     * Applies validation annotations from a field to the schema
     *
     * @param field        field to extract annotations from
     * @param schema       schema to apply validations to
     * @param objectSchema parent schema for required fields
     * @param fieldName    name of the field
     */
    private void applyFieldValidationAnnotations(Field field,
                                                 io.swagger.v3.oas.models.media.Schema<?> schema,
                                                 ObjectSchema objectSchema,
                                                 String fieldName) {
        // Check for @NotNull, @NotBlank, @NotEmpty
        if (field.isAnnotationPresent(NotNull.class) ||
                field.isAnnotationPresent(NotBlank.class) ||
                field.isAnnotationPresent(NotEmpty.class)) {
            addRequiredItemIfAbsent(objectSchema, fieldName);
        }

        // Check for @Size
        var sizeAnnotation = field.getAnnotation(Size.class);
        if (sizeAnnotation != null) {
            if (sizeAnnotation.min() > 0) {
                schema.setMinLength(sizeAnnotation.min());
            }
            if (sizeAnnotation.max() < Integer.MAX_VALUE) {
                schema.setMaxLength(sizeAnnotation.max());
            }
        }

        // Check for @Min / @Max
        var minAnnotation = field.getAnnotation(Min.class);
        if (minAnnotation != null) {
            schema.setMinimum(java.math.BigDecimal.valueOf(minAnnotation.value()));
        }

        var maxAnnotation = field.getAnnotation(Max.class);
        if (maxAnnotation != null) {
            schema.setMaximum(java.math.BigDecimal.valueOf(maxAnnotation.value()));
        }

        // Check for @Pattern
        var patternAnnotation = field.getAnnotation(Pattern.class);
        if (patternAnnotation != null) {
            schema.setPattern(patternAnnotation.regexp());
        }
    }

    /**
     * Check if current parameter is ModelAttribute
     *
     * @param paramInfo method parameter
     * @return is model attribute or not
     */
    @Override
    public boolean isModelAttribute(ParameterInfo paramInfo) {
        return paramInfo.hasAnnotation(ModelAttribute.class);
    }
}
