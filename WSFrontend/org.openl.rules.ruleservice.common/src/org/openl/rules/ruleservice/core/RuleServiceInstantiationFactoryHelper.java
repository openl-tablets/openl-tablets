package org.openl.rules.ruleservice.core;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.AnySpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanClass;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.ruleservice.core.annotations.BeanToSpreadsheetResultConvert;
import org.openl.rules.ruleservice.core.annotations.ExternalParam;
import org.openl.rules.ruleservice.core.annotations.NoTypeConversion;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAroundAdvice;
import org.openl.rules.ruleservice.core.interceptors.annotations.NotConvertor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.TypeResolver;
import org.openl.rules.ruleservice.core.interceptors.annotations.UseOpenMethodReturnType;
import org.openl.rules.ruleservice.core.interceptors.converters.SPRToPlainConverterAdvice;
import org.openl.rules.ruleservice.core.interceptors.converters.VariationResultSPRToPlainConverterAdvice;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.variation.VariationsResult;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;

public final class RuleServiceInstantiationFactoryHelper {
    private RuleServiceInstantiationFactoryHelper() {
        // Hidden constructor
    }

    /**
     * Special ClassVisitor to generate interface with {@link Object} as the return type for methods that have "after
     * interceptors".
     *
     * @author PUdalau
     */
    private static class RuleServiceInterceptorsSupportClassVisitor extends ClassVisitor {
        private final Map<String, List<Pair<Method, MethodSignatureChanges>>> methodsWithSignatureNeedsChange;
        private final Map<String, List<Method>> methodsToRemove;

        /**
         * Constructs instance with delegated {@link ClassVisitor} and set of methods.
         *
         * @param visitor delegated {@link ClassVisitor}.
         * @param methodsWithSignatureNeedsChange Methods where to change return type.
         */
        private RuleServiceInterceptorsSupportClassVisitor(ClassVisitor visitor,
                Map<Method, MethodSignatureChanges> methodsWithSignatureNeedsChange,
                Collection<Method> methodsToRemove) {
            super(Opcodes.ASM5, visitor);
            Objects.requireNonNull(methodsWithSignatureNeedsChange, "methodsWithSignatureNeedsChange cannot be null");
            this.methodsWithSignatureNeedsChange = new HashMap<>();
            // Build map by method name to improve performance of the method search loop
            for (Entry<Method, MethodSignatureChanges> entry : methodsWithSignatureNeedsChange.entrySet()) {
                List<Pair<Method, MethodSignatureChanges>> listOfMethods = this.methodsWithSignatureNeedsChange
                    .computeIfAbsent(entry.getKey().getName(), e -> new ArrayList<>());
                listOfMethods.add(Pair.of(entry.getKey(), entry.getValue()));
            }
            Objects.requireNonNull(methodsToRemove, "methodsToRemove cannot be null");
            this.methodsToRemove = new HashMap<>();
            // Build map by method name to improve performance of the method search loop
            for (Method method : methodsToRemove) {
                List<Method> listOfMethods = this.methodsToRemove.computeIfAbsent(method.getName(),
                    e -> new ArrayList<>());
                listOfMethods.add(method);
            }

        }

        @Override
        public MethodVisitor visitMethod(final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            List<Method> listOfMethodsToRemove = methodsToRemove.get(name);
            if (listOfMethodsToRemove != null) {
                for (Method method : listOfMethodsToRemove) {
                    if (descriptor.equals(Type.getMethodDescriptor(method))) {
                        return null;
                    }
                }
            }
            List<Pair<Method, MethodSignatureChanges>> listOfMethods = methodsWithSignatureNeedsChange.get(name);
            if (listOfMethods != null) {
                for (Pair<Method, MethodSignatureChanges> entry : listOfMethods) {
                    Method method = entry.getKey();
                    if (descriptor.equals(Type.getMethodDescriptor(method))) {
                        Pair<Class<?>, Boolean>[] newParamTypes = entry.getValue().getNewParamTypes();
                        Class<?> newRetType = entry.getValue().getReturnType();
                        MethodVisitor mv = super.visitMethod(access,
                            name,
                            Type.getMethodDescriptor(
                                newRetType != null ? Type.getType(newRetType) : Type.getReturnType(descriptor),
                                newParamTypes != null ? Arrays.stream(newParamTypes)
                                    .map(Pair::getLeft)
                                    .map(Type::getType)
                                    .toArray(Type[]::new) : Type.getArgumentTypes(descriptor)),
                            signature,
                            exceptions);
                        if (newRetType != null && entry.getValue().isGenerateReturnConverters()) {
                            AnnotationVisitor av = mv
                                .visitAnnotation(Type.getDescriptor(ServiceCallAfterInterceptor.class), true);
                            AnnotationVisitor av1 = av.visitArray("value");
                            av1.visit("value",
                                Type.getType(VariationsResult.class
                                    .equals(newRetType) ? VariationResultSPRToPlainConverterAdvice.class
                                                        : SPRToPlainConverterAdvice.class));
                            av1.visitEnd();
                            av.visitEnd();
                        }
                        if (newParamTypes != null) {
                            for (int i = 0; i < newParamTypes.length; i++) {
                                if (Boolean.TRUE.equals(newParamTypes[i].getValue())) {
                                    AnnotationVisitor av = mv.visitParameterAnnotation(i,
                                        Type.getDescriptor(BeanToSpreadsheetResultConvert.class),
                                        true);
                                    av.visitEnd();
                                }
                            }
                        }
                        return mv;
                    }
                }
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$Original";

    /**
     * Returns service class for instantiation strategy according to after interceptors of methods in service class of
     * service specified as the argument.
     *
     * @param serviceClass Interface for service, which will be used for service class creation.
     * @return Service class for instantiation strategy based on service class for service.
     */
    public static Class<?> buildInterfaceForInstantiationStrategy(Class<?> serviceClass,
            ClassLoader classLoader,
            Object serviceTarget,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        return processInterface(null,
            serviceClass,
            false,
            classLoader,
            serviceTarget,
            provideRuntimeContext,
            provideVariations);
    }

    public static Class<?> buildInterfaceForService(IOpenClass openClass,
            Class<?> serviceClass,
            ClassLoader classLoader,
            Object serviceTarget,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        return processInterface(openClass,
            serviceClass,
            true,
            classLoader,
            serviceTarget,
            provideRuntimeContext,
            provideVariations);
    }

    private static Class<?> processInterface(IOpenClass openClass,
            Class<?> serviceClass,
            boolean toServiceClass,
            ClassLoader classLoader,
            Object serviceTarget,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");

        Map<Method, MethodSignatureChanges> methodsWithSignatureNeedsChange = new HashMap<>();
        Set<Method> methodsToRemove = new HashSet<>();
        for (Method method : serviceClass.getMethods()) {
            if (ITableProperties.class.isAssignableFrom(method.getReturnType())
                    || (!toServiceClass && method.isAnnotationPresent(ServiceExtraMethod.class))) {
                methodsToRemove.add(method);
            } else {
                var changes = getMethodSignatureChanges(method, openClass, classLoader, toServiceClass, serviceTarget, provideRuntimeContext, provideVariations);
                if (changes != null) {
                    methodsWithSignatureNeedsChange.put(method, changes);
                }
            }
        }

        if (methodsWithSignatureNeedsChange.isEmpty() && methodsToRemove.isEmpty()) {
            return serviceClass;
        } else {
            ClassWriter classWriter = new ClassWriter(0);
            ClassVisitor classVisitor = new RuleServiceInterceptorsSupportClassVisitor(classWriter,
                methodsWithSignatureNeedsChange,
                methodsToRemove);
            String className = serviceClass.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
            InterfaceTransformer transformer = toServiceClass ? new InterfaceTransformer(serviceClass, className)
                                                              : new InterfaceTransformer(serviceClass,
                                                                  className,
                                                                  InterfaceTransformer.IGNORE_PARAMETER_ANNOTATIONS);
            transformer.accept(classVisitor);
            classWriter.visitEnd();
            try {
                // Create class object.
                //
                return ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);
            } catch (Exception e) {
                throw new OpenlNotCheckedException(e);
            }
        }
    }

    private static Class<? extends ServiceMethodAfterAdvice<?>> getLastServiceMethodAfterAdvice(
            ServiceCallAfterInterceptor serviceCallAfterInterceptor) {
        Class<? extends ServiceMethodAfterAdvice<?>>[] interceptors = serviceCallAfterInterceptor.value();
        int i = interceptors.length - 1;
        while (i >= 0) {
            Class<? extends ServiceMethodAfterAdvice<?>> serviceMethodAfterAdvice = interceptors[i];
            if (!serviceMethodAfterAdvice.isAnnotationPresent(NotConvertor.class)) {
                return serviceMethodAfterAdvice;
            }
            i--;
        }
        return null;
    }

    private static Class<?> resolveNewMethodReturnType(IOpenClass openClass,
            Method method,
            IOpenMember openMember,
            ClassLoader classLoader,
            boolean toServiceClass,
            boolean provideVariations) {
        if (toServiceClass && method.isAnnotationPresent(RulesType.class)) {
            RulesType rulesType = method.getAnnotation(RulesType.class);
            try {
                Class<?> loadedType = findOrLoadType(rulesType, openClass, classLoader);
                Class<?> t = method.getReturnType();
                while (t.isArray()) {
                    t = t.getComponentType();
                    loadedType = Array.newInstance(loadedType, 0).getClass();
                }
                return loadedType;
            } catch (ClassNotFoundException e) {
                throw new InstantiationException(
                    String.format("Failed to load type '%s' that used in @RulesType annotation.", rulesType.value()));
            }
        }
        ServiceCallAfterInterceptor serviceCallAfterInterceptor = method
            .getAnnotation(ServiceCallAfterInterceptor.class);
        if (serviceCallAfterInterceptor != null && (!provideVariations || !method.getReturnType()
            .equals(VariationsResult.class))) {
            Class<? extends ServiceMethodAfterAdvice<?>> lastServiceMethodAfterAdvice = getLastServiceMethodAfterAdvice(
                serviceCallAfterInterceptor);
            if (lastServiceMethodAfterAdvice != null) {
                return extractReturnTypeForMethod(openMember, toServiceClass, lastServiceMethodAfterAdvice);
            }
        }
        ServiceCallAroundInterceptor serviceCallAroundInterceptor = method
            .getAnnotation(ServiceCallAroundInterceptor.class);
        if (serviceCallAroundInterceptor != null && (!provideVariations || !method.getReturnType()
            .equals(VariationsResult.class))) {
            Class<? extends ServiceMethodAroundAdvice<?>> serviceMethodAroundAdvice = serviceCallAroundInterceptor
                .value();
            return extractReturnTypeForMethod(openMember, toServiceClass, serviceMethodAroundAdvice);
        }
        return null;
    }

    public static Class<?> findOrLoadType(RulesType rulesType,
            IOpenClass openClass,
            ClassLoader classLoader) throws ClassNotFoundException {
        String typeName = rulesType.value();
        try {
            return classLoader.loadClass(typeName);
        } catch (ClassNotFoundException e) {
            for (IOpenClass type : openClass.getTypes()) {
                if (Objects.equals(type.getName(), typeName)) {
                    return type.getInstanceClass();
                }
            }
            List<CustomSpreadsheetResultOpenClass> sprTypes = openClass.getTypes()
                .stream()
                .filter(CustomSpreadsheetResultOpenClass.class::isInstance)
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .filter(CustomSpreadsheetResultOpenClass::isGenerateBeanClass)
                .collect(Collectors.toList());

            for (CustomSpreadsheetResultOpenClass sprType : sprTypes) {
                if (Objects.equals(sprType.getBeanClass().getName(), typeName)) {
                    return sprType.getBeanClass();
                }
            }
            for (CustomSpreadsheetResultOpenClass sprType : sprTypes) {
                if (Objects.equals(sprType.getBeanClass().getSimpleName(), typeName)) {
                    return sprType.getBeanClass();
                }
            }
            throw e;
        }
    }

    static Map<Method, Method> getMethodMap(Class<?> serviceClass, Class<?> serviceTargetClass, Object serviceTarget) {
        var methodMap = new HashMap<Method, Method>();
        for (Method method : serviceClass.getMethods()) {
            Pair<IOpenMember, Class<?>[]> openMemberResolved = findIOpenMember(serviceTarget, method);
            IOpenMember openMember = openMemberResolved.getLeft();
            Method serviceTargetMethod = null;
            if (openMember != null) {
                serviceTargetMethod = MethodUtil.getMatchingAccessibleMethod(serviceTargetClass,
                        method.getName(),
                        openMemberResolved.getRight());
            }
            methodMap.put(method, serviceTargetMethod);
        }
        return methodMap;
    }

    private static Pair<IOpenMember, Class<?>[]> findIOpenMember(Object serviceTarget, Method method) {
        for (Class<?> clazz : serviceTarget.getClass().getInterfaces()) {
            try {
                Class<?>[] parameterTypes = method.getParameterTypes();
                int i = 0;
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(ExternalParam.class)) {
                        parameterTypes[i] = null;
                    } else if (parameter.isAnnotationPresent(BeanToSpreadsheetResultConvert.class)) {
                        Class<?> t = parameterTypes[i];
                        int dim = 0;
                        while (t.isArray()) {
                            t = t.getComponentType();
                            dim++;
                        }
                        if (t.isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
                            parameterTypes[i] = dim > 0 ? Array.newInstance(SpreadsheetResult.class, dim).getClass()
                                    : SpreadsheetResult.class;
                        }
                    }
                    i++;
                }
                parameterTypes = Arrays.stream(parameterTypes).filter(Objects::nonNull).toArray(Class<?>[]::new);
                Method m = clazz.getMethod(method.getName(), parameterTypes);
                return Pair.of(RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, serviceTarget),
                        parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return Pair.of(null, null);
    }

    private static Class<?> extractReturnTypeForMethod(IOpenMember openMember,
            boolean toServiceClass,
            Class<? extends ServiceMethodAdvice> serviceMethodAdvice) {
        if (toServiceClass) {
            UseOpenMethodReturnType useOpenMethodReturnType = serviceMethodAdvice
                .getAnnotation(UseOpenMethodReturnType.class);
            if (useOpenMethodReturnType != null) {
                return extractOpenMethodReturnType(openMember, useOpenMethodReturnType.value());
            }
            return null;
        } else {
            return Object.class;
        }
    }

    private static Class<?> extractOpenMethodReturnType(IOpenMember openMember, TypeResolver typeResolver) {
        IOpenClass returnType = openMember.getType();
        switch (typeResolver) {
            case ORIGINAL:
                return returnType.getInstanceClass();
            case IF_SPR_TO_PLAIN:
                IOpenClass type = returnType;
                int dim = 0;
                while (type.isArray()) {
                    type = type.getComponentClass();
                    dim++;
                }
                if (type instanceof CustomSpreadsheetResultOpenClass && ((CustomSpreadsheetResultOpenClass) type)
                    .isGenerateBeanClass()) {
                    Class<?> t = ((CustomSpreadsheetResultOpenClass) type).getBeanClass();
                    return dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                } else if (type instanceof SpreadsheetResultOpenClass) {
                    Class<?> t;
                    // Check: custom spreadsheet is enabled
                    if (((SpreadsheetResultOpenClass) type).getModule() != null) {
                        t = ((SpreadsheetResultOpenClass) type).toCustomSpreadsheetResultOpenClass().getBeanClass();
                    } else {
                        t = type.getInstanceClass();
                    }
                    return dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                } else {
                    return returnType.getInstanceClass();
                }
            default:
                throw new IllegalStateException();
        }
    }

    private static boolean isTypeChangingAnnotationPresent(Method method) {
        return method.isAnnotationPresent(ServiceCallAfterInterceptor.class) || method
            .isAnnotationPresent(ServiceCallAroundInterceptor.class);
    }

    private static MethodSignatureChanges getMethodSignatureChanges(Method method,
            IOpenClass openClass,
            ClassLoader classLoader,
            boolean toServiceClass,
            Object serviceTarget,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        MethodSignatureChanges changes;
        IOpenMember openMember = null;
        if (toServiceClass && !method.isAnnotationPresent(ServiceExtraMethod.class)) {
            List<Class<?>> parameterTypes = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                if (!parameter.isAnnotationPresent(ExternalParam.class)) {
                    parameterTypes.add(parameter.getType());
                }
            }
            openMember = RuleServiceOpenLServiceInstantiationHelper
                .getOpenMember(method.getName(), parameterTypes.toArray(new Class<?>[0]), serviceTarget);
            if (openMember == null) {
                throw new IllegalStateException("Open member is not found.");
            }
        }
        Pair<Class<?>, Boolean>[] newParamTypes = resolveNewMethodParamTypes(method,
                openClass,
                classLoader,
            openMember,
                toServiceClass,
                provideRuntimeContext);
        Class<?> newReturnType = resolveNewMethodReturnType(openClass,
                method,
            openMember,
                classLoader,
                toServiceClass,
                provideVariations);
        if (newReturnType != null) {
            changes = new MethodSignatureChanges(newParamTypes, newReturnType, false);
        } else if (openMember != null && !isTypeChangingAnnotationPresent(method)) {
            IOpenClass type = openMember.getType();
            int dim = 0;
            while (type.isArray()) {
                type = type.getComponentClass();
                dim++;
            }
            if (provideVariations && method.getReturnType().equals(VariationsResult.class)) {
                changes = new MethodSignatureChanges(newParamTypes, VariationsResult.class, true);
            } else if (type instanceof CustomSpreadsheetResultOpenClass || type instanceof SpreadsheetResultOpenClass || type instanceof AnySpreadsheetResultOpenClass) {
                Class<?> t;
                if (type instanceof CustomSpreadsheetResultOpenClass && ((CustomSpreadsheetResultOpenClass) type)
                    .isGenerateBeanClass()) {
                    t = ((CustomSpreadsheetResultOpenClass) type).getBeanClass();
                } else if (type instanceof SpreadsheetResultOpenClass && ((SpreadsheetResultOpenClass) type)
                    .getModule() != null) {
                    t = ((SpreadsheetResultOpenClass) type).toCustomSpreadsheetResultOpenClass().getBeanClass();
                } else {
                    t = Map.class;
                }
                if (dim > 0) {
                    t = Array.newInstance(t, new int[dim]).getClass();
                }
                changes = new MethodSignatureChanges(newParamTypes, t, true);
            } else if (JavaOpenClass.OBJECT.equals(type) && !JavaOpenClass.OBJECT.equals(openMember.getType())) {
                changes = new MethodSignatureChanges(newParamTypes, openMember.getType().getInstanceClass(), true);
            } else if (newParamTypes != null) {
                changes = new MethodSignatureChanges(newParamTypes, null, false);
            } else {
                changes = null;
            }
        } else if (newParamTypes != null) {
            changes = new MethodSignatureChanges(newParamTypes, null, false);
        } else {
            changes = null;
        }
        return changes;
    }

    @SuppressWarnings("unchecked")
    private static Pair<Class<?>, Boolean>[] resolveNewMethodParamTypes(Method method,
            IOpenClass openClass,
            ClassLoader classLoader,
            IOpenMember openMember,
            boolean toServiceClass,
            boolean provideRuntimeContext) {
        List<Pair<Class<?>, Boolean>> methodParamTypes = new ArrayList<>();
        boolean f = false;
        int i = 0;
        for (Parameter parameter : method.getParameters()) {
            if (!toServiceClass && parameter.isAnnotationPresent(ExternalParam.class)) {
                f = true;
            } else {
                Class<?> methodParamType = method.getParameterTypes()[i];
                Boolean paramTypeSprToBeanConversation = Boolean.FALSE;
                if (parameter.isAnnotationPresent(RulesType.class)) {
                    RulesType rulesType = parameter.getAnnotation(RulesType.class);
                    try {
                        Class<?> loadedType = findOrLoadType(rulesType, openClass, classLoader);
                        Class<?> t = method.getParameterTypes()[i];
                        while (t.isArray()) {
                            t = t.getComponentType();
                            loadedType = Array.newInstance(loadedType, 0).getClass();
                        }
                        methodParamType = loadedType;
                        f = true;
                    } catch (ClassNotFoundException e) {
                        throw new InstantiationException(String
                            .format("Failed to load type '%s' that used in @RulesType annotation.", rulesType.value()));
                    }
                } else {
                    Class<?> baseParameterType = parameter.getType();
                    int dim = 0;
                    while (baseParameterType.isArray()) {
                        baseParameterType = baseParameterType.getComponentType();
                        dim++;
                    }
                    if (toServiceClass && openMember instanceof IOpenMethod && baseParameterType.isAssignableFrom(
                        SpreadsheetResult.class) && !parameter.isAnnotationPresent(NoTypeConversion.class)) {
                        IOpenMethod openMethod = (IOpenMethod) openMember;
                        if ((!provideRuntimeContext || i > 0) && i - (provideRuntimeContext ? 1 : 0) < openMethod
                            .getSignature()
                            .getNumberOfParameters()) {
                            IOpenClass baseOpenParameterType = openMethod.getSignature()
                                .getParameterType(i - (provideRuntimeContext ? 1 : 0));
                            int d = 0;
                            while (baseOpenParameterType.isArray()) {
                                baseOpenParameterType = baseOpenParameterType.getComponentClass();
                                d++;
                            }
                            if (dim != d) {
                                throw new InstantiationException(String.format(
                                    "Unexpected array dimension size for '%s' method parameter '%s'. Expected dimension size is '%s', but found '%s'.",
                                    MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                                    i,
                                    d,
                                    dim));
                            }
                            if (baseOpenParameterType instanceof CustomSpreadsheetResultOpenClass || baseOpenParameterType instanceof SpreadsheetResultOpenClass) {
                                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;
                                if (baseOpenParameterType instanceof CustomSpreadsheetResultOpenClass) {
                                    customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) baseOpenParameterType;
                                } else {
                                    customSpreadsheetResultOpenClass = ((SpreadsheetResultOpenClass) baseOpenParameterType)
                                        .toCustomSpreadsheetResultOpenClass();
                                }
                                if (customSpreadsheetResultOpenClass.isGenerateBeanClass() && parameter
                                    .getType() != customSpreadsheetResultOpenClass.getBeanClass()) {
                                    Class<?> t = customSpreadsheetResultOpenClass.getBeanClass();
                                    methodParamType = dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                                    paramTypeSprToBeanConversation = Boolean.TRUE;
                                    f = true;
                                }
                            }
                        }
                    } else if (!toServiceClass && !parameter
                        .isAnnotationPresent(NoTypeConversion.class) && baseParameterType
                            .isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
                        methodParamType = dim > 0 ? Array.newInstance(SpreadsheetResult.class, dim).getClass()
                                                  : SpreadsheetResult.class;
                        paramTypeSprToBeanConversation = Boolean.TRUE;
                        f = true;
                    }
                }
                methodParamTypes.add(Pair.of(methodParamType, paramTypeSprToBeanConversation));
            }
            i++;
        }
        return f ? (Pair<Class<?>, Boolean>[]) methodParamTypes.toArray(new Pair[0]) : null;
    }

    private static class MethodSignatureChanges {
        boolean generateReturnConverters;
        Pair<Class<?>, Boolean>[] newParamTypes;
        Class<?> returnType;

        public MethodSignatureChanges(Pair<Class<?>, Boolean>[] newParamTypes,
                Class<?> returnType,
                boolean generateReturnConverters) {
            this.newParamTypes = newParamTypes;
            this.generateReturnConverters = generateReturnConverters;
            this.returnType = returnType;
        }

        public Pair<Class<?>, Boolean>[] getNewParamTypes() {
            return newParamTypes;
        }

        public boolean isGenerateReturnConverters() {
            return generateReturnConverters;
        }

        public Class<?> getReturnType() {
            return returnType;
        }
    }

}
