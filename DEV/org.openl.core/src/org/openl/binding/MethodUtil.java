/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.lang.reflect.Method;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.openl.base.INamedThing;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.OpenClassDelegator;
import org.openl.util.IConvertor;
import org.openl.util.print.Formatter;

/**
 * @author snshor
 *
 */
public class MethodUtil {

    private static final IConvertor<IOpenClass, String> DEFAULT_TYPE_CONVERTER = new IConvertor<IOpenClass, String>() {
        @Override public String convert(IOpenClass type) {
            return printType(type);
        }
    };

    public static String printType(IOpenClass type) {
        return type instanceof OpenClassDelegator ? type.getName() : type.getDisplayName(INamedThing.SHORT);
    }

    public static StringBuilder printMethod(IOpenMethodHeader method, StringBuilder buf) {
        printMethod(method, true, buf, DEFAULT_TYPE_CONVERTER);
        return buf;
    }

    public static String printMethod(IOpenMethodHeader methodHeader, final int mode, boolean printType) {
        StringBuilder buf = new StringBuilder(100);
        IConvertor<IOpenClass, String> typeConverter = new IConvertor<IOpenClass, String>() {
            @Override public String convert(IOpenClass type) {
                return type.getDisplayName(mode);
            }
        };

        printMethod(methodHeader, printType, buf, typeConverter);

        return buf.toString();
    }

    public static void printMethod(IOpenMethodHeader methodHeader,
            boolean printType,
            StringBuilder buf,
            IConvertor<IOpenClass, String> typeConverter) {
        if (printType) {
            buf.append(typeConverter.convert(methodHeader.getType())).append(' ');
        }

        startPrintingMethodName(methodHeader.getName(), buf);

        IMethodSignature signature = methodHeader.getSignature();

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            printParameterInfo(typeConverter.convert(signature.getParameterType(i)), signature.getParameterName(i), i == 0, buf);
        }

        endPrintingMethodName(buf);
    }

    public static String printMethod(String name, Class<?>[] params) {
        return printMethod(name, params, new StringBuilder()).toString();
    }

    public static StringBuilder printMethod(String name, Class<?>[] params, StringBuilder buf) {
        startPrintingMethodName(name, buf);
        
        for (int i = 0; i < params.length; i++) {
            printParameterInfo(params[i].getName(), null, i == 0, buf);
        }
        
        endPrintingMethodName(buf);
        return buf;
    }

    public static String printMethod(String name, IOpenClass[] params) {
        return printMethod(name, params, new StringBuilder()).toString();
    }

    public static StringBuilder printMethod(String name, IOpenClass[] params, StringBuilder buf) {
        startPrintingMethodName(name, buf);
        
        for (int i = 0; i < params.length; i++) {
            printParameterInfo(params[i].getName(), null, i == 0, buf);
        }
        endPrintingMethodName(buf);
        return buf;
    }

    public static StringBuilder printMethodWithParameterValues(IOpenMethodHeader method, Object[] params, int mode, StringBuilder buf) {
        startPrintingMethodName(method.getName(), buf);

        IMethodSignature signature = method.getSignature();
        for (int i = 0; i < params.length; i++) {
            printParameterInfo(null, signature.getParameterName(i), params[i], i == 0, mode, buf);
        }

        endPrintingMethodName(buf);

        return buf;
    }
    
    public static String printMethodWithParameterValues(IOpenMethodHeader method, Object[] params, int mode) {
        return printMethodWithParameterValues(method, params, mode, new StringBuilder()).toString();
    }
    
    private static void startPrintingMethodName(String name, StringBuilder buf) {
        buf.append(name).append('(');
    }
    
    private static void endPrintingMethodName(StringBuilder buf) {
        buf.append(')');
    }
    private static void printParameterInfo(String type, String name, boolean isFirst, StringBuilder buf){
        printParameterInfo(type, name, null, isFirst, 0, buf);
    }
    
    private static void printParameterInfo(String type, String name, Object value, boolean isFirst, int displayMode, StringBuilder buf){
        if (!isFirst){
            buf.append(", ");
        }
        
        if (type != null){
            buf.append(type);
        }
        
        if (type != null && name != null){
            buf.append(' ');
        }
        
        if (name != null){
            buf.append(name);
        }
        
        if (value != null){
            buf.append(" = ");
            Formatter.format(value, displayMode, buf);
        }
    }
    
    public static Method getMatchingAccessibleMethod(Class<?> methodOwner, String methodName, Class<?>[] argTypes,
            boolean autoboxing) {
        Method resultMethod = null;
        Method[] methods = methodOwner.getMethods();
        for (Method method : methods) {
            Class<?>[] signatureParams = method.getParameterTypes();
            if (methodName.equals(method.getName()) && signatureParams.length == argTypes.length) {
                if (ClassUtils.isAssignable(argTypes, signatureParams, autoboxing)) {
                    method = MethodUtils.getAccessibleMethod(method);//kills inherited methods
                    if (method != null) {
                        if (resultMethod != null) {
                            resultMethod = getCloserMethod(resultMethod, method, argTypes, autoboxing);
                        } else {
                            resultMethod = method;
                        }
                    }
                }
            }
        }
        return resultMethod;
    }

    private static Method getCloserMethod(Method firstMethod, Method secondMethod, Class<?>[] argTypes,
            boolean autoboxing) {
        int firstTransfCount = getTransformationsCount(firstMethod.getParameterTypes(), argTypes, autoboxing);
        if (firstTransfCount < 0) {
            return secondMethod;
        }
        int secondTransfCount = getTransformationsCount(secondMethod.getParameterTypes(), argTypes, autoboxing);
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
     * @param autoboxing flag that indicates
     * @return <code>-1</code> if signature to check is not suitable for
     *         specified args and transformations count otherwise.
     */
    private static int getTransformationsCount(Class<?>[] signatureToCheck, Class<?>[] argTypes, boolean autoboxing) {
        int transformationsCount = 0;
        for (int i = 0; i < argTypes.length; i++) {
            if (!signatureToCheck[i].equals(argTypes[i])) {
                if (!ClassUtils.isAssignable(argTypes[i], signatureToCheck[i], autoboxing)) {
                    return -1;
                }
                transformationsCount++;
            }
        }
        return transformationsCount;
    }
}
