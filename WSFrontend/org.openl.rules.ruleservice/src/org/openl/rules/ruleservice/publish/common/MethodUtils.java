package org.openl.rules.ruleservice.publish.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.types.IOpenClass;
import org.openl.util.generation.GenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MethodUtils {
    private MethodUtils() {
    }

    private static final Comparator<Method> METHOD_COMPARATOR = (o1, o2) -> {
        if (o1.getName().equals(o2.getName())) {
            if (o1.getParameterTypes().length == o2.getParameterTypes().length) {
                int i = 0;
                while (i < o1.getParameterTypes().length && o1.getParameterTypes()[i]
                    .equals(o2.getParameterTypes()[i])) {
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

    public static Method[] sort(Method[] m) {
        Method[] methods = m.clone();
        Arrays.sort(methods, METHOD_COMPARATOR);
        return methods;
    }

    public static List<Method> sort(Collection<Method> m) {
        List<Method> methods = new ArrayList<>(m);
        Collections.sort(methods, METHOD_COMPARATOR);
        return methods;
    }

    private static void validateAndUpdateParameterNames(String[] parameterNames) {
        Set<String> allNames = new HashSet<>();
        for (String s : parameterNames) {
            allNames.add(s);
        }
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < parameterNames.length; i++) {
            if (allNames.contains(parameterNames[i])) {
                allNames.remove(parameterNames[i]);
                usedNames.add(parameterNames[i]);
            } else {
                int j = 0;
                while (allNames.contains("arg" + j) || usedNames.contains("arg" + j)) {
                    j++;
                }
                parameterNames[i] = "arg" + j;
            }
        }
    }

    public static String[] getParameterNames(Method method, OpenLService service) {
        try {
            if (service != null && service.getOpenClass() != null) {
                IOpenClass openClass = service.getOpenClass();
                boolean provideRuntimeContext = service.isProvideRuntimeContext();
                boolean provideVariations = service.isProvideVariations();
                String[] parameterNames = GenUtils
                    .getParameterNames(method, openClass, provideRuntimeContext, provideVariations);

                int i = 0;
                for (Annotation[] annotations : method.getParameterAnnotations()) {
                    for (Annotation annotation : annotations) {
                        if (annotation instanceof Name) {
                            Name name = (Name) annotation;
                            if (!name.value().isEmpty()) {
                                parameterNames[i] = name.value();
                            } else {
                                Logger log = LoggerFactory.getLogger(MethodUtils.class);
                                if (log.isWarnEnabled()) {
                                    log.warn(
                                        "Invalid parameter name '" + name.value() + "'. Parameter name for '" + method
                                            .getClass()
                                            .getCanonicalName() + "#" + method.getName() + "' was skipped!");
                                }
                            }
                        }
                    }
                    i++;
                }

                validateAndUpdateParameterNames(parameterNames);

                return parameterNames;
            }
        } catch (RuleServiceInstantiationException e) {
        }
        return GenUtils.getParameterNames(method);
    }
}
