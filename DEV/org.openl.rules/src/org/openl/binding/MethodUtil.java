package org.openl.binding;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.reflect.MethodUtils;

import org.openl.base.INamedThing;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.print.DefaultFormat;

/**
 * @author snshor
 */
public final class MethodUtil {

    private static final Function<IOpenClass, String> DEFAULT_TYPE_CONVERTER = MethodUtil::printType;

    private MethodUtil() {
        // Hidden constructor
    }

    public static String printType(IOpenClass type) {
        return type != null ? type.getDisplayName(INamedThing.SHORT) : "null";
    }

    public static StringBuilder printMethod(IOpenMethodHeader method, StringBuilder buf) {
        buf.append(DEFAULT_TYPE_CONVERTER.apply(method.getType())).append(' ');
        printMethod(method, buf, DEFAULT_TYPE_CONVERTER);
        return buf;
    }

    public static String printConstructorWithNamedParameters(IOpenMethodHeader method, Map<String, IOpenClass> params) {
        var buf = new StringBuilder();
        if (method.getDeclaringClass() instanceof JavaOpenClass) {
            buf.append(method.getDeclaringClass().getPackageName()).append('\n');
        }
        buf.append(DEFAULT_TYPE_CONVERTER.apply(method.getType())).append(' ');
        var prefix = "";
        buf.append('(');
        for (Map.Entry<String, IOpenClass> name : params.entrySet()) {
            buf.append(prefix);
            prefix = ", ";
            buf.append(DEFAULT_TYPE_CONVERTER.apply(name.getValue())).append(" ").append(name.getKey());
        }
        endPrintingMethodName(buf);
        return buf.toString();
    }

    public static String printConstructor(IOpenMethodHeader method) {
        var buf = new StringBuilder();
        if (method.getDeclaringClass() instanceof JavaOpenClass) {
            buf.append(method.getDeclaringClass().getPackageName()).append('\n');
        }
        buf.append(DEFAULT_TYPE_CONVERTER.apply(method.getType())).append(' ');
        buf.append('(');
        printParameters(method, buf, DEFAULT_TYPE_CONVERTER);
        endPrintingMethodName(buf);
        return buf.toString();
    }

    public static String printSignature(IOpenMethodHeader methodHeader, final int mode) {
        var buf = new StringBuilder();
        Function<IOpenClass, String> typeConverter = (e) -> e.getDisplayName(mode);
        printMethod(methodHeader, buf, typeConverter);
        return buf.toString();
    }

    public static String printQualifiedMethodName(Method method) {
        return method.getDeclaringClass().getTypeName() + "." + MethodUtil.printMethod(method.getName(),
                method.getParameterTypes());
    }

    public static void printMethod(IOpenMethodHeader methodHeader,
                                   StringBuilder buf,
                                   Function<IOpenClass, String> typeConverter) {
        startPrintingMethodName(methodHeader.getName(), buf);
        printParameters(methodHeader, buf, typeConverter);
        endPrintingMethodName(buf);
    }

    private static void printParameters(IOpenMethodHeader methodHeader, StringBuilder buf, Function<IOpenClass, String> typeConverter) {
        var signature = methodHeader.getSignature();
        for (var i = 0; i < signature.getNumberOfParameters(); i++) {
            var type = typeConverter.apply(signature.getParameterType(i));
            var name = signature.getParameterName(i);
            if (i != 0) {
                buf.append(", ");
            }

            if (type != null) {
                buf.append(type);
            }

            if (type != null && name != null) {
                buf.append(' ');
            }

            if (name != null) {
                buf.append(name);
            }
        }
    }

    public static String printMethod(String name, Class<?>[] params, boolean shortClassNames) {
        return printMethod(name, params, shortClassNames, new StringBuilder()).toString();
    }

    public static String printMethod(String name, Class<?>[] params) {
        return printMethod(name, params, false, new StringBuilder()).toString();
    }

    public static StringBuilder printMethod(String name,
                                            Class<?>[] params,
                                            boolean shortClassNames,
                                            StringBuilder buf) {
        startPrintingMethodName(name, buf);

        for (var i = 0; i < params.length; i++) {
            String type = shortClassNames ? params[i].getSimpleName() : params[i].getTypeName();
            if (i != 0) {
                buf.append(", ");
            }
            buf.append(type);
        }

        endPrintingMethodName(buf);
        return buf;
    }

    public static String printMethod(String name, IOpenClass[] params) {
        return printMethod(name, params, new StringBuilder()).toString();
    }

    public static StringBuilder printMethod(String name, IOpenClass[] params, StringBuilder buf) {
        startPrintingMethodName(name, buf);

        for (var i = 0; params != null && i < params.length; i++) {
            var type = params[i].getName();
            if (i != 0) {
                buf.append(", ");
            }

            if (type != null) {
                buf.append(type);
            }
        }
        endPrintingMethodName(buf);
        return buf;
    }

    public static String printMethodWithParameterValues(IOpenMethodHeader method, Object[] params) {
        var buf = new StringBuilder();
        startPrintingMethodName(method.getName(), buf);

        var signature = method.getSignature();
        for (var i = 0; params != null && i < params.length; i++) {
            var name = signature.getParameterName(i);
            if (i != 0) {
                buf.append(", ");
            }

            if (name != null) {
                buf.append(name);
            }

            if (params[i] != null) {
                buf.append(" = ");
                DefaultFormat.format(params[i], buf);
            }
        }

        endPrintingMethodName(buf);

        return buf.toString();
    }

    private static void startPrintingMethodName(String name, StringBuilder buf) {
        buf.append(name).append('(');
    }

    private static void endPrintingMethodName(StringBuilder buf) {
        buf.append(')');
    }

    public static Method getMatchingAccessibleMethod(Class<?> methodOwner, String methodName, Class<?>[] argTypes) {
        Method resultMethod = null;
        var methods = methodOwner.getMethods();
        for (Method method : methods) {
            var signatureParams = method.getParameterTypes();
            if (methodName.equals(method.getName()) && signatureParams.length == argTypes.length) {
                if (isAssignable(argTypes, signatureParams)) {
                    method = MethodUtils.getAccessibleMethod(method);// kills inherited methods
                    if (method != null) {
                        if (resultMethod != null) {
                            resultMethod = getCloserMethod(resultMethod, method, argTypes);
                        } else {
                            resultMethod = method;
                        }
                    }
                }
            }
        }
        return resultMethod;
    }

    private static boolean isAssignable(Class<?>[] classArray, Class<?>[] toClassArray) {
        for (var i = 0; i < classArray.length; i++) {
            Class<?> from = classArray[i];
            Class<?> to = toClassArray[i];
            if (!ClassUtils.isAssignable(from, to)) {
                return false;
            }
        }
        return true;
    }

    private static Method getCloserMethod(Method firstMethod, Method secondMethod, Class<?>[] argTypes) {
        var firstTransfCount = getTransformationsCount(firstMethod.getParameterTypes(), argTypes);
        if (firstTransfCount < 0) {
            return secondMethod;
        }
        var secondTransfCount = getTransformationsCount(secondMethod.getParameterTypes(), argTypes);
        if (secondTransfCount < 0 || secondTransfCount >= firstTransfCount) {
            return firstMethod;
        }
        return secondMethod;
    }

    /**
     * Get differences between two signatures.
     *
     * @param signatureToCheck Signature to check
     * @param argTypes         Types of existing arguments.
     * @return <code>-1</code> if signature to check is not suitable for specified args and transformations count
     * otherwise.
     */
    private static int getTransformationsCount(Class<?>[] signatureToCheck, Class<?>[] argTypes) {
        if (!isAssignable(argTypes, signatureToCheck)) {
            return -1;
        }
        var transformationsCount = 0;
        for (var i = 0; i < argTypes.length; i++) {
            if (!signatureToCheck[i].equals(argTypes[i])) {
                transformationsCount++;
            }
        }
        return transformationsCount;
    }
}
