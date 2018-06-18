package org.openl.util.generation;

import org.openl.rules.variation.VariationsPack;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.JavaKeywordUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
                IMethodSignature signature = m.getSignature();
                int numberOfParameters = signature.getNumberOfParameters();
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
                    if (i >= numberOfParameters) {
                        f = false;
                        break;
                    }
                    if (!clazz.equals(signature.getParameterType(i).getInstanceClass())) {
                        f = false;
                        break;
                    }
                    i++;
                }
                if (f && i != numberOfParameters){
                    f = false;
                }
                if (f) {
                    List<String> parameterNames = new ArrayList<>();
                    if (hasContext) {
                        parameterNames.add("runtimeContext");
                    }
                    for (i = 0; i < numberOfParameters; i++) {
                        String pName = signature.getParameterName(i);
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

}
