package org.openl.rules.ruleservice.publish.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.types.IOpenClass;
import org.openl.util.generation.GenUtils;

public final class MethodUtil {
    private MethodUtil() {
    }

    public static List<Method> sort(Collection<Method> m) {
        List<Method> methods = new ArrayList<Method>(m);
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                if (o1.getName().equals(o2.getName())) {
                    if (o1.getParameterTypes().length == o2.getParameterTypes().length) {
                        int i = 0;
                        while (i < o1.getParameterTypes().length && o1.getParameterTypes()[i].equals(o2.getParameterTypes()[i])) {
                            i++;
                        }
                        return o1.getParameterTypes()[i].getName().compareTo(o2.getParameterTypes()[i].getName());
                    } else {
                        return o1.getParameterTypes().length - o2.getParameterTypes().length;
                    }
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            };
        });
        return methods;
    }

    public static String[] getParameterNames(Method method, OpenLService service) {
        if (service != null && service.getOpenClass() != null) {
            IOpenClass openClass = service.getOpenClass();
            boolean provideRuntimeContext = service.isProvideRuntimeContext();
            boolean provideVariations = service.isProvideVariations();
            String[] parameterNames = GenUtils.getParameterNames(method, openClass, provideRuntimeContext, provideVariations);
            return parameterNames;
        }
        return GenUtils.getParameterNames(method);
    }
}
