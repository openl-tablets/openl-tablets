package org.openl.rules.spring.openapi.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

import org.openl.rules.spring.openapi.OpenApiUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Information holder for parameters of methods of Spring REST Controllers
 */
public class ParameterInfo {

    private final MethodInfo methodInfo;
    private final MethodParameter methodParameter;
    private final int index;
    private final Type type;
    private Parameter parameter;
    private final JsonView jsonView;

    public ParameterInfo(MethodInfo methodInfo, MethodParameter methodParameter, int index) {
        this.methodInfo = methodInfo;
        this.methodParameter = methodParameter;
        this.index = index;
        this.parameter = AnnotatedElementUtils.findMergedAnnotation(methodParameter.getParameter(), Parameter.class);
        this.jsonView = Optional.ofNullable(methodParameter.getParameterAnnotation(JsonView.class))
            .orElseGet(methodInfo::getJsonView);
        this.type = OpenApiUtils.getType(methodParameter);
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotation) {
        return methodParameter.hasParameterAnnotation(annotation);
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public MethodParameter getMethodParameter() {
        return methodParameter;
    }

    public int getIndex() {
        return index;
    }

    public Type getType() {
        return type;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public JsonView getJsonView() {
        return jsonView;
    }

    public <T extends Annotation> T getParameterAnnotation(Class<T> anno) {
        return methodParameter.getParameterAnnotation(anno);
    }
}
