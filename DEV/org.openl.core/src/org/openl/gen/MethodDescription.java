package org.openl.gen;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.openl.util.StringUtils;

/**
 * Method description.
 *
 * @author Vladyslav Pikus
 */
public class MethodDescription {

    static final TypeDescription[] NO_ARGS = new TypeDescription[0];

    private final String name;
    private final TypeDescription returnType;
    private final TypeDescription[] argsTypes;
    private final AnnotationDescription[] annotations;

    private final String[] argsNames;

    /**
     * Initialize method description with given parameters
     *
     * @param name       method name
     * @param returnType method return type
     * @param argsTypes  method parameters
     * @throws NullPointerException if method name or return type is null
     */
    public MethodDescription(String name, Class<?> returnType, Class<?>[] argsTypes, String[] argsNames) {
        this(name, returnType, argsTypes, AnnotationDescription.EMPTY_ANNOTATIONS, argsNames);
    }

    /**
     * Initialize method description with given parameters
     *
     * @param name        method name
     * @param returnType  method return type
     * @param argsTypes   method parameters
     * @param annotations method annotation descriptions
     * @throws NullPointerException if method name or return type is null
     */
    public MethodDescription(String name,
                             Class<?> returnType,
                             Class<?>[] argsTypes,
                             AnnotationDescription[] annotations,
                             String[] argsNames) {
        this(name,
                returnType.getName(),
                Stream.of(argsTypes).map(argType -> new TypeDescription(argType.getName())).toArray(TypeDescription[]::new),
                annotations, argsNames);
    }

    /**
     * Initialize method description with given parameters
     *
     * @param name        method name
     * @param returnType  method return type
     * @param argsTypes   method parameter descriptions
     * @param annotations method annotation descriptions
     * @throws NullPointerException if method name or return type is null
     */
    public MethodDescription(String name,
                             String returnType,
                             TypeDescription[] argsTypes,
                             AnnotationDescription[] annotations,
                             String[] argsNames) {
        Objects.requireNonNull(returnType, "Method return type is null.");
        this.name = Objects.requireNonNull(name, "Method name is null.");
        this.returnType = new TypeDescription(returnType);
        this.argsTypes = Optional.ofNullable(argsTypes).orElse(NO_ARGS);
        this.annotations = Optional.ofNullable(annotations).orElse(AnnotationDescription.EMPTY_ANNOTATIONS);
        this.argsNames = Optional.ofNullable(argsNames).orElse(StringUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Get method return type
     *
     * @return method return type
     */
    public TypeDescription getReturnType() {
        return returnType;
    }

    /**
     * Get method parameter descriptions
     *
     * @return method parameter descriptions
     */
    public TypeDescription[] getArgsTypes() {
        return argsTypes;
    }

    /**
     * Get method parameter names
     */
    public String[] getArgsNames() {
        return argsNames;
    }

    /**
     * Get method name
     *
     * @return method name
     */
    public String getName() {
        return name;
    }

    /**
     * Get method annotation description
     *
     * @return method annotation descriptionW
     */
    public AnnotationDescription[] getAnnotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodDescription that = (MethodDescription) o;
        return Objects.equals(name, that.name) && Arrays.equals(argsTypes, that.argsTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(argsTypes);
        return result;
    }
}
