package org.openl.rules.ruleservice.storelogdata;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Inject<T> {
    private final Class<? extends Annotation> annotationClass;
    private final Supplier<T> resourceSupplier;
    private final Consumer<T> destroyFunction;

    public Inject(Class<? extends Annotation> annotationClass, Supplier<T> resourceSupplier) {
        this(annotationClass, resourceSupplier, null);
    }

    public Inject(Class<? extends Annotation> annotationClass,
            Supplier<T> resourceSupplier,
            Consumer<T> destroyFunction) {
        this.annotationClass = annotationClass;
        this.resourceSupplier = resourceSupplier;
        this.destroyFunction = destroyFunction;
    }

    public Class<? extends Annotation> getAnnotationClass() {
        return annotationClass;
    }

    public T getResource() {
        return resourceSupplier.get();
    }

    @SuppressWarnings("unchecked")
    public void destroy(Object resource) {
        if (destroyFunction != null) {
            destroyFunction.accept((T) resource);
        }
    }
}
