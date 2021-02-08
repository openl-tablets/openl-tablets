package org.openl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Method Parameter builder.
 *
 * @author Vladyslav Pikus
 */
public class MethodParameterBuilder {

    private final String parameterType;
    private final List<AnnotationDescription> annotations = new ArrayList<>();

    private MethodParameterBuilder(String parameterType) {
        InterfaceByteCodeBuilder.requireNonBlank(parameterType, "Method name is null or blank.");
        this.parameterType = parameterType;
    }

    /**
     * Build {@link TypeDescription} object
     * 
     * @return instance of {@link TypeDescription}
     */
    public TypeDescription build() {
        return new TypeDescription(parameterType, annotations.toArray(AnnotationDescription.EMPTY_ANNOTATIONS));
    }

    /**
     * Add new parameter annotation
     *
     * @param annotation parameter annotation
     * @return {@code this}
     */
    public MethodParameterBuilder addAnnotation(AnnotationDescription annotation) {
        annotations.add(Objects.requireNonNull(annotation, "Annotation description is null"));
        return this;
    }

    /**
     * Create builder from {@link Class} type
     * 
     * @param parameterType method parameter class
     * @return method parameter builder
     */
    public static MethodParameterBuilder create(Class<?> parameterType) {
        return new MethodParameterBuilder(parameterType.getName());
    }

    /**
     * Create builder from custom type
     * 
     * @param parameterType method parameter class
     * @return method parameter builder
     */
    public static MethodParameterBuilder create(String parameterType) {
        return new MethodParameterBuilder(parameterType);
    }
}
