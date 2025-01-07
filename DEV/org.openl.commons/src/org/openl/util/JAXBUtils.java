package org.openl.util;

import java.lang.reflect.ParameterizedType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public final class JAXBUtils {
    private JAXBUtils() {
    }

    public static Class<?> extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(Class<?> boundType) {
        if (!boundType.isPrimitive()) {
            var adapter = boundType.getAnnotation(XmlJavaTypeAdapter.class);
            if (adapter != null && adapter.value().getGenericSuperclass() instanceof ParameterizedType type) {
                if (type.getActualTypeArguments()[0] instanceof Class<?> clazz) {
                    return clazz;
                }
            }
        }
        return null;
    }
}
