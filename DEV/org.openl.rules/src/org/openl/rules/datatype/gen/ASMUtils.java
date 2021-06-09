package org.openl.rules.datatype.gen;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

/**
 * A set of utility methods for working togeter with ASM framework.
 *
 * @author Yury Molchan
 */
public final class ASMUtils {

    private ASMUtils() {
    }

    /**
     * Search a method in the built map;
     *
     * @param methodsMap a target map in which the method is searching
     * @param methodName a method name for searching
     * @param descriptor an argument types descriptor of a method
     * @return null if the method is not found
     */
    public static Method findMethod(Map<String, List<Method>> methodsMap, String methodName, String descriptor) {
        List<Method> listOfMethods = methodsMap.get(methodName);
        if (listOfMethods != null) {
            for (Method method : listOfMethods) {
                if (descriptor.equals(Type.getMethodDescriptor(method))) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Builds a map for searching a method
     * 
     * @param clazz a target class to build a map
     * @return returns a map
     */
    public static Map<String, List<Method>> buildMap(Class<?> clazz) {
        Map<String, List<Method>> ret = new HashMap<>();
        for (Method method : clazz.getMethods()) {
            List<Method> listOfMethods = ret.computeIfAbsent(method.getName(), e -> new ArrayList<>());
            listOfMethods.add(method);
        }
        return ret;
    }

}
