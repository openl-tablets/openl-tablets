package org.openl.gen;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.openl.gen.AnnotationDescription.AnnotationProperty;

/**
 * Annotation Description Builder
 *
 * @author Vladyslav Pikus
 */
public class AnnotationDescriptionBuilder {

    private final String annotationType;

    private final List<AnnotationProperty> properties = new ArrayList<>();

    private AnnotationDescriptionBuilder(String annotationType) {
        InterfaceByteCodeBuilder.requireNonBlank(annotationType, "Annotation type is null or blank.");
        this.annotationType = annotationType;
    }

    /**
     * Add annotation property
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @return {@link this}
     */
    public AnnotationDescriptionBuilder withProperty(String propertyName, Object propertyValue) {
        properties.add(new AnnotationProperty(propertyName, propertyValue));
        return this;
    }

    /**
     * Add annotation property
     *
     * @param propertyName property name
     * @param typeDescription java type description (when we need to write {@code Object.class} as value)
     * @return
     */
    public AnnotationDescriptionBuilder withProperty(String propertyName, TypeDescription typeDescription) {
        properties.add(new AnnotationProperty(propertyName, typeDescription));
        return this;
    }

    /**
     * Add annotation property
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param array determines if this property is array or not
     * @return {@link this}
     */
    public AnnotationDescriptionBuilder withProperty(String propertyName, Object propertyValue, boolean array) {
        properties.add(new AnnotationProperty(propertyName, propertyValue, array, false));
        return this;
    }

    /**
     * Build {@link AnnotationDescription} object
     * @return instance of {@link AnnotationDescription}
     */
    public AnnotationDescription build() {
        return new AnnotationDescription(annotationType, properties.toArray(AnnotationDescription.EMPTY_PROPS));
    }

    /**
     * Create annotation builder from {@link Class} type
     *
     * @param annotationType annotation type
     * @return annotation builder
     */
    public static AnnotationDescriptionBuilder create(Class<? extends Annotation> annotationType) {
        return new AnnotationDescriptionBuilder(annotationType.getName());
    }

    /**
     * Create annotation builder from custom type
     *
     * @param annotationType annotation type
     * @return annotation builder
     */
    public static AnnotationDescriptionBuilder create(String annotationType) {
        return new AnnotationDescriptionBuilder(annotationType);
    }

}
