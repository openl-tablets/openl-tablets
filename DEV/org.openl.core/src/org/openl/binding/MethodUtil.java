/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.lang.reflect.Method;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.openl.base.INamedThing;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.util.ClassUtils;
import org.openl.util.IConvertor;
import org.openl.util.print.DefaultFormat;

/**
 * @author snshor
 *
 */
public final class MethodUtil {

    private static final IConvertor<IOpenClass, String> DEFAULT_TYPE_CONVERTER = (e) -> printType(e);

    private MethodUtil() {
        // Hidden constructor
    }

    public static String printType(IOpenClass type) {
        return type.getDisplayName(INamedThing.SHORT);
    }

    public static StringBuilder printMethod(IOpenMethodHeader method, StringBuilder buf) {
        buf.append(DEFAULT_TYPE_CONVERTER.convert(method.getType())).append(' ');
        printMethod(method, buf, DEFAULT_TYPE_CONVERTER);
        return buf;
    }

    public static String printSignature(IOpenMethodHeader methodHeader, final int mode) {
        StringBuilder buf = new StringBuilder(100);
        IConvertor<IOpenClass, String> typeConverter = (e) -> e.getDisplayName(mode);
        printMethod(methodHeader, buf, typeConverter);

        return buf.toString();
    }

    public static String printQualifiedMethodName(Method method) {
        return method.getDeclaringClass().getTypeName() + "." + MethodUtil.printMethod(method.getName(),
            method.getParameterTypes());
    }

    private static void printMethod(IOpenMethodHeader methodHeader,
            StringBuilder buf,
            IConvertor<IOpenClass, String> typeConverter) {

        startPrintingMethodName(methodHeader.getName(), buf);

        IMethodSignature signature = methodHeader.getSignature();

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            String type = typeConverter.convert(signature.getParameterType(i));
            String name = signature.getParameterName(i);
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

        endPrintingMethodName(buf);
    }

    public static String printMethod(String name, Class<?>[] params) {
        return printMethod(name, params, new StringBuilder()).toString();
    }

    public static StringBuilder printMethod(String name, Class<?>[] params, StringBuilder buf) {
        startPrintingMethodName(name, buf);

        for (int i = 0; i < params.length; i++) {
            String type = params[i].getName();
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

    public static String printMethod(String name, IOpenClass[] params) {
        return printMethod(name, params, new StringBuilder()).toString();
    }

    public static StringBuilder printMethod(String name, IOpenClass[] params, StringBuilder buf) {
        startPrintingMethodName(name, buf);

        for (int i = 0; params != null && i < params.length; i++) {
            String type = params[i].getName();
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
        StringBuilder buf = new StringBuilder();
        startPrintingMethodName(method.getName(), buf);

        IMethodSignature signature = method.getSignature();
        for (int i = 0; params != null && i < params.length; i++) {
            String name = signature.getParameterName(i);
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
        Method[] methods = methodOwner.getMethods();
        for (Method method : methods) {
            Class<?>[] signatureParams = method.getParameterTypes();
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
        for (int i = 0; i < classArray.length; i++) {
            Class<?> from = classArray[i];
            Class<?> to = toClassArray[i];
            if (!ClassUtils.isAssignable(from, to)) {
                return false;
            }
        }
        return true;
    }

    private static Method getCloserMethod(Method firstMethod, Method secondMethod, Class<?>[] argTypes) {
        int firstTransfCount = getTransformationsCount(firstMethod.getParameterTypes(), argTypes);
        if (firstTransfCount < 0) {
            return secondMethod;
        }
        int secondTransfCount = getTransformationsCount(secondMethod.getParameterTypes(), argTypes);
        if (secondTransfCount < 0 || secondTransfCount >= firstTransfCount) {
            return firstMethod;
        }
        return secondMethod;
    }

    /**
     * Get differences between two signatures.
     *
     * @param signatureToCheck Signature to check
     * @param argTypes Types of existing arguments.
     * @return <code>-1</code> if signature to check is not suitable for specified args and transformations count
     *         otherwise.
     */
    private static int getTransformationsCount(Class<?>[] signatureToCheck, Class<?>[] argTypes) {
        if (!isAssignable(argTypes, signatureToCheck)) {
            return -1;
        }
        int transformationsCount = 0;
        for (int i = 0; i < argTypes.length; i++) {
            if (!signatureToCheck[i].equals(argTypes[i])) {
                transformationsCount++;
            }
        }
        return transformationsCount;
    }
}
