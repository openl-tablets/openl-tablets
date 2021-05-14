package org.openl.rules.ruleservice.storelogdata;

import java.lang.annotation.Annotation;

public class Inject {
    private final Class<? extends Annotation> annotationClass;
    private final Object resource;

    public Inject(Class<? extends Annotation> annotationClass, Object resource) {
        this.annotationClass = annotationClass;
        this.resource = resource;
    }

    public Class<? extends Annotation> getAnnotationClass() {
        return annotationClass;
    }

    public Object getResource() {
        return resource;
    }
}
