package org.openl.gen;

import java.util.Objects;
import java.util.Optional;

/**
 * Annotation description.
 *
 * @author Vladyslav Pikus
 */
public class AnnotationDescription {

    private static final AnnotationProperty[] EMPTY_PROPS = new AnnotationProperty[0];
    static final AnnotationDescription[] EMPTY_ANNOTATIONS = new AnnotationDescription[0];

    private final TypeDescription annotationType;
    private final AnnotationProperty[] properties;

    /**
     * Initialize annotation description with given parameters.
     *
     * @param annotationType annotation class
     * @param properties annotation properties
     * @throws NullPointerException if {@code annotationType} is {@code null}
     */
    public AnnotationDescription(Class<?> annotationType, AnnotationProperty[] properties) {
        Objects.requireNonNull(annotationType, "Annotation type is null.");
        this.annotationType = new TypeDescription(annotationType.getName());
        this.properties = Optional.ofNullable(properties).orElse(EMPTY_PROPS);
    }

    /**
     * Get annotation type description
     *
     * @return annotation type description
     */
    public TypeDescription getAnnotationType() {
        return annotationType;
    }

    /**
     * Get annotation properties
     *
     * @return annotation properties
     */
    public AnnotationProperty[] getProperties() {
        return properties;
    }

    /**
     * Annotation property description.
     *
     * @author Vladyslav Pikus
     */
    public static class AnnotationProperty {

        private final String name;
        private final Object value;

        /**
         * Initialize annotation property with given parameters.
         *
         * @param name property name
         * @param value property value
         * @throws NullPointerException if any argument is {@code null}
         */
        public AnnotationProperty(String name, Object value) {
            this.name = Objects.requireNonNull(name, "Annotation property name is null.");
            this.value = Objects.requireNonNull(value, "Annotation property value is null.");
        }

        /**
         * Get annotation property name
         *
         * @return annotation property name
         */
        public String getName() {
            return name;
        }

        /**
         * Get annotation property value
         *
         * @return annotation property value
         */
        public Object getValue() {
            return value;
        }
    }
}
