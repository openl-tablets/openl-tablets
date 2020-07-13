package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class SupportConverterHelper {
    private SupportConverterHelper() {
    }

    static List<Method> getAllMethods(Class<?> cls) {
        if (cls == null) {
            return Collections.emptyList();
        }
        List<Method> methods = new ArrayList<>(Arrays.asList(cls.getMethods()));
        List<Class<?>> interfaces = org.apache.commons.lang3.ClassUtils.getAllInterfaces(cls);
        List<Class<?>> superClasses = org.apache.commons.lang3.ClassUtils.getAllSuperclasses(cls);
        interfaces.forEach(e -> methods.addAll(Arrays.asList(e.getMethods())));
        superClasses.forEach(e -> methods.addAll(Arrays.asList(e.getMethods())));
        return methods;
    }
}
