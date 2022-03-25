package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.rules.spring.openapi.OpenApiUtils;
import org.openl.rules.spring.openapi.SpringMvcHandlerMethodsHelper;
import org.openl.rules.spring.openapi.model.ControllerAdviceInfo;
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
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Open API reader for Spring MVC
 *
 * @author Vladyslav Pikus
 */
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
        var controllerAdviceInfos = handlerMethodsHelper.getControllerAdvices()
            .values()
            .stream()
            .map(ControllerAdviceInfo::new)
            .collect(Collectors.toList());
        handlerMethodsHelper.getHandlerMethods()
            .entrySet()
            .stream()
            .filter(e -> isRestControllers(e.getValue(), controllers))
            .filter(e -> !OpenApiUtils.isHiddenApiMethod(e.getValue().getMethod()))
            .forEach(e -> visitHandlerMethod(openApiContext,
                e.getKey(),
                e.getValue(),
                selectControllerAdvices(controllerAdviceInfos, e.getValue().getBeanType())));
    }

    private void visitHandlerMethod(OpenApiContext openApiContext,
            RequestMappingInfo mappingInfo,
            HandlerMethod method,
            List<ControllerAdviceInfo> controllerAdviceInfos) {
        if (mappingInfo.getPatternsCondition() == null) {
            return;
        }
        for (var controllerAdviceInfo : controllerAdviceInfos) {
            if (controllerAdviceInfo.getApiResponses().isEmpty()) {
                apiResponseService.generateResponses(openApiContext, controllerAdviceInfo);
            }
        }
        var methodInfoBuilder = MethodInfo.Builder.from(method, mappingInfo);
        for (String pathPattern : mappingInfo.getPatternsCondition().getPatterns()) {
            methodInfoBuilder.pathPattern(pathPattern);
            for (RequestMethod requestMethod : mappingInfo.getMethodsCondition().getMethods()) {
                methodInfoBuilder.requestMethod(requestMethod);
                var operation = parseMethod(openApiContext, methodInfoBuilder.build());
                for (var controllerAdviceInfo : controllerAdviceInfos) {
                    controllerAdviceInfo.getApiResponses().forEach(operation.getResponses()::addApiResponse);
                }
            }
        }
        if (openApiContext.getOpenAPI().getTags() != null) {
            openApiContext.getOpenAPI().getTags().sort(Comparator.comparing(Tag::getName));
        }
    }

    private Operation parseMethod(OpenApiContext apiContext, MethodInfo methodInfo) {
        final var operation = new Operation();

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
        List<ParameterInfo> formParameters = new ArrayList<>();
        ParameterInfo requestBodyParam = null;
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
            if (parameterInfo.hasAnnotation(RequestPart.class) || (OpenApiUtils
                .isFile(parameterInfo.getType()) && parameterInfo.hasAnnotation(RequestParam.class))) {
                formParameters.add(parameterInfo);
            } else if (parameterInfo.hasAnnotation(org.springframework.web.bind.annotation.RequestBody.class)) {
                requestBodyParam = parameterInfo;
            } else {
                parameters.add(parameterInfo);
            }
        }
        // parse parameters
        apiParameterService.parse(apiContext, methodInfo, parameters).forEach(operation::addParametersItem);

        // parse request body
        apiRequestService.parse(apiContext, methodInfo, formParameters, requestBodyParam)
            .ifPresent(operation::requestBody);

        // register parsed operation method
        PathItem pathItem;
        if (apiContext.getPaths().containsKey(methodInfo.getPathPattern())) {
            pathItem = apiContext.getPaths().get(methodInfo.getPathPattern());
        } else {
            pathItem = new PathItem();
            apiContext.getPaths().addPathItem(methodInfo.getPathPattern(), pathItem);
        }
        pathItem.operation(PathItem.HttpMethod.valueOf(methodInfo.getRequestMethod().name()), operation);

        return operation;
    }

    private void parseOperation(OpenApiContext apiContext,
            io.swagger.v3.oas.annotations.Operation apiOperation,
            Operation operation) {
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

    private static boolean isDeprecatedMethod(Method method) {
        return AnnotationUtils.findAnnotation(method, Deprecated.class) != null || AnnotationUtils
            .findAnnotation(method.getDeclaringClass(), Deprecated.class) != null;
    }

    private static List<ControllerAdviceInfo> selectControllerAdvices(
            Collection<ControllerAdviceInfo> controllerAdvices,
            Class<?> beanType) {
        return controllerAdvices.stream()
            .filter(controllerAdvice -> new ControllerAdviceBean(controllerAdvice).isApplicableToBeanType(beanType))
            .collect(Collectors.toList());
    }
}
