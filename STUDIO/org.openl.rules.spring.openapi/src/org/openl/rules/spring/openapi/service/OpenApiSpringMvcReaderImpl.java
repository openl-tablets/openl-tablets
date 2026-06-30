package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import org.openl.rules.spring.openapi.OpenApiUtils;
import org.openl.rules.spring.openapi.SpringMvcHandlerMethodsHelper;
import org.openl.rules.spring.openapi.model.ControllerAdviceInfo;
import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;
import org.openl.util.StringUtils;

/**
 * Open API reader for Spring MVC
 *
 * @author Vladyslav Pikus
 */
@Component
public class OpenApiSpringMvcReaderImpl {

    private final SpringMvcHandlerMethodsHelper handlerMethodsHelper;
    private final OpenApiResponseService apiResponseService;
    private final OpenApiRequestServiceImpl apiRequestService;
    private final OpenApiParameterService apiParameterService;
    private final OpenApiSecurityService apiSecurityService;
    private final OpenApiPropertyResolver apiPropertyResolver;
    private final List<OpenApiOperationCustomizer> operationCustomizers;

    public OpenApiSpringMvcReaderImpl(SpringMvcHandlerMethodsHelper handlerMethodsHelper,
                                      OpenApiResponseService apiResponseService,
                                      OpenApiRequestServiceImpl apiRequestService,
                                      OpenApiParameterService apiParameterService,
                                      OpenApiSecurityService apiSecurityService,
                                      OpenApiPropertyResolver apiPropertyResolver,
                                      ObjectProvider<OpenApiOperationCustomizer> operationCustomizers) {
        this.handlerMethodsHelper = handlerMethodsHelper;
        this.apiResponseService = apiResponseService;
        this.apiRequestService = apiRequestService;
        this.apiParameterService = apiParameterService;
        this.apiSecurityService = apiSecurityService;
        this.apiPropertyResolver = apiPropertyResolver;
        this.operationCustomizers = operationCustomizers.orderedStream().toList();
    }

    /**
     * Read OpenAPI schema for controllers from list
     */
    public String read() {
        OpenApiContext openApiContext = new OpenApiContext();
        apiSecurityService.generateGlobalSecurity(openApiContext);
        var controllerAdviceInfos = handlerMethodsHelper.getControllerAdvices()
                .values()
                .stream()
                .map(ControllerAdviceInfo::new)
                .collect(Collectors.toList());
        handlerMethodsHelper.getHandlerMethods()
                .entrySet()
                .stream()
                .filter(e -> isRestControllers(e.getValue()))
                // Process handlers in a deterministic order. Several handlers can collapse into one operation (e.g.
                // the multipart/raw/archive variants of one path); the merged parameters, their order, and the _1/_2
                // suffixes of duplicate operation ids are all assigned in processing order, which Spring does not keep
                // stable between runs. Order by the request mapping (path, method, consumes) rather than the Java
                // method: a proxied controller (e.g. @Validated) reports a CGLIB method whose class name carries a
                // run-specific suffix, so its toString() is not stable, while the mapping is.
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .forEach(e -> visitHandlerMethod(openApiContext,
                        e.getKey(),
                        e.getValue(),
                        selectControllerAdvices(controllerAdviceInfos, e.getValue().getBeanType())));

        if (openApiContext.getOpenAPI().getTags() != null) {
            openApiContext.getOpenAPI().getTags().sort(Comparator.comparing(Tag::getName));
        }

        try {
            // Sort map entries (paths, component schemas, schema properties, ...) by key so the document keeps the
            // same order on every run regardless of the order in which controllers were registered or schemas were
            // first referenced. ORDER_MAP_ENTRIES_BY_KEYS is the feature; only the legacy mapper-level setter for it
            // is deprecated, so it is enabled on the writer instead.
            return Json.mapper()
                    .writer(standardPrettyPrinter())
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(openApiContext.getOpenAPI());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize the calculated OpenAPI schema", e);
        }
    }

    /**
     * Pretty printer that renders the document in the conventional JSON style: a two-space indent, each array element
     * on its own line, and a single space after (not before) the {@code :} separator.
     *
     * @return pretty printer for the OpenAPI document
     */
    private static DefaultPrettyPrinter standardPrettyPrinter() {
        var indenter = new DefaultIndenter("  ", "\n");
        return new DefaultPrettyPrinter()
                .withObjectIndenter(indenter)
                .withArrayIndenter(indenter)
                .withSeparators(Separators.createDefaultInstance()
                        .withObjectFieldValueSpacing(Separators.Spacing.AFTER));
    }

    private void visitHandlerMethod(OpenApiContext openApiContext,
                                    RequestMappingInfo mappingInfo,
                                    HandlerMethod method,
                                    List<ControllerAdviceInfo> controllerAdviceInfos) {
        if (mappingInfo.getPathPatternsCondition() == null) {
            return;
        }
        for (var controllerAdviceInfo : controllerAdviceInfos) {
            if (controllerAdviceInfo.getApiResponses().isEmpty()) {
                apiResponseService.generateResponses(openApiContext, controllerAdviceInfo);
            }
        }
        var methodInfoBuilder = MethodInfo.Builder.from(method, mappingInfo);
        for (var pathPattern : mappingInfo.getPathPatternsCondition().getPatterns()) {
            methodInfoBuilder.pathPattern(pathPattern.getPatternString());
            var requestMethods = mappingInfo.getMethodsCondition().getMethods();
            if (requestMethods.isEmpty()) {
                // if request method is not defined, it means that ALL HTTP methods are accepted
                requestMethods = Set.of(RequestMethod.values());
            }
            // Iterate in the fixed enum order. A handler matching several methods (e.g. an all-methods endpoint)
            // produces one operation per method, and the _1/_2 suffixes of duplicate operation ids are assigned in
            // iteration order; neither Set.of(...) nor the mapping's method set keeps a stable order between runs.
            requestMethods.stream().sorted().forEach(requestMethod -> {
                methodInfoBuilder.requestMethod(requestMethod);
                parseMethod(openApiContext, methodInfoBuilder.build(), controllerAdviceInfos);
            });
        }
    }

    private void parseMethod(OpenApiContext apiContext,
                             MethodInfo methodInfo,
                             List<ControllerAdviceInfo> controllerAdviceInfos) {
        final var operation = Optional.ofNullable(apiContext.getPaths().get(methodInfo.getPathPattern())).map(path -> {
            return switch (methodInfo.getRequestMethod()) {
                case GET -> path.getGet();
                case HEAD -> path.getHead();
                case POST -> path.getPost();
                case PUT -> path.getPut();
                case PATCH -> path.getPatch();
                case DELETE -> path.getDelete();
                case OPTIONS -> path.getOptions();
                case TRACE -> path.getTrace();
                default -> null;
            };
        }).orElseGet(Operation::new);

        if (isDeprecatedMethod(methodInfo.getMethod())) {
            operation.setDeprecated(true);
        }

        if (StringUtils.isBlank(operation.getOperationId())) {
            operation.setOperationId(getOperationId(apiContext, methodInfo.getMethod().getName()));
        }

        // parse OpenAPI Tags annotations
        parseMethodTags(apiContext, methodInfo.getHandler(), operation);

        // parse OpenAPI Operation annotation
        parseOperation(apiContext, methodInfo.getOperationAnnotation(), operation);

        // fill responses from Controller Advices
        for (var controllerAdviceInfo : controllerAdviceInfos) {
            if (controllerAdviceInfo.getApiResponses().isEmpty()) {
                continue;
            }
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }
            controllerAdviceInfo.getApiResponses().forEach(operation.getResponses()::addApiResponse);
        }

        // parse response body
        var generatedResponses = apiResponseService.generateResponses(apiContext, methodInfo);
        if (generatedResponses != null) {
            if (operation.getResponses() == null) {
                operation.setResponses(generatedResponses);
            } else {
                generatedResponses.forEach(operation.getResponses()::addApiResponse);
            }
        }

        if (operation.getResponses() != null && operation.getResponses().size() > 1 && operation.getResponses()
                .get(ApiResponses.DEFAULT) != null && operation.getResponses().get("200") == null) {
            var defaultResponse = operation.getResponses().remove(ApiResponses.DEFAULT);
            operation.getResponses().put("200", defaultResponse);
        }

        // split parameters
        List<ParameterInfo> parameters = new ArrayList<>();
        List<ParameterInfo> formParameters = new ArrayList<>();
        Set<Parameter> requestBodyParams = new HashSet<>();
        List<Parameter> allParamAnnos = new ArrayList<>();
        Optional.ofNullable(methodInfo.getOperationAnnotation())
                .map(io.swagger.v3.oas.annotations.Operation::parameters)
                .ifPresent(params -> allParamAnnos.addAll(Arrays.asList(params)));
        Optional.ofNullable(ReflectionUtils.getRepeatableAnnotations(methodInfo.getMethod(), Parameter.class))
                .ifPresent(allParamAnnos::addAll);

        ParameterInfo requestBodyParam = null;
        MethodParameter[] methodParameters = methodInfo.getHandler().getMethodParameters();
        int idx = 0;
        boolean formRequest = methodInfo.isFormRequest();
        for (MethodParameter methodParameter : methodParameters) {
            var parameterInfo = new ParameterInfo(methodInfo, methodParameter, idx++);
            if (parameterInfo.getParameter() != null && parameterInfo.getParameter().hidden()) {
                continue;
            }
            if (OpenApiUtils.isIgnorableType(parameterInfo.getType())) {
                continue;
            }
            var reqPart = parameterInfo.getParameterAnnotation(RequestPart.class);
            var reqParam = parameterInfo.getParameterAnnotation(RequestParam.class);
            var modelAttribute = apiRequestService.isModelAttribute(parameterInfo);
            if (modelAttribute || reqPart != null || (reqParam != null && (OpenApiUtils.isFile(parameterInfo
                    .getType()) || (formRequest && parameterInfo.getParameter() != null && parameterInfo.getParameter()
                    .in() == ParameterIn.DEFAULT)))) {
                formParameters.add(parameterInfo);
                // Skip parameter name resolution for @ModelAttribute - it will be expanded into fields
                if (!modelAttribute && parameterInfo.getParameter() == null) {
                    // Try to find Parameter annotation in other places
                    var paramName = Optional.ofNullable(reqPart)
                            .map(RequestPart::name)
                            .or(() -> Optional.of(reqParam).map(RequestParam::name))
                            .get();
                    allParamAnnos.stream()
                            .filter(p -> ParameterIn.DEFAULT == p.in() && paramName.equals(p.name()))
                            .findFirst()
                            .ifPresent(p -> {
                                requestBodyParams.add(p);
                                parameterInfo.setParameter(p);
                            });
                }
            } else if (apiRequestService.isRequestBody(parameterInfo)) {
                requestBodyParam = parameterInfo;
            } else {
                parameters.add(parameterInfo);
            }
        }
        // parse parameters; when several handler methods map to the same path and HTTP method (e.g. the
        // multipart, raw and archive variants of one endpoint) OpenAPI collapses them into a single operation,
        // so add each parameter only once to avoid duplicating the shared path and query parameters.
        addDistinctParameters(operation,
                apiParameterService.generateParameters(apiContext, methodInfo, parameters, requestBodyParams));

        // parse request body
        var sourceRequestBody = apiRequestService
                .generateRequestBody(apiContext, methodInfo, formParameters, requestBodyParam);
        if (sourceRequestBody != null) {
            if (operation.getRequestBody() != null) {
                apiRequestService.mergeRequestBody(sourceRequestBody, operation.getRequestBody());
            } else {
                operation.requestBody(sourceRequestBody);
            }
        }

        // apply optional cross-cutting customizers (e.g. response field projection 'fields' parameter)
        for (var customizer : operationCustomizers) {
            customizer.customize(methodInfo, operation);
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

    /**
     * Adds parameters to the operation, skipping any whose name and location are already present.
     * <p>
     * Several handler methods may map to the same path and HTTP method, for example the multipart, raw and
     * archive variants of one endpoint. They collapse into a single OpenAPI operation, so their shared path and
     * query parameters must be added only once. When the collapsed methods declare the same parameter with a
     * different default, the merged operation cannot advertise one variant's value, so that default is dropped.
     *
     * @param operation  target operation, possibly already holding parameters from a sibling handler method
     * @param parameters parameters produced for the current handler method
     */
    private static void addDistinctParameters(Operation operation,
                                              Collection<io.swagger.v3.oas.models.parameters.Parameter> parameters) {
        var present = new HashMap<String, io.swagger.v3.oas.models.parameters.Parameter>();
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(parameter -> present.put(parameterKey(parameter), parameter));
        }
        for (var parameter : parameters) {
            var existing = present.putIfAbsent(parameterKey(parameter), parameter);
            if (existing == null) {
                operation.addParametersItem(parameter);
            } else {
                dropConflictingDefault(existing, parameter);
            }
        }
    }

    /**
     * Reconciles a parameter shared by several handler methods that collapse into one operation. When the sibling
     * declarations disagree on the default value, no single default is correct for the merged operation, so the
     * default is removed rather than advertising one variant's value as if it applied to all.
     *
     * @param kept      parameter already added to the operation
     * @param duplicate same-named parameter from another collapsed handler method
     */
    private static void dropConflictingDefault(io.swagger.v3.oas.models.parameters.Parameter kept,
                                               io.swagger.v3.oas.models.parameters.Parameter duplicate) {
        var keptSchema = kept.getSchema();
        var duplicateSchema = duplicate.getSchema();
        if (keptSchema != null && duplicateSchema != null
                && !Objects.equals(keptSchema.getDefault(), duplicateSchema.getDefault())) {
            keptSchema.setDefault(null);
            // Clear the flag too, otherwise the serializer renders the absent default as an explicit "default": null.
            keptSchema.setDefaultSetFlag(false);
        }
    }

    private static String parameterKey(io.swagger.v3.oas.models.parameters.Parameter parameter) {
        // A $ref parameter carries no name or location, so key ref-only parameters by their reference to keep distinct
        // ones from colliding on "null:null".
        if (parameter.get$ref() != null) {
            return parameter.get$ref();
        }
        return parameter.getName() + ":" + parameter.getIn();
    }

    private void parseOperation(OpenApiContext apiContext,
                                io.swagger.v3.oas.annotations.Operation apiOperation,
                                Operation operation) {
        if (apiOperation == null) {
            return;
        }
        if (StringUtils.isNotBlank(apiOperation.summary())) {
            operation.setSummary(apiPropertyResolver.resolve(apiOperation.summary()));
        }
        if (StringUtils.isNotBlank(apiOperation.description())) {
            operation.setDescription(apiPropertyResolver.resolve(apiOperation.description()));
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
    }

    private void parseMethodTags(OpenApiContext openApiContext, HandlerMethod method, Operation operation) {
        var tags = Optional
                .ofNullable(ReflectionUtils.getRepeatableAnnotations(method.getMethod(),
                        io.swagger.v3.oas.annotations.tags.Tag.class))
                .orElseGet(Collections::emptyList);

        AnnotationsUtils.getTags(tags.toArray(io.swagger.v3.oas.annotations.tags.Tag[]::new), false)
                .stream()
                .flatMap(Collection::stream)
                .forEach(tagItem -> {
                    openApiContext.addTagsItem(tagItem);
                    if (operation.getTags() == null || !operation.getTags().contains(tagItem.getName())) {
                        operation.addTagsItem(tagItem.getName());
                    }
                });

        var typeTags = openApiContext.getClassTags(method.getBeanType());
        if (typeTags == null) {
            processTagsFromType(openApiContext, method.getBeanType());
            typeTags = openApiContext.getClassTags(method.getBeanType());
        }
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
                .ofNullable(AnnotationUtils.findAnnotation(beanType, OpenAPIDefinition.class))
                .map(OpenAPIDefinition::tags)
                .ifPresent(anno -> tags.add(Stream.of(anno)));
        Optional
                .ofNullable(
                        ReflectionUtils.getRepeatableAnnotations(beanType, io.swagger.v3.oas.annotations.tags.Tag.class))
                .ifPresent(anno -> tags.add(anno.stream()));

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
            operationIdToFind = "%s_%d".formatted(operationId, ++counter);
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
                        path.getPatch(),
                        path.getTrace())
                .filter(Objects::nonNull)
                .map(Operation::getOperationId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private static boolean isRestControllers(HandlerMethod method) {
        return isRestController(method.getBeanType()) && hasResponseBody(method);
    }

    private static boolean isRestController(Class<?> cl) {
        return AnnotationUtils.findAnnotation(cl, RestController.class) != null || (AnnotationUtils.findAnnotation(cl,
                Controller.class) != null && AnnotationUtils.findAnnotation(cl, ResponseBody.class) != null);
    }

    private static boolean hasResponseBody(HandlerMethod method) {
        return method.hasMethodAnnotation(
                ResponseBody.class) || AnnotationUtils.findAnnotation(method.getBeanType(), ResponseBody.class) != null;
    }

    private static boolean isDeprecatedMethod(Method method) {
        return AnnotationUtils.findAnnotation(method, Deprecated.class) != null || AnnotationUtils
                .findAnnotation(method.getDeclaringClass(), Deprecated.class) != null;
    }

    private static List<ControllerAdviceInfo> selectControllerAdvices(
            Collection<ControllerAdviceInfo> controllerAdvices,
            Class<?> beanType) {
        return controllerAdvices.stream()
                .filter(controllerAdvice -> createHandlerTypePredicate(controllerAdvice).test(beanType))
                .collect(Collectors.toList());
    }

    private static HandlerTypePredicate createHandlerTypePredicate(Object bean) {
        var beanType = ClassUtils.getUserClass(bean);
        var controllerAdvice = AnnotatedElementUtils.findMergedAnnotation(beanType, ControllerAdvice.class);
        if (controllerAdvice == null) {
            return HandlerTypePredicate.forAnyHandlerType();
        }
        return HandlerTypePredicate.builder()
                .basePackage(controllerAdvice.basePackages())
                .basePackageClass(controllerAdvice.basePackageClasses())
                .assignableType(controllerAdvice.assignableTypes())
                .annotation(controllerAdvice.annotations())
                .build();
    }
}
