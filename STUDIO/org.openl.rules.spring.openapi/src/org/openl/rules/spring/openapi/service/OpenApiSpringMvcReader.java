package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.rules.spring.openapi.OpenApiUtils;
import org.openl.rules.spring.openapi.SpringMvcHandlerMethodsHelper;
import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;
import org.openl.util.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.tags.Tag;

@Component
public class OpenApiSpringMvcReader {

    private final SpringMvcHandlerMethodsHelper handlerMethodsHelper;
    private final OpenApiResponseService apiResponseService;
    private final OpenApiRequestService apiRequestService;
    private final OpenApiParameterService apiParameterService;

    public OpenApiSpringMvcReader(SpringMvcHandlerMethodsHelper handlerMethodsHelper,
            OpenApiResponseService apiResponseService,
            OpenApiRequestService apiRequestService,
            OpenApiParameterService apiParameterService) {
        this.handlerMethodsHelper = handlerMethodsHelper;
        this.apiResponseService = apiResponseService;
        this.apiRequestService = apiRequestService;
        this.apiParameterService = apiParameterService;
    }

    public void read(OpenApiContext openApiContext, Map<String, Class<?>> controllers) {
        handlerMethodsHelper.getHandlerMethods()
            .entrySet()
            .stream()
            .filter(e -> isRestControllers(e.getValue(), controllers))
            .filter(e -> !isHiddenApiMethod(e.getValue().getMethod()))
            .forEach(e -> visitHandlerMethod(openApiContext, e.getKey(), e.getValue()));
    }

    private void visitHandlerMethod(OpenApiContext openApiContext,
            RequestMappingInfo mappingInfo,
            HandlerMethod method) {
        var methodInfoBuilder = MethodInfo.Builder.from(method, mappingInfo);
        for (String pathPattern : mappingInfo.getPatternsCondition().getPatterns()) {
            methodInfoBuilder.pathPattern(pathPattern);
            for (RequestMethod requestMethod : mappingInfo.getMethodsCondition().getMethods()) {
                methodInfoBuilder.requestMethod(requestMethod);
                parseMethod(openApiContext, methodInfoBuilder.build());
            }
        }
        if (openApiContext.getOpenAPI().getTags() != null) {
            openApiContext.getOpenAPI().getTags().sort(Comparator.comparing(Tag::getName));
        }
    }

    private void parseMethod(OpenApiContext apiContext, MethodInfo methodInfo) {
        final var operation = new Operation();

        JsonView jsonViewAnnotationForRequestBody = null;
        if (!methodInfo.ignoreJsonView()) {
            jsonViewAnnotationForRequestBody = (JsonView) Arrays
                .stream(ReflectionUtils.getParameterAnnotations(methodInfo.getMethod()))
                .filter(arr -> Arrays.stream(arr)
                    .anyMatch(annotation -> annotation.annotationType()
                        .equals(io.swagger.v3.oas.annotations.parameters.RequestBody.class)))
                .flatMap(Arrays::stream)
                .filter(annotation -> annotation.annotationType().equals(JsonView.class))
                .reduce((a, b) -> null)
                .orElse(methodInfo.getJsonView());
        }

        if (isDeprecatedMethod(methodInfo.getMethod())) {
            operation.setDeprecated(true);
        }

        if (StringUtils.isBlank(operation.getOperationId())) {
            operation.setOperationId(getOperationId(apiContext, methodInfo.getMethod().getName()));
        }

        // parse OpenAPI Tags annotations
        parseMethodTags(apiContext, methodInfo.getHandler(), operation);

        // parse OpenAPI RequestBody annotation
        var apiRequestBody = ReflectionUtils.getAnnotation(methodInfo.getMethod(),
            io.swagger.v3.oas.annotations.parameters.RequestBody.class);
        if (apiRequestBody != null && operation.getRequestBody() == null) {
            parseRequestBody(apiContext, apiRequestBody, methodInfo.getConsumes(), methodInfo.getJsonView())
                .ifPresent(operation::setRequestBody);
        }

        // parse OpenAPI Operation annotation
        parseOperation(apiContext,
            methodInfo.getOperationAnnotation(),
            operation,
            methodInfo.getConsumes(),
            methodInfo.getProduces(),
            methodInfo.getJsonView());

        // parse response body
        apiResponseService.parse(apiContext, methodInfo).ifPresent(responses -> {
            if (operation.getResponses() == null) {
                operation.setResponses(responses);
            } else {
                responses.forEach(operation.getResponses()::addApiResponse);
            }
        });

        // split parameters
        List<ParameterInfo> parameters = new ArrayList<>();
        List<ParameterInfo> requestBodyParameters = new ArrayList<>();
        MethodParameter[] methodParameters = methodInfo.getHandler().getMethodParameters();
        int idx = 0;
        for (MethodParameter methodParameter : methodParameters) {
            var parameterInfo = new ParameterInfo(methodInfo, methodParameter, idx++);
            if (parameterInfo.getParameter() != null && parameterInfo.getParameter().hidden()) {
                continue;
            }
            if (OpenApiUtils.isIgnorableType(parameterInfo.getType())) {
                continue;
            }
            if (parameterInfo.hasAnnotation(RequestPart.class) || parameterInfo
                .hasAnnotation(org.springframework.web.bind.annotation.RequestBody.class) || (OpenApiUtils
                    .isFile(parameterInfo.getType()) && parameterInfo.hasAnnotation(RequestParam.class))) {
                requestBodyParameters.add(parameterInfo);
            } else {
                parameters.add(parameterInfo);
            }
        }
        // parse parameters
        apiParameterService.parse(apiContext, methodInfo, parameters).forEach(operation::addParametersItem);

        apiRequestService.parse(apiContext, methodInfo, requestBodyParameters);

        // parse OpenAPI Parameters from method parameters
        var resolvedParameters = resolveMethodParameters(apiContext,
            methodInfo.getHandler(),
            methodInfo.getConsumes(),
            methodInfo.getJsonView(),
            jsonViewAnnotationForRequestBody);

        // apply request body from method parameters
        if (operation.getRequestBody() == null) {
            operation.setRequestBody(resolvedParameters.requestBody);
        } else if (resolvedParameters.requestBody != null) {
            // just copy schema for request body if it's missing in original definition
            var currentContent = operation.getRequestBody().getContent();
            for (var entry : resolvedParameters.requestBody.getContent().entrySet()) {
                var currentMediaType = currentContent.get(entry.getKey());
                if (currentMediaType == null) {
                    currentContent.addMediaType(entry.getKey(), entry.getValue());
                } else if (currentMediaType.getSchema() == null) {
                    currentMediaType.setSchema(entry.getValue().getSchema());
                }
            }
        }

        // register parsed operation method
        PathItem pathItem;
        if (apiContext.getPaths().containsKey(methodInfo.getPathPattern())) {
            pathItem = apiContext.getPaths().get(methodInfo.getPathPattern());
        } else {
            pathItem = new PathItem();
            apiContext.getPaths().addPathItem(methodInfo.getPathPattern(), pathItem);
        }
        pathItem.operation(PathItem.HttpMethod.valueOf(methodInfo.getRequestMethod().name()), operation);
    }

    private Optional<RequestBody> parseRequestBody(OpenApiContext apiContext,
            io.swagger.v3.oas.annotations.parameters.RequestBody apiRequestBody,
            String[] consumes,
            JsonView jsonViewAnnotation) {
        if (apiRequestBody == null) {
            return Optional.empty();
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
            return Optional.empty();
        }
        AnnotationsUtils
            .getContent(apiRequestBody
                .content(), new String[0], consumes, null, apiContext.getComponents(), jsonViewAnnotation)
            .ifPresent(requestBody::setContent);
        return Optional.of(requestBody);
    }

    private ResolvedParameters resolveMethodParameters(OpenApiContext apiContext,
            HandlerMethod method,
            String[] consumes,
            JsonView jsonViewAnnotation,
            JsonView jsonViewAnnotationForRequestBody) {
        var resolvedParameters = new ResolvedParameters();
        ObjectSchema objectSchema = null;
        MethodParameter[] methodParameters = method.getMethodParameters();
        for (int i = 0; i < methodParameters.length; i++) {
            MethodParameter methodParameter = methodParameters[i];
            var apiParameter = methodParameter.getParameterAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            var requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
            var requestPart = methodParameter.getParameterAnnotation(RequestPart.class);
            if (apiParameter != null && apiParameter.hidden()) {
                // skip hidden parameters
                continue;
            }
            var parameterType = ParameterProcessor.getParameterType(apiParameter, true);
            if (parameterType == null) {
                parameterType = OpenApiUtils.getType(methodParameter);
            }
            if (OpenApiUtils.isIgnorableType(parameterType)) {
                continue;
            }
            if ((OpenApiUtils.isFile(parameterType) && requestParam != null) || requestPart != null) {
                var resolvedSchema = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(
                        new AnnotatedType(parameterType).resolveAsRef(true).jsonViewAnnotation(jsonViewAnnotation));
                if (resolvedSchema.schema != null) {
                    if (objectSchema == null) {
                        objectSchema = new ObjectSchema();
                    }
                    String name = null;
                    boolean required = false;
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
                    if (apiParameter != null) {
                        if (StringUtils.isNotBlank(apiParameter.name())) {
                            name = apiParameter.name();
                        }
                        required = apiParameter.required();
                    }
                    if (name == null) {
                        name = "arg" + i;
                    }
                    if (required) {
                        objectSchema.addRequiredItem(name);
                    }
                    objectSchema.addProperties(name, resolvedSchema.schema);

                    if (resolvedSchema.referencedSchemas != null) {
                        resolvedSchema.referencedSchemas
                            .forEach((key, schema) -> apiContext.getComponents().addSchemas(key, schema));
                    }
                }
                continue;
            }
            var requestBodyAnno = methodParameter
                .getParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class);

            if (requestBodyAnno != null) {
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
                var parameter = ParameterProcessor.applyAnnotations(null,
                    parameterType,
                    Arrays.asList(methodParameter.getParameterAnnotations()),
                    apiContext.getComponents(),
                    new String[0],
                    consumes,
                    jsonViewAnnotationForRequestBody);
                if (parameter.getContent() != null && !parameter.getContent().isEmpty()) {
                    requestBody.setContent(parameter.getContent());
                } else if (parameter.getSchema() != null) {
                    var content = new Content();
                    if (consumes.length == 0) {
                        var mediaTypeObject = new MediaType();
                        mediaTypeObject.setSchema(parameter.getSchema());
                        content.addMediaType("*/*", mediaTypeObject);
                    } else {
                        Stream.of(consumes).forEach(consume -> {
                            var mediaTypeObject = new MediaType();
                            mediaTypeObject.setSchema(parameter.getSchema());
                            content.addMediaType(consume, mediaTypeObject);
                        });
                    }
                    requestBody.setContent(content);
                }
                resolvedParameters.requestBody = requestBody;
                continue;
            }
        }
        if (objectSchema != null) {
            var content = new Content();
            if (consumes.length == 0) {
                var mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
                mediaTypeObject.setSchema(objectSchema);
                content.addMediaType("*/*", mediaTypeObject);
            } else {
                for (String consume : consumes) {
                    var mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
                    mediaTypeObject.setSchema(objectSchema);
                    content.addMediaType(consume, mediaTypeObject);
                }
            }
            resolvedParameters.requestBody = new RequestBody().content(content);
        }
        return resolvedParameters;
    }

    private void applyValidationAnnotations(MethodParameter methodParameter, Parameter parameter) {
        var min = methodParameter.getParameterAnnotation(Min.class);
        if (min != null) {
            parameter.getSchema().setMinimum(BigDecimal.valueOf(min.value()));
        }
        var max = methodParameter.getParameterAnnotation(Max.class);
        if (max != null) {
            parameter.getSchema().setMaximum(BigDecimal.valueOf(max.value()));
        }
        var decimalMin = methodParameter.getParameterAnnotation(DecimalMin.class);
        if (decimalMin != null) {
            parameter.getSchema().setMinimum(new BigDecimal(decimalMin.value()));
        }
        var decimalMax = methodParameter.getParameterAnnotation(DecimalMax.class);
        if (decimalMax != null) {
            parameter.getSchema().setMaximum(new BigDecimal(decimalMax.value()));
        }
        var pattern = methodParameter.getParameterAnnotation(Pattern.class);
        if (pattern != null) {
            parameter.getSchema().setPattern(pattern.regexp());
        }
    }

    private static final class ResolvedParameters {
        RequestBody requestBody;
    }

    private void parseOperation(OpenApiContext apiContext,
            io.swagger.v3.oas.annotations.Operation apiOperation,
            Operation operation,
            String[] consumes,
            String[] produces,
            JsonView jsonViewAnnotation) {
        if (apiOperation == null) {
            return;
        }
        if (StringUtils.isNotBlank(apiOperation.summary())) {
            operation.setSummary(apiOperation.summary());
        }
        if (StringUtils.isNotBlank(apiOperation.description())) {
            operation.setDescription(apiOperation.description());
        }
        if (StringUtils.isNotBlank(apiOperation.operationId())) {
            operation.setOperationId(getOperationId(apiContext, apiOperation.operationId()));
        }
        if (apiOperation.deprecated()) {
            operation.setDeprecated(Boolean.TRUE);
        }
        if (apiOperation.tags() != null) {
            Stream.of(apiOperation.tags())
                .filter(tag -> operation.getTags() == null || !operation.getTags().contains(tag))
                .forEach(operation::addTagsItem);
        }

        if (apiOperation.requestBody() != null && operation.getRequestBody() == null) {
            parseRequestBody(apiContext, apiOperation.requestBody(), consumes, jsonViewAnnotation)
                .ifPresent(operation::setRequestBody);
        }
    }

    // TODO: Parse Callback, ExternalDocumentation, SecurityRequirement, Server
    private void parseMethodTags(OpenApiContext openApiContext, HandlerMethod method, Operation operation) {
        var typeTags = openApiContext.getClassTags(method.getBeanType());
        if (typeTags == null) {
            processTagsFromType(openApiContext, method.getBeanType());
            typeTags = openApiContext.getClassTags(method.getBeanType());
        }
        List<Stream<io.swagger.v3.oas.annotations.tags.Tag>> tags = new ArrayList<>();
        Optional
            .ofNullable(
                AnnotationUtils.findAnnotation(method.getMethod(), io.swagger.v3.oas.annotations.tags.Tags.class))
            .ifPresent(anno -> tags.add(Stream.of(anno.value())));
        Optional
            .ofNullable(
                AnnotationUtils.findAnnotation(method.getMethod(), io.swagger.v3.oas.annotations.tags.Tag.class))
            .ifPresent(anno -> tags.add(Stream.of(anno)));
        AnnotationsUtils
            .getTags(tags.stream().flatMap(Function.identity()).toArray(io.swagger.v3.oas.annotations.tags.Tag[]::new),
                false)
            .stream()
            .flatMap(Collection::stream)
            .forEach(tagItem -> {
                openApiContext.addTagsItem(tagItem);
                if (operation.getTags() == null || !operation.getTags().contains(tagItem.getName())) {
                    operation.addTagsItem(tagItem.getName());
                }
            });

        Optional.ofNullable(typeTags)
            .map(Map::keySet)
            .stream()
            .flatMap(Collection::stream)
            .filter(tag -> operation.getTags() == null || !operation.getTags().contains(tag))
            .forEach(operation::addTagsItem);
    }

    private void processTagsFromType(OpenApiContext openApiContext, Class<?> beanType) {
        List<Stream<io.swagger.v3.oas.annotations.tags.Tag>> tags = new ArrayList<>();
        Optional
            .ofNullable(AnnotationUtils.findAnnotation(beanType, io.swagger.v3.oas.annotations.OpenAPIDefinition.class))
            .map(io.swagger.v3.oas.annotations.OpenAPIDefinition::tags)
            .ifPresent(anno -> tags.add(Stream.of(anno)));
        Optional.ofNullable(AnnotationUtils.findAnnotation(beanType, io.swagger.v3.oas.annotations.tags.Tags.class))
            .ifPresent(anno -> tags.add(Stream.of(anno.value())));
        Optional.ofNullable(AnnotationUtils.findAnnotation(beanType, io.swagger.v3.oas.annotations.tags.Tag.class))
            .ifPresent(anno -> tags.add(Stream.of(anno)));

        AnnotationsUtils
            .getTags(tags.stream().flatMap(Function.identity()).toArray(io.swagger.v3.oas.annotations.tags.Tag[]::new),
                false)
            .ifPresent(res -> openApiContext.addClassTags(beanType, res));
    }

    protected String getOperationId(OpenApiContext apiContext, String operationId) {
        boolean operationIdUsed = existOperationId(apiContext, operationId);
        String operationIdToFind = null;
        int counter = 0;
        while (operationIdUsed) {
            operationIdToFind = String.format("%s_%d", operationId, ++counter);
            operationIdUsed = existOperationId(apiContext, operationIdToFind);
        }
        if (operationIdToFind != null) {
            operationId = operationIdToFind;
        }
        return operationId;
    }

    private boolean existOperationId(OpenApiContext apiContext, String operationId) {
        if (apiContext.getPaths() == null) {
            return false;
        }
        for (var path : apiContext.getPaths().values()) {
            var pathOperationIds = extractOperationIdFromPathItem(path);
            if (pathOperationIds.contains(operationId)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractOperationIdFromPathItem(PathItem path) {
        return Stream
            .of(path.getGet(),
                path.getPost(),
                path.getPut(),
                path.getDelete(),
                path.getOptions(),
                path.getHead(),
                path.getPatch())
            .filter(Objects::nonNull)
            .map(Operation::getOperationId)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    private static boolean isRestControllers(HandlerMethod method, Map<String, Class<?>> controllers) {
        return isRestController(method.getBeanType()) && controllers.get(method.getBean().toString()) == method
            .getBeanType() && hasResponseBody(method);
    }

    private static boolean isRestController(Class<?> cl) {
        return AnnotationUtils.findAnnotation(cl, RestController.class) != null || (AnnotationUtils.findAnnotation(cl,
            Controller.class) != null && AnnotationUtils.findAnnotation(cl, ResponseBody.class) != null);
    }

    private static boolean hasResponseBody(HandlerMethod method) {
        return method.hasMethodAnnotation(
            ResponseBody.class) || AnnotationUtils.findAnnotation(method.getBeanType(), ResponseBody.class) != null;
    }

    private static boolean isHiddenApiMethod(Method method) {
        var anno = AnnotationUtils.findAnnotation(method, io.swagger.v3.oas.annotations.Operation.class);
        return anno != null && anno.hidden() || AnnotationUtils.findAnnotation(method,
            io.swagger.v3.oas.annotations.Hidden.class) != null || AnnotationUtils
                .findAnnotation(method.getDeclaringClass(), io.swagger.v3.oas.annotations.Hidden.class) != null;
    }

    private static boolean isDeprecatedMethod(Method method) {
        return AnnotationUtils.findAnnotation(method, Deprecated.class) != null || AnnotationUtils
            .findAnnotation(method.getDeclaringClass(), Deprecated.class) != null;
    }
}
