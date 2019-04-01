package org.openl.rules.datatype.gen;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.objectweb.asm.Type;

/**
 * A set of utility methods for working togeter with ASM framework.
 *
 * @author Yury Molchan
 */
public class ASMUtils {

    /**
     * Search a method in the class. It works like {@link Class#getMethod(String, Class[])}
     *
     * @param clazz a target class in which the method is searching
     * @param methodName a method name for searching
     * @param argumentTypes an argument types descriptor of a method
     * @return null if the method is not found
     */
    public static Method getMethod(Class<?> clazz, String methodName, String argumentTypes) {
        Type[] types = Type.getArgumentTypes(argumentTypes);
        for (Method method : clazz.getMethods()) {
            if (methodName.equals(method.getName()) && Arrays.equals(types, Type.getArgumentTypes(method))) {
                return method;
            }
        }
        return null;
    }
}
