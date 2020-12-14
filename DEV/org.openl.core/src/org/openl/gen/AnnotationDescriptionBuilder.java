package org.openl.gen;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.openl.gen.AnnotationDescription.AnnotationProperty;

/**
 * @author Vladyslav Pikus
 */
public class AnnotationDescriptionBuilder {

    private final String annotationType;

    private final List<AnnotationProperty> properties = new ArrayList<>();

    private AnnotationDescriptionBuilder(String annotationType) {
        JavaInterfaceByteCodeBuilder.requireNonBlank(annotationType, "Annotation type is null or blank.");
        this.annotationType = annotationType;
    }

    public AnnotationDescriptionBuilder withProperty(String propertyName, Object propertyValue) {
        properties.add(new AnnotationProperty(propertyName, propertyValue));
        return this;
    }

    public AnnotationDescription build() {
        return new AnnotationDescription(annotationType, properties.toArray(AnnotationDescription.EMPTY_PROPS));
    }

    public static AnnotationDescriptionBuilder create(Class<? extends Annotation> annotationType) {
        return new AnnotationDescriptionBuilder(annotationType.getName());
    }

    public static AnnotationDescriptionBuilder create(String annotationType) {
        return new AnnotationDescriptionBuilder(annotationType);
    }

}
