package org.openl.rules.spring.openapi.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;

import org.openl.rules.spring.openapi.OpenApiUtils;

/**
 * Information holder for parameters of methods of Spring REST Controllers
 */
public class ParameterInfo {

    @Getter
    private final MethodInfo methodInfo;
    @Getter
    private final MethodParameter methodParameter;
    @Getter
    private final int index;
    @Getter
    private final Type type;
    @Getter
    @Setter
    private Parameter parameter;
    @Getter
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

    public <T extends Annotation> T getParameterAnnotation(Class<T> anno) {
        return methodParameter.getParameterAnnotation(anno);
    }
}
