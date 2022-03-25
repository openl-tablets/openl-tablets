package org.openl.rules.spring.openapi.model;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import org.openl.rules.spring.openapi.OpenApiUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ExceptionHandlerInfo {

    private final Class<?> controllerAdviceBeanType;
    private final Method method;
    private final String statusCode;
    private final String[] produces;
    private final Type returnType;
    private final Type wrapperReturnType;
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

    public Method getMethod() {
        return method;
    }

    public String[] getProduces() {
        return produces;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Type getWrapperReturnType() {
        return wrapperReturnType;
    }

    public Class<? extends Throwable>[] getHandledExceptions() {
        return handledExceptions;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public Class<?> getControllerAdviceBeanType() {
        return controllerAdviceBeanType;
    }

    public static class Builder {

        private final Class<?> controllerAdviceBeanType;
        private final Method method;
        private final String statusCode;
        private final Class<? extends Throwable>[] handledExceptions;
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
            if (returnType instanceof ParameterizedType) {
                var rawType = ((ParameterizedType) returnType).getRawType();
                if (rawType == ResponseEntity.class || rawType == HttpEntity.class) {
                    wrapperReturnType = returnType;
                    returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
                }
            }
            this.handledExceptions = Objects
                .requireNonNull(AnnotationUtils.findAnnotation(method, ExceptionHandler.class))
                .value();
        }

        public Type getReturnType() {
            return returnType;
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
