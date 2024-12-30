package org.openl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.util.StringUtils;

/**
 * Method description builder
 *
 * @author Vladyslav Pikus
 */
public class MethodDescriptionBuilder {

    private final String methodName;
    private final String returnType;
    private final List<TypeDescription> params = new ArrayList<>();
    private final List<String> paramNames = new ArrayList<>();

    private final List<AnnotationDescription> annotations = new ArrayList<>();

    private MethodDescriptionBuilder(String methodName, String returnType) {
        InterfaceByteCodeBuilder.requireNonBlank(methodName, "Method name is null or blank.");
        this.methodName = methodName;
        this.returnType = returnType;
    }

    /**
     * Add new method parameter
     *
     * @param type method parameter
     * @return {@code this}
     */
    public MethodDescriptionBuilder addParameter(TypeDescription type) {
        params.add(Objects.requireNonNull(type, "Parameter is null."));
        return this;
    }

    public MethodDescriptionBuilder addParameterName(String name) {
        paramNames.add(Objects.requireNonNull(name, "Parameter name is null."));
        return this;
    }

    /**
     * Add new method annotation
     *
     * @param annotation method annotation
     * @return {@code this}
     */
    public MethodDescriptionBuilder addAnnotation(AnnotationDescription annotation) {
        annotations.add(Objects.requireNonNull(annotation, "Annotation description is null"));
        return this;
    }

    /**
     * Build {@link MethodDescription} object
     *
     * @return instance of {@link MethodDescription}
     */
    public MethodDescription build() {
        return new MethodDescription(methodName,
                returnType,
                params.toArray(MethodDescription.NO_ARGS),
                annotations.toArray(AnnotationDescription.EMPTY_ANNOTATIONS),
                paramNames.toArray(StringUtils.EMPTY_STRING_ARRAY));
    }

    /**
     * Create method builder from {@link Class} type
     *
     * @param methodName method name
     * @param returnType method return type
     * @return method parameter builder
     */
    public static MethodDescriptionBuilder create(String methodName, Class<?> returnType) {
        return new MethodDescriptionBuilder(methodName, returnType.getName());
    }

    /**
     * Create method builder from custom type
     *
     * @param methodName method name
     * @param returnType method return type
     * @return method parameter builder
     */
    public static MethodDescriptionBuilder create(String methodName, String returnType) {
        return new MethodDescriptionBuilder(methodName, returnType);
    }
}
