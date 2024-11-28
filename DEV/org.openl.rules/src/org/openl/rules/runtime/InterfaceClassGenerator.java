package org.openl.rules.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.classloader.ClassLoaderUtils;
import org.openl.gen.InterfaceByteCodeBuilder;
import org.openl.gen.MethodDescriptionBuilder;
import org.openl.gen.TypeDescription;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.util.ClassUtils;

public class InterfaceClassGenerator {

    private String[] includes;
    private String[] excludes;

    public InterfaceClassGenerator() {
    }

    public InterfaceClassGenerator(String[] includes, String[] excludes) {
        this.includes = Objects.requireNonNull(includes, "includes cannot be null");
        this.excludes = Objects.requireNonNull(excludes, "excludes cannot be null");
    }

    public Class<?> generateInterface(String className,
                                      IOpenClass openClass,
                                      ClassLoader classLoader) throws Exception {
        if (!className.contains(".")) {
            className = "org.openl.generated.interfaces." + className;
        }

        var classBuilder = InterfaceByteCodeBuilder.create(className);
        if (openClass == null) {
            return generateAndLoad(className, classLoader, classBuilder);
        }

        Set<MethodKey> methodsInClass = new HashSet<>();

        Map<IOpenClass, Boolean> validationMap = new HashMap<>();

        final Collection<IOpenMethod> methods = openClass.getMethods();
        for (IOpenMethod method : methods) {
            if (!isIgnoredMember(method, validationMap)) {
                var signature = method.getSignature();
                String name = method.getName();
                Class<?> returnType = method.getType().getInstanceClass();
                boolean isMember = isMember(name, returnType, signature.getParameterTypes());
                if (isMember) {
                    var methodBuilder = MethodDescriptionBuilder.create(name, returnType);
                    var pNum = signature.getNumberOfParameters();
                    for (int i = 0; i < pNum; i++) {
                        var paramName = signature.getParameterName(i);
                        var paramType = signature.getParameterType(i).getInstanceClass().getName();
                        methodBuilder.addParameterName(paramName);
                        methodBuilder.addParameter(new TypeDescription(paramType));
                    }
                    classBuilder.addAbstractMethod(methodBuilder.build());
                    methodsInClass.add(new MethodKey(method));
                }
            }
        }

        for (IOpenField field : openClass.getFields()) {
            if (!isIgnoredMember(field, validationMap) && field.isReadable()) {
                String name = ClassUtils.getter(field.getName());
                Class<?> returnType = field.getType().getInstanceClass();
                boolean isMember = isMember(name, returnType, IOpenClass.EMPTY);
                if (isMember) {
                    MethodKey key = new MethodKey(name, IOpenClass.EMPTY);
                    // Skip getter for field if method is defined with the same signature.
                    if (!methodsInClass.contains(key)) {
                        var methodBuilder = MethodDescriptionBuilder.create(name, returnType);
                        classBuilder.addAbstractMethod(methodBuilder.build());
                        methodsInClass.add(key);
                    }
                }
            }
        }

        return generateAndLoad(className, classLoader, classBuilder);
    }

    private static Class<?> generateAndLoad(String className, ClassLoader classLoader, InterfaceByteCodeBuilder builder) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        var bytecode = builder.buildJava().byteCode();

        // Create class object.
        //
        return ClassLoaderUtils.defineClass(className, bytecode, classLoader);
    }

    /**
     * Checks that given member is ignored.
     *
     * @param member member (method or field)
     * @return <code>true</code> - if member should be ignored (will be skipped due interface generation phase),
     * <code>false</code> - otherwise
     */
    private static boolean isIgnoredMember(IOpenMember member, Map<IOpenClass, Boolean> validationMap) {
        if (member instanceof DataOpenField) {
            DataOpenField dataOpenField = (DataOpenField) member;
            if (XlsNodeTypes.XLS_RUN_METHOD.equals(dataOpenField.getNodeType()) || XlsNodeTypes.XLS_TEST_METHOD
                    .equals(dataOpenField.getNodeType())) {
                return true;
            }
        }
        if (isInvalidType(member.getType(), validationMap)) {
            return true;
        }
        if (member instanceof IOpenMethod) {
            IOpenMethod openMethod = (IOpenMethod) member;
            for (IOpenClass parameterType : openMethod.getSignature().getParameterTypes()) {
                if (isInvalidType(parameterType, validationMap)) {
                    return true;
                }
            }
        }
        return member instanceof JavaOpenConstructor || member instanceof ComponentOpenClass.ThisField || member instanceof ComponentOpenClass.GetOpenClass || member instanceof TestSuiteMethod;
    }

    private static boolean isInvalidType(IOpenClass openClass, Map<IOpenClass, Boolean> invalidTypeMap) {
        Boolean v = invalidTypeMap.get(openClass);
        if (v != null) {
            return v;
        }
        invalidTypeMap.put(openClass, Boolean.FALSE);
        if (openClass.isArray()) {
            boolean isInvalidComponentType = isInvalidType(openClass.getComponentClass(), invalidTypeMap);
            invalidTypeMap.put(openClass, isInvalidComponentType);
            return isInvalidComponentType;
        }
        if (openClass instanceof DatatypeOpenClass) {
            if (openClass.getInstanceClass() == null) {
                invalidTypeMap.put(openClass, Boolean.TRUE);
                return true;
            }
            for (IOpenField openField : openClass.getFields()) {
                if (isInvalidType(openField.getType(), invalidTypeMap)) {
                    invalidTypeMap.put(openClass, Boolean.TRUE);
                    return true;
                }
            }
        }
        if (NullOpenClass.isAnyNull(openClass)) {
            invalidTypeMap.put(openClass, Boolean.TRUE);
            return true;
        }
        return false;
    }

    private boolean isMember(String name, Class<?> returnType, IOpenClass[] parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.getCanonicalName());
        sb.append(" ").append(name).append("(");
        boolean first = true;
        for (IOpenClass paramType : parameterTypes) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(paramType.getInstanceClass().getCanonicalName());
        }
        sb.append(")");
        var methodSignature = sb.toString();

        boolean isMember = true;
        if (includes != null && includes.length > 0) {
            isMember = false;
            for (String pattern : includes) {
                if (Pattern.matches(pattern, methodSignature)) {
                    isMember = true;
                }
            }
        }
        if (excludes != null && excludes.length > 0 && isMember) {
            for (String pattern : excludes) {
                if (Pattern.matches(pattern, methodSignature)) {
                    isMember = false;
                }
            }
        }
        return isMember;
    }
}
