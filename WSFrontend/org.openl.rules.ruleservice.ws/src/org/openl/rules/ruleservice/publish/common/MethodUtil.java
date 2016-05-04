package org.openl.rules.ruleservice.publish.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.variation.VariationsPack;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;

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
            for (IOpenMethod m : service.getOpenClass().getMethods()) {
                if (m.getName().equals(method.getName())) {
                    int i = 0;
                    boolean f = true;
                    boolean skipRuntimeContextParameter = false;
                    boolean variationPackIsLastParameter = false;
                    int j = 0;
                    for (Class<?> clazz : method.getParameterTypes()) {
                        j++;
                        if (service.isProvideRuntimeContext() && !skipRuntimeContextParameter) {
                            skipRuntimeContextParameter = true;
                            continue;
                        }
                        if (j == method.getParameterTypes().length && service.isProvideVariations() && clazz.isAssignableFrom(VariationsPack.class)) {
                            variationPackIsLastParameter = true;
                            continue;
                        }
                        if (i >= m.getSignature().getNumberOfParameters()) {
                            f = false;
                            break;
                        }
                        if (!clazz.equals(m.getSignature().getParameterType(i).getInstanceClass())) {
                            f = false;
                            break;
                        }
                        i++;
                    }
                    if (f && i != m.getSignature().getNumberOfParameters()){
                        f = false;
                    }
                    if (f) {
                        List<String> parameterNames = new ArrayList<String>();
                        if (service.isProvideRuntimeContext()) {
                            parameterNames.add("runtimeContext");
                        }
                        for (i = 0; i < m.getSignature().getNumberOfParameters(); i++) {
                            String pName = convertParameterName(m.getSignature().getParameterName(i));
                            parameterNames.add(pName);
                        }
                        if (variationPackIsLastParameter) {
                            parameterNames.add("variationPack");
                        }
                        return parameterNames.toArray(new String[] {});
                    }
                }
            }
        }
        String[] parameterNames = new String[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            parameterNames[i] = "arg" + i;
        }
        return parameterNames;
    }

    public static String convertParameterName(String pName) {
        if (pName.length() == 1){
            return pName.toLowerCase();
        }else{
            if (pName.length() > 1 && Character.isUpperCase(pName.charAt(1))){
                return StringUtils.capitalize(pName);
            }else{
                return StringUtils.uncapitalize(pName);
            }
        }
    }
}
