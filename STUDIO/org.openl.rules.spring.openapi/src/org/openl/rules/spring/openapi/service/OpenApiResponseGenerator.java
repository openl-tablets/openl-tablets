package org.openl.rules.spring.openapi.service;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.rules.spring.openapi.OpenApiUtils;
import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * The builder class for OpenAPI responses
 *
 * @author Vladyslav Pikus
 */
@Component
public class OpenApiResponseGenerator {

    private final OpenApiParameterService parameterService;

    public OpenApiResponseGenerator(OpenApiParameterService parameterService) {
        this.parameterService = parameterService;
    }

    /**
     * Build OpenAPI {@link ApiResponses}
     *
     * @param apiContext current OpenAPI context
     * @param methodInfo Spring method handler
     * @return resulted API responses or empty
     */
    public Optional<ApiResponses> generate(OpenApiContext apiContext, MethodInfo methodInfo) {
        var responses = new ApiResponses();

        // gather ApiResponses annotation from class level
        processApiResponses(
            ReflectionUtils.getRepeatableAnnotations(methodInfo.getBeanType(),
                io.swagger.v3.oas.annotations.responses.ApiResponse.class),
            methodInfo.getProduces(),
            apiContext.getComponents(),
            responses,
            methodInfo.getJsonView());

        // gather ApiResponses annotation from method level
        processApiResponses(
            ReflectionUtils.getRepeatableAnnotations(methodInfo.getMethod(),
                io.swagger.v3.oas.annotations.responses.ApiResponse.class),
            methodInfo.getProduces(),
            apiContext.getComponents(),
            responses,
            methodInfo.getJsonView());

        // gather ApiResponses annotation from OpenAPI Operation annotation
        var operationAnno = methodInfo.getOperationAnnotation();
        if (operationAnno != null) {
            processApiResponses(Arrays.asList(operationAnno.responses()),
                methodInfo.getProduces(),
                apiContext.getComponents(),
                responses,
                methodInfo.getJsonView());
        }

        decorate(methodInfo, responses, apiContext.getComponents());

        if (responses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(responses);
    }

    private void processApiResponses(List<io.swagger.v3.oas.annotations.responses.ApiResponse> apiResponses,
            String[] produces,
            Components components,
            ApiResponses responses,
            JsonView jsonView) {
        if (CollectionUtils.isNotEmpty(apiResponses)) {
            for (var apiResponse : apiResponses) {
                processApiResponse(apiResponse, produces, components, responses, jsonView);
            }
        }
    }

    private void processApiResponse(io.swagger.v3.oas.annotations.responses.ApiResponse apiResponse,
            String[] produces,
            Components components,
            ApiResponses responses,
            JsonView jsonView) {

        var response = new ApiResponse();
        if (StringUtils.isNotBlank(apiResponse.ref())) {
            response.set$ref(apiResponse.ref());
            if (StringUtils.isNotBlank(apiResponse.responseCode())) {
                responses.addApiResponse(apiResponse.responseCode(), response);
            } else {
                responses._default(response);
            }
        } else {
            if (StringUtils.isNotBlank(apiResponse.description())) {
                response.setDescription(apiResponse.description());
            }
            if (apiResponse.extensions().length > 0) {
                AnnotationsUtils.getExtensions(apiResponse.extensions()).forEach(response::addExtension);
            }

            AnnotationsUtils.getContent(apiResponse.content(), new String[0], produces, null, components, jsonView)
                .ifPresent(response::content);
            AnnotationsUtils.getHeaders(apiResponse.headers(), jsonView).ifPresent(response::headers);
            if (StringUtils.isNotBlank(response.getDescription()) || response.getContent() != null || response
                .getHeaders() != null) {
                var links = AnnotationsUtils.getLinks(apiResponse.links());
                if (links.size() > 0) {
                    response.setLinks(links);
                }
                if (StringUtils.isNotBlank(apiResponse.responseCode())) {
                    responses.addApiResponse(apiResponse.responseCode(), response);
                } else {
                    responses._default(response);
                }
            }
        }
    }

    private void decorate(MethodInfo methodInfo, ApiResponses responses, Components components) {
        var returnType = methodInfo.getReturnType();
        boolean genericResponseCode = false;
        if (returnType instanceof ParameterizedType) {
            var rawType = ((ParameterizedType) returnType).getRawType();
            if (rawType == ResponseEntity.class || rawType == HttpEntity.class) {
                returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
                genericResponseCode = true;
            }
        }
        if (OpenApiUtils.isVoid(returnType)) {
            if (genericResponseCode) {
                if (responses.isEmpty()) {
                    responses._default(createDefaultApiResponse());
                }
            } else {
                var responseCode = Optional.ofNullable(methodInfo.getHttpStatus())
                    .map(HttpStatus::value)
                    .map(String::valueOf)
                    .orElse("200");
                if (responses.isEmpty()) {
                    responses.addApiResponse(responseCode, createDefaultApiResponse());
                } else if (responses.get(responseCode) == null) {
                    responses.addApiResponse(responseCode, createDefaultApiResponse());
                }
            }
        } else {
            var schema = parameterService.resolveSchema(returnType, components, methodInfo.getJsonView());
            if (schema != null) {
                var content = new Content();
                var mediaType = new io.swagger.v3.oas.models.media.MediaType().schema(schema);
                AnnotationsUtils.applyTypes(new String[0], methodInfo.getProduces(), content, mediaType);
                if (responses.isEmpty()) {
                    if (genericResponseCode) {
                        responses._default(createDefaultApiResponse().content(content));
                    } else {
                        var responseCode = Optional.ofNullable(methodInfo.getHttpStatus())
                            .map(HttpStatus::value)
                            .map(String::valueOf)
                            .orElse("200");
                        responses.addApiResponse(responseCode, createDefaultApiResponse().content(content));
                    }
                } else {
                    if (methodInfo.getHttpStatus() == null) {
                        var defaultResponse = responses.getDefault();
                        if (defaultResponse != null) {
                            extendApiResponse(defaultResponse, content, schema);
                        } else {
                            var apiResponse = responses.get("200");
                            if (apiResponse != null) {
                                extendApiResponse(apiResponse, content, schema);
                            }
                        }
                    } else {
                        var statusCode = String.valueOf(methodInfo.getHttpStatus().value());
                        var apiResponse = responses.get(statusCode);
                        if (apiResponse != null) {
                            extendApiResponse(apiResponse, content, schema);
                        } else {
                            responses.addApiResponse(statusCode, createDefaultApiResponse().content(content));
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void extendApiResponse(ApiResponse response, Content content, Schema schema) {
        if (StringUtils.isBlank(response.get$ref())) {
            if (response.getContent() == null) {
                response.setContent(content);
            } else {
                var currentContent = response.getContent();
                for (var entry : currentContent.entrySet()) {
                    if (entry.getValue().getSchema() == null) {
                        entry.getValue().setSchema(schema);
                    }
                }
            }
        }
    }

    private static ApiResponse createDefaultApiResponse() {
        return new ApiResponse().description("default response");
    }

}
