package org.openl.rules.spring.openapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;

@Component
public class OpenApiRequestService {

    private final OpenApiParameterService apiParameterService;

    public OpenApiRequestService(OpenApiParameterService apiParameterService) {
        this.apiParameterService = apiParameterService;
    }

    public Optional<RequestBody> parse(OpenApiContext apiContext,
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

        return Optional.ofNullable(merge(requestBody, springRequestBody));
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

        for (var paramInfo : formParamInfos) {
            var requestParam = paramInfo.getParameterAnnotation(RequestParam.class);
            var requestPart = paramInfo.getParameterAnnotation(RequestPart.class);
            String name = null;
            boolean required;
            if (requestParam != null) {
                if (StringUtils.isNotBlank(requestParam.name())) {
                    name = requestParam.name();
                }
                required = requestParam.required();
            } else {
                if (StringUtils.isNotBlank(requestPart.name())) {
                    name = requestPart.name();
                }
                required = requestPart.required();
            }
            var apiParameter = paramInfo.getParameter();
            if (apiParameter != null) {
                if (StringUtils.isNotBlank(apiParameter.name())) {
                    name = apiParameter.name();
                }
                required = apiParameter.required();
            }
            if (name == null) {
                name = "arg" + paramInfo.getIndex();
            }
            if (required) {
                objectSchema.addRequiredItem(name);
            }

            var parameterType = ParameterProcessor.getParameterType(paramInfo.getParameter(), true);
            if (parameterType == null) {
                parameterType = paramInfo.getType();
            }
            var schema = apiParameterService.resolveSchema(parameterType, components, paramInfo.getJsonView());
            apiParameterService.applyValidationAnnotations(paramInfo, schema);
            objectSchema.addProperties(name, schema);
        }

        RequestBody requestBody = null;
        if (CollectionUtils.isNotEmpty(objectSchema.getProperties())) {
            var content = new Content();
            for (String consume : methodInfo.getConsumes()) {
                content.addMediaType(consume, new io.swagger.v3.oas.models.media.MediaType().schema(objectSchema));
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
        if (requestBodyAnno.required()) {
            requestBody.setRequired(Boolean.TRUE);
        }
        if (apiParameter != null) {
            if (StringUtils.isNotBlank(apiParameter.ref())) {
                requestBody.set$ref(apiParameter.ref());
            }
            if (StringUtils.isNotBlank(apiParameter.description())) {
                requestBody.setDescription(apiParameter.description());
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
        }
        var parameter = ParameterProcessor.applyAnnotations(null,
            parameterType,
            apiParameter != null ? List.of(apiParameter) : Collections.emptyList(),
            components,
            new String[0],
            methodInfo.getConsumes(),
            methodInfo.getJsonView());
        if (parameter.getContent() != null && !parameter.getContent().isEmpty()) {
            requestBody.setContent(parameter.getContent());
        } else if (parameter.getSchema() != null) {
            var content = new Content();
            Stream.of(methodInfo.getConsumes()).forEach(consume -> {
                var mediaTypeObject = new MediaType();
                mediaTypeObject.setSchema(parameter.getSchema());
                content.addMediaType(consume, mediaTypeObject);
            });
            requestBody.setContent(content);
        }
        return requestBody;
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
}
