package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.rules.spring.openapi.OpenApiUtils;
import org.openl.rules.spring.openapi.SpringMvcHandlerMethodsHelper;
import org.openl.rules.spring.openapi.model.ControllerAdviceInfo;
import org.openl.rules.spring.openapi.model.ExceptionHandlerInfo;
import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.util.CollectionUtils;
import org.openl.util.StreamUtils;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.core.util.AnnotationsUtils;
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
public class OpenApiResponseService {

    private final OpenApiParameterService apiParameterService;
    private final RequestMappingHandlerAdapter mappingHandlerAdapter;
    private final Map<Class<?>, ExceptionHandlerMethodResolver> exHandlerAdviceCache;
    private final List<String> openLRestExceptionBasePackages;

    public OpenApiResponseService(OpenApiParameterService apiParameterService,
            RequestMappingHandlerAdapter mappingHandlerAdapter,
            SpringMvcHandlerMethodsHelper handlerMethodsHelper,
            @Qualifier("openLRestExceptionBasePackages") List<String> openLRestExceptionBasePackages) {
        this.apiParameterService = apiParameterService;
        this.mappingHandlerAdapter = mappingHandlerAdapter;
        this.openLRestExceptionBasePackages = openLRestExceptionBasePackages;

        this.exHandlerAdviceCache = handlerMethodsHelper.getControllerAdvices()
            .values()
            .stream()
            .map(Object::getClass)
            .collect(StreamUtils.toLinkedMap(Function.identity(), ExceptionHandlerMethodResolver::new));
    }

    /**
     * Generate OpenApi Responses for Spring Controller Advice bean
     *
     * @param apiContext current OpenApi context
     * @param controllerAdviceInfo controller advice to scan
     */
    public void generateResponses(OpenApiContext apiContext, ControllerAdviceInfo controllerAdviceInfo) {
        var beanType = controllerAdviceInfo.getControllerAdvice().getClass();

        var classApiResponses = new ApiResponses();
        processApiResponses(getApiResponses(
            beanType), new String[0], apiContext.getComponents(), classApiResponses, null);

        classApiResponses.forEach(controllerAdviceInfo.getApiResponses()::addApiResponse);

        for (Method method : ReflectionUtils.getAllDeclaredMethods(beanType)) {
            if (isExceptionHandlerMethod(method)) {
                var exHandlerInfoBuilder = ExceptionHandlerInfo.Builder.from(beanType, method);
                Set<String> possibleProduces = new HashSet<>();
                for (var converter : mappingHandlerAdapter.getMessageConverters()) {
                    converter.getSupportedMediaTypes((Class<?>) exHandlerInfoBuilder.getReturnType())
                        .stream()
                        .map(Object::toString)
                        .forEach(possibleProduces::add);
                }
                var exHandlerInfo = exHandlerInfoBuilder.produces(possibleProduces.toArray(new String[0])).build();
                var methodApiResponses = new ApiResponses();
                processApiResponses(getApiResponses(
                    method), exHandlerInfo.getProduces(), apiContext.getComponents(), methodApiResponses, null);

                decorateExceptionHandler(exHandlerInfo,
                    classApiResponses,
                    methodApiResponses,
                    apiContext.getComponents());
                methodApiResponses.forEach(controllerAdviceInfo.getApiResponses()::addApiResponse);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void decorateExceptionHandler(ExceptionHandlerInfo exHandlerInfo,
            ApiResponses classApiResponses,
            ApiResponses methodApiResponses,
            Components components) {
        Set<String> statusCodes = new HashSet<>();
        if (exHandlerInfo.getStatusCode() != null) {
            statusCodes.add(exHandlerInfo.getStatusCode());
        } else {
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            for (var baseException : exHandlerInfo.getHandledExceptions()) {
                scanner.addIncludeFilter(new AssignableTypeFilter(baseException));
            }
            for (var basePackage : openLRestExceptionBasePackages) {
                var candidates = scanner.findCandidateComponents(basePackage);
                for (var candidate : candidates) {
                    try {
                        var cl = (Class<? extends Throwable>) Class.forName(candidate.getBeanClassName());
                        if (OpenApiUtils.isHidden(cl)) {
                            continue;
                        }
                        Method bestMatchingMethod = exHandlerAdviceCache
                            .get(exHandlerInfo.getControllerAdviceBeanType())
                            .resolveMethodByExceptionType(cl);
                        if (exHandlerInfo.getMethod().equals(bestMatchingMethod)) {
                            var responseStatus = AnnotationUtils
                                .findAnnotation(Class.forName(candidate.getBeanClassName()), ResponseStatus.class);
                            if (responseStatus != null) {
                                statusCodes.add(String.valueOf(responseStatus.code().value()));
                            }
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            }
        }
        var schema = apiParameterService.resolveSchema(exHandlerInfo.getReturnType(), components, null);
        if (schema != null) {
            var content = createContent(schema, exHandlerInfo.getProduces());
            if (statusCodes.isEmpty()) {
                for (var apiResponse : methodApiResponses.values()) {
                    extendApiResponse(apiResponse, content, schema);
                }
            } else {
                for (var statusCode : statusCodes) {
                    var apiResponse = Optional.ofNullable(methodApiResponses.get(statusCode))
                        .or(() -> Optional.ofNullable(classApiResponses.get(statusCode)))
                        .orElse(null);
                    var httpStatus = Objects.requireNonNull(HttpStatus.resolve(Integer.parseInt(statusCode)));
                    if (apiResponse == null) {
                        methodApiResponses.addApiResponse(statusCode,
                            new ApiResponse().description(httpStatus.getReasonPhrase()).content(content));
                    } else {
                        extendApiResponse(apiResponse, content, schema);
                    }
                }
            }
        }
    }

    /**
     * Build OpenAPI {@link ApiResponses}
     *
     * @param apiContext current OpenAPI context
     * @param methodInfo Spring method handler
     * @return resulted API responses or empty
     */
    public Optional<ApiResponses> parse(OpenApiContext apiContext, MethodInfo methodInfo) {
        var responses = new ApiResponses();

        // gather ApiResponses annotation from class level
        processApiResponses(getApiResponses(methodInfo.getBeanType()),
            methodInfo.getProduces(),
            apiContext.getComponents(),
            responses,
            methodInfo.getJsonView());

        // gather ApiResponses annotation from method level
        processApiResponses(getApiResponses(methodInfo.getMethod()),
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
            var schema = apiParameterService.resolveSchema(returnType, components, methodInfo.getJsonView());
            if (schema != null) {
                var content = createContent(schema, methodInfo.getProduces());
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

    private static List<io.swagger.v3.oas.annotations.responses.ApiResponse> getApiResponses(Class<?> beanType) {
        return io.swagger.v3.core.util.ReflectionUtils.getRepeatableAnnotations(beanType,
            io.swagger.v3.oas.annotations.responses.ApiResponse.class);
    }

    private static List<io.swagger.v3.oas.annotations.responses.ApiResponse> getApiResponses(Method method) {
        return io.swagger.v3.core.util.ReflectionUtils.getRepeatableAnnotations(method,
            io.swagger.v3.oas.annotations.responses.ApiResponse.class);
    }

    @SuppressWarnings("rawtypes")
    private Content createContent(Schema schema, String[] produces) {
        var content = new Content();
        var mediaType = new io.swagger.v3.oas.models.media.MediaType().schema(schema);
        AnnotationsUtils.applyTypes(new String[0], produces, content, mediaType);
        return content;
    }

    private boolean isExceptionHandlerMethod(Method method) {
        return Modifier.isPublic(method.getModifiers()) && !OpenApiUtils.isHiddenApiMethod(method) && AnnotationUtils
            .findAnnotation(method, ExceptionHandler.class) != null;
    }

}
