package org.openl.rules.spring.openapi.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.openl.rules.spring.openapi.OpenApiUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.annotations.Operation;

public class MethodInfo {

    private static final String[] DEFAULT_CONSUMES = new String[] { MediaType.ALL_VALUE };

    private final HandlerMethod handler;
    private final RequestMethod requestMethod;
    private final String pathPattern;
    private final String[] produces;
    private final String[] consumes;
    private final Operation operationAnnotation;
    private final JsonView jsonView;
    private final HttpStatus httpStatus;

    private MethodInfo(Builder from) {
        this.handler = from.handler;
        this.requestMethod = from.requestMethod;
        this.pathPattern = from.pathPattern;
        this.produces = from.produces;
        this.consumes = from.consumes;
        this.operationAnnotation = from.operationAnnotation;
        this.jsonView = from.jsonView;
        this.httpStatus = from.httpStatus;
    }

    public HandlerMethod getHandler() {
        return handler;
    }

    public io.swagger.v3.oas.annotations.Operation getOperationAnnotation() {
        return operationAnnotation;
    }

    public JsonView getJsonView() {
        return jsonView;
    }

    public String[] getProduces() {
        return produces;
    }

    public String[] getConsumes() {
        return consumes;
    }

    public Class<?> getBeanType() {
        return handler.getBeanType();
    }

    public Method getMethod() {
        return handler.getMethod();
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public boolean ignoreJsonView() {
        return operationAnnotation != null && operationAnnotation.ignoreJsonView();
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public Type getReturnType() {
        return OpenApiUtils.getType(handler.getReturnType());
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static class Builder {

        private static final Function<Set<MediaType>, String[]> MEDIA_TYPES_TO_ARRAY = set -> set.stream()
            .map(Object::toString)
            .toArray(String[]::new);

        private final HandlerMethod handler;
        private final String[] produces;
        private final String[] consumes;
        private final Operation operationAnnotation;
        private final JsonView jsonView;
        private final HttpStatus httpStatus;

        private RequestMethod requestMethod;
        private String pathPattern;

        private Builder(HandlerMethod handler, String[] produces, String[] consumes) {
            this.handler = handler;
            this.produces = produces;
            this.consumes = consumes.length == 0 ? DEFAULT_CONSUMES : consumes;

            this.operationAnnotation = ReflectionUtils.getAnnotation(handler.getMethod(), Operation.class);
            if (operationAnnotation == null || !operationAnnotation.ignoreJsonView()) {
                jsonView = ReflectionUtils.getAnnotation(handler.getMethod(), JsonView.class);
            } else {
                jsonView = null;
            }

            httpStatus = Optional.ofNullable(handler.getMethodAnnotation(ResponseStatus.class))
                .map(ResponseStatus::value)
                .orElse(null);
        }

        public static Builder from(HandlerMethod handler, RequestMappingInfo mappingInfo) {
            return new Builder(handler,
                MEDIA_TYPES_TO_ARRAY.apply(mappingInfo.getProducesCondition().getProducibleMediaTypes()),
                MEDIA_TYPES_TO_ARRAY.apply(mappingInfo.getConsumesCondition().getConsumableMediaTypes()));
        }

        public Builder requestMethod(RequestMethod requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        public Builder pathPattern(String pathPattern) {
            this.pathPattern = pathPattern;
            return this;
        }

        public MethodInfo build() {
            return new MethodInfo(this);
        }

    }
}
