package org.openl.rules.ruleservice.publish.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.util.generation.GenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MethodUtils {
    private MethodUtils() {
    }

    private static final Comparator<Method> METHOD_COMPARATOR = Comparator.comparing(Method::getName)
        .thenComparingInt(Method::getParameterCount)
        .thenComparing(Method::getParameterTypes, MethodUtils::compareNames);

    private static int compareNames(Class<?>[] p1, Class<?>[] p2) {
        for (int i = 0; i < p1.length; i++) {
            String name1 = p1[i].getName();
            String name2 = p2[i].getName();
            int cmp = name1.compareTo(name2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public static List<Method> sort(Collection<Method> m) {
        List<Method> methods = new ArrayList<>(m);
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }

    private static void validateAndUpdateParameterNames(String[] parameterNames) {
        Set<String> allNames = new HashSet<>(Arrays.asList(parameterNames));
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

    public static IOpenMethod getRulesMethod(Method method, OpenLService service) {
        try {
            if (service != null && service.getOpenClass() != null) {
                return OpenClassHelper.findRulesMethod(service.getOpenClass(), method);
            }
        } catch (RuleServiceInstantiationException ignored) {
        }
        return null;
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
                                        "Invalid parameter name '{}' is used in @Name annotation for method '{}.{}'.",
                                        name.value(),
                                        method.getClass().getTypeName(),
                                        MethodUtil.printMethod(method.getName(), method.getParameterTypes()));
                                }
                            }
                        }
                    }
                    i++;
                }

                validateAndUpdateParameterNames(parameterNames);

                return parameterNames;
            }
        } catch (RuleServiceInstantiationException ignored) {
        }
        return GenUtils.getParameterNames(method);
    }
}
