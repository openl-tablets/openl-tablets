package org.openl.rules.ruleservice.storelogdata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Inject<R> {
    private final Class<? extends Annotation> annotationClass;
    private final BiFunction<Method, Annotation, R> resourceFunction;
    private final Consumer<R> destroyFunction;

    public Inject(Class<? extends Annotation> annotationClass, BiFunction<Method, Annotation, R> resourceFunction) {
        this(annotationClass, resourceFunction, null);
    }

    public Inject(Class<? extends Annotation> annotationClass,
            BiFunction<Method, Annotation, R> resourceFunction,
            Consumer<R> destroyFunction) {
        this.annotationClass = annotationClass;
        this.resourceFunction = resourceFunction;
        this.destroyFunction = destroyFunction;
    }

    public Class<? extends Annotation> getAnnotationClass() {
        return annotationClass;
    }

    public R getResource(Method interfaceMethod, Annotation annotation) {
        return resourceFunction.apply(interfaceMethod, annotation);
    }

    @SuppressWarnings("unchecked")
    public void destroy(Object resource) {
        if (destroyFunction != null) {
            destroyFunction.accept((R) resource);
        }
    }
}
