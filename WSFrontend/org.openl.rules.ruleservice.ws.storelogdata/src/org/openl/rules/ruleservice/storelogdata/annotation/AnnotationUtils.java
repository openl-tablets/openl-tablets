package org.openl.rules.ruleservice.storelogdata.annotation;

import java.lang.annotation.Annotation;

import org.openl.rules.ruleservice.storelogdata.StoreLogData;

public final class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static <T extends Annotation> T getAnnotationInServiceClassOrServiceMethod(StoreLogData storeLogData,
            Class<T> annotationClass) {
        if (storeLogData.getServiceMethod() != null) {
            T annotation = storeLogData.getServiceMethod().getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        if (storeLogData.getServiceClass() != null && storeLogData.getServiceClass()
            .isAnnotationPresent(annotationClass)) {
            return storeLogData.getServiceClass().getAnnotation(annotationClass);
        }
        return null;
    }
}
