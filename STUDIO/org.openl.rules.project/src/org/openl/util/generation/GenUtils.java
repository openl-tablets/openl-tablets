package org.openl.util.generation;

import org.openl.rules.variation.VariationsPack;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.JavaKeywordUtils;
import org.openl.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ymolchan on 17.05.2017.
 */
public final class GenUtils {

    public static String[] getParameterNames(Method method) {
        String[] parameterNames = new String[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            parameterNames[i] = "arg" + i;
        }
        return parameterNames;
    }

    public static String[] getParameterNames(Method method, IOpenClass openClass, boolean hasContext, boolean hasVariations) {
        for (IOpenMethod m : openClass.getMethods()) {
            if (m.getName().equals(method.getName())) {
                int i = 0;
                boolean f = true;
                boolean skipRuntimeContextParameter = false;
                boolean variationPackIsLastParameter = false;
                int j = 0;
                for (Class<?> clazz : method.getParameterTypes()) {
                    j++;
                    if (hasContext && !skipRuntimeContextParameter) {
                        skipRuntimeContextParameter = true;
                        continue;
                    }
                    if (j == method.getParameterTypes().length && hasVariations && clazz.isAssignableFrom(VariationsPack.class)) {
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
                    if (hasContext) {
                        parameterNames.add("runtimeContext");
                    }
                    for (i = 0; i < m.getSignature().getNumberOfParameters(); i++) {
                        String pName = convertParameterName(m.getSignature().getParameterName(i));
                        parameterNames.add(pName);
                    }
                    if (variationPackIsLastParameter) {
                        parameterNames.add("variationPack");
                    }

                    fixJavaKeyWords(parameterNames);

                    return parameterNames.toArray(new String[] {});
                }
            }
        }
        return getParameterNames(method);
    }

    private static void fixJavaKeyWords(List<String> parameterNames) {
        for (int i = 0; i < parameterNames.size(); i++) {
            if (JavaKeywordUtils.isJavaKeyword(parameterNames.get(i))) {
                int k = 0;
                boolean f = false;
                while (!f) {
                    k++;
                    String s = parameterNames.get(i) + k;
                    boolean g = true;
                    for (int j = 0; j < parameterNames.size(); j++) {
                        if (j != i && s.equals(parameterNames.get(j))) {
                            g = false;
                            break;
                        }
                    }
                    if (g) {
                        f = true;
                    }
                }
                parameterNames.set(i, parameterNames.get(i) + k);
            }
        }
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
