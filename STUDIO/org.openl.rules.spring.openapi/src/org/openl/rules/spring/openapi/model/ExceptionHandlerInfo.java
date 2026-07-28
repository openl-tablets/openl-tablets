package org.openl.rules.spring.openapi.model;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.openl.rules.spring.openapi.OpenApiUtils;

/**
 * Information holder for methods from beans annotated with
 * {@link org.springframework.web.bind.annotation.ControllerAdvice}
 */
public class ExceptionHandlerInfo {

    @Getter
    private final Class<?> controllerAdviceBeanType;
    @Getter
    private final Method method;
    @Getter
    private final String statusCode;
    @Getter
    private final String[] produces;
    @Getter
    private final Type returnType;
    @Getter
    private final Type wrapperReturnType;
    @Getter
    private final Class<? extends Throwable>[] handledExceptions;

    private ExceptionHandlerInfo(Builder from) {
        this.controllerAdviceBeanType = from.controllerAdviceBeanType;
        this.method = from.method;
        this.produces = from.produces;
        this.returnType = from.returnType;
        this.wrapperReturnType = from.wrapperReturnType;
        this.handledExceptions = from.handledExceptions;
        this.statusCode = from.statusCode;
    }

    public static class Builder {

        private final Class<?> controllerAdviceBeanType;
        private final Method method;
        private final String statusCode;
        private final Class<? extends Throwable>[] handledExceptions;
        @Getter
        private Type returnType;
        private Type wrapperReturnType;
        private String[] produces;

        private Builder(Class<?> controllerAdviceBeanType, Method method) {
            this.controllerAdviceBeanType = controllerAdviceBeanType;
            this.method = method;
            this.statusCode = Optional.ofNullable(AnnotationUtils.findAnnotation(method, ResponseStatus.class))
                    .map(ResponseStatus::value)
                    .map(HttpStatus::value)
                    .map(String::valueOf)
                    .orElse(null);
            this.returnType = OpenApiUtils.getReturnType(method);
            if (returnType instanceof ParameterizedType type) {
                var rawType = type.getRawType();
                if (rawType == ResponseEntity.class || rawType == HttpEntity.class) {
                    wrapperReturnType = returnType;
                    returnType = type.getActualTypeArguments()[0];
                }
            }
            this.handledExceptions = Objects
                    .requireNonNull(AnnotationUtils.findAnnotation(method, ExceptionHandler.class))
                    .value();
        }

        public static Builder from(Class<?> controllerAdviceBeanType, Method method) {
            return new Builder(controllerAdviceBeanType, method);
        }

        public Builder produces(String[] produces) {
            this.produces = produces;
            return this;
        }

        public ExceptionHandlerInfo build() {
            return new ExceptionHandlerInfo(this);
        }
    }

}
