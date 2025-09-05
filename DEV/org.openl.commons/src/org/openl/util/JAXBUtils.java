package org.openl.util;

import java.lang.reflect.ParameterizedType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public final class JAXBUtils {
    private JAXBUtils() {
    }

    public static Class<?> extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(Class<?> boundType) {
        return extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(boundType,
                boundType.getAnnotation(XmlJavaTypeAdapter.class));
    }

    public static Class<?> extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(Class<?> boundType,
                                                                             XmlJavaTypeAdapter xmlJavaTypeAdapter) {
        if (!boundType.isPrimitive()) {
            if (xmlJavaTypeAdapter != null) {
                @SuppressWarnings("rawtypes")
                Class<? extends XmlAdapter> xmlAdapterClass = xmlJavaTypeAdapter.value();
                java.lang.reflect.Type type = xmlAdapterClass.getGenericSuperclass();
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    java.lang.reflect.Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments[0] instanceof Class) {
                        return (Class<?>) actualTypeArguments[0];
                    }
                }
            }
        }
        return null;
    }

}