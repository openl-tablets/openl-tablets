package org.openl.gen;

import java.util.Objects;
import java.util.Optional;

/**
 * Annotation description.
 *
 * @author Vladyslav Pikus
 */
public class AnnotationDescription {

    static final AnnotationProperty[] EMPTY_PROPS = new AnnotationProperty[0];
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
    AnnotationDescription(Class<?> annotationType, AnnotationProperty[] properties) {
        this(annotationType.getName(), properties);
    }

    AnnotationDescription(String annotationType, AnnotationProperty[] properties) {
        Objects.requireNonNull(annotationType, "Annotation type is null.");
        this.annotationType = new TypeDescription(annotationType);
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
        private final boolean array;

        /**
         * Initialize annotation property with given parameters.
         *
         * @param name property name
         * @param value property value
         * @throws NullPointerException if any argument is {@code null}
         */
        AnnotationProperty(String name, Object value) {
            this(name, value, Optional.ofNullable(value)
                    .map(Object::getClass)
                    .map(Class::isArray)
                    .orElse(false));
        }

        /**
         * Initialize annotation property with given parameters.
         *
         * @param name property name
         * @param value property value
         * @param array determines if this property is array or not
         * @throws NullPointerException if any argument is {@code null}
         */
        AnnotationProperty(String name, Object value, boolean array) {
            this.name = Objects.requireNonNull(name, "Annotation property name is null.");
            this.value = Objects.requireNonNull(value, "Annotation property value is null.");
            this.array = array;
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

        /**
         * Get an array identificator
         *
         * @return an array identificator
         */
        public boolean isArray() {
            return array;
        }
    }
}
