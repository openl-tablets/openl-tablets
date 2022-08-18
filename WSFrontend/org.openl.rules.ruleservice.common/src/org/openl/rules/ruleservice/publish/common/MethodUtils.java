package org.openl.rules.ruleservice.publish.common;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.variation.VariationsPack;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.util.ClassUtils;
import org.openl.util.JavaKeywordUtils;
import org.openl.util.generation.GenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MethodUtils {
    private MethodUtils() {
    }

    private static void validateAndUpdateParameterNames(List<String> parameterNames) {
        Set<String> allNames = new HashSet<>(parameterNames);
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < parameterNames.size(); i++) {
            if (allNames.contains(parameterNames.get(i))) {
                allNames.remove(parameterNames.get(i));
                usedNames.add(parameterNames.get(i));
            } else {
                int j = 0;
                while (allNames.contains("arg" + j) || usedNames.contains("arg" + j)) {
                    j++;
                }
                parameterNames.set(i, "arg" + j);
            }
        }
    }

    public static String[] getParameterNames(IOpenMember openMember,
            Method method,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        List<String> parameterNames = new ArrayList<>();
        boolean f = false;
        if (provideRuntimeContext) {
            parameterNames.add("runtimeContext");
        }
        if (openMember instanceof IOpenMethod) {
            IOpenMethod openMethod = (IOpenMethod) openMember;
            IMethodSignature methodSignature = openMethod.getSignature();
            for (int i = 0; i < methodSignature.getNumberOfParameters(); i++) {
                String pName = methodSignature.getParameterName(i);
                parameterNames.add(pName);
            }
            f = true;
        } else if (openMember instanceof IOpenField) {
            IOpenField openField = (IOpenField) openMember;
            if (ClassUtils.getter(openField.getName()).equals(method.getName())) {
                f = true;
            }
        }
        if (f && provideVariations && method.getParameters()[method.getParameters().length - 1].getType()
            .isAssignableFrom(VariationsPack.class)) {
            parameterNames.add("variationPack");
        }
        if (!f) {
            parameterNames = new ArrayList<>(Arrays.asList(GenUtils.getParameterNames(method)));
        }
        GenUtils.fixJavaKeyWords(parameterNames);

        int i = 0;
        for (Parameter parameter : method.getParameters()) {
            Name name = parameter.getAnnotation(Name.class);
            if (name != null) {
                if (!name.value().isEmpty() && !JavaKeywordUtils.isJavaKeyword(name.value())) {
                    parameterNames.set(i, name.value());
                } else {
                    Logger log = LoggerFactory.getLogger(MethodUtils.class);
                    if (log.isWarnEnabled()) {
                        log.warn("Invalid parameter name '{}' is used in @Name annotation for the method '{}.{}'.",
                                name.value(),
                                method.getClass().getTypeName(),
                                MethodUtil.printMethod(method.getName(), method.getParameterTypes()));
                    }
                }
            }
            i++;
        }

        validateAndUpdateParameterNames(parameterNames);
        return parameterNames.toArray(new String[] {});
    }
}