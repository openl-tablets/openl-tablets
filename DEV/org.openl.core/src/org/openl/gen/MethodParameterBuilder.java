package org.openl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Vladyslav Pikus
 */
public class MethodParameterBuilder {

    private final String parameterType;
    private final List<AnnotationDescription> annotations = new ArrayList<>();

    private MethodParameterBuilder(String parameterType) {
        JavaInterfaceByteCodeBuilder.requireNonBlank(parameterType, "Method name is null or blank.");
        this.parameterType = parameterType;
    }

    public TypeDescription build() {
        return new TypeDescription(parameterType, annotations.toArray(AnnotationDescription.EMPTY_ANNOTATIONS));
    }

    public MethodParameterBuilder addAnnotation(AnnotationDescription annotation) {
        annotations.add(Objects.requireNonNull(annotation, "Annotation description is null"));
        return this;
    }

    public static MethodParameterBuilder create(Class<?> parameterType) {
        return new MethodParameterBuilder(parameterType.getName());
    }

    public static MethodParameterBuilder create(String parameterType) {
        return new MethodParameterBuilder(parameterType);
    }
}
