package org.openl.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Vladyslav Pikus
 */
public class MethodDescriptionBuilder {

    private final String methodName;
    private final String returnType;
    private final List<TypeDescription> params = new ArrayList<>();
    private final List<AnnotationDescription> annotations = new ArrayList<>();

    private MethodDescriptionBuilder(String methodName, String returnType) {
        JavaInterfaceByteCodeBuilder.requireNonBlank(methodName, "Method name is null or blank.");
        this.methodName = methodName;
        this.returnType = returnType;
    }

    public MethodDescriptionBuilder addParameter(TypeDescription type) {
        params.add(Objects.requireNonNull(type, "Parameter is null."));
        return this;
    }

    public MethodDescriptionBuilder addAnnotation(AnnotationDescription annotation) {
        annotations.add(Objects.requireNonNull(annotation, "Annotation description is null"));
        return this;
    }

    public MethodDescription build() {
        return new MethodDescription(methodName, returnType,
                params.toArray(MethodDescription.NO_ARGS),
                annotations.toArray(AnnotationDescription.EMPTY_ANNOTATIONS));
    }

    public static MethodDescriptionBuilder create(String methodName, Class<?> returnType) {
        return new MethodDescriptionBuilder(methodName, returnType.getName());
    }

    public static MethodDescriptionBuilder create(String methodName, String returnType) {
        return new MethodDescriptionBuilder(methodName, returnType);
    }
}
