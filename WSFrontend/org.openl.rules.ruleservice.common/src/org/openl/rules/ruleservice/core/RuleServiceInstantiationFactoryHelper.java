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

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.openl.binding.MethodUtil;
import org.openl.classloader.ClassLoaderUtils;
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
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
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
         * @param visitor                         delegated {@link ClassVisitor}.
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
                var listOfMethods = this.methodsWithSignatureNeedsChange
                        .computeIfAbsent(entry.getKey().getName(), e -> new ArrayList<>());
                listOfMethods.add(Pair.of(entry.getKey(), entry.getValue()));
            }
            Objects.requireNonNull(methodsToRemove, "methodsToRemove cannot be null");
            this.methodsToRemove = new HashMap<>();
            // Build map by method name to improve performance of the method search loop
            for (Method method : methodsToRemove) {
                var listOfMethods = this.methodsToRemove.computeIfAbsent(method.getName(),
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
            if (isMethodToRemove(name, descriptor)) {
                return null;
            }
            List<Pair<Method, MethodSignatureChanges>> listOfMethods = methodsWithSignatureNeedsChange.get(name);
            if (listOfMethods != null) {
                for (Pair<Method, MethodSignatureChanges> entry : listOfMethods) {
                    var method = entry.getKey();
                    if (descriptor.equals(Type.getMethodDescriptor(method))) {
                        return visitChangedMethod(access, name, descriptor, signature, exceptions, entry.getValue());
                    }
                }
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        private boolean isMethodToRemove(String name, String descriptor) {
            List<Method> listOfMethodsToRemove = methodsToRemove.get(name);
            if (listOfMethodsToRemove != null) {
                for (Method method : listOfMethodsToRemove) {
                    if (descriptor.equals(Type.getMethodDescriptor(method))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private MethodVisitor visitChangedMethod(int access,
                                                 String name,
                                                 String descriptor,
                                                 String signature,
                                                 String[] exceptions,
                                                 MethodSignatureChanges changes) {
            var newParamTypes = changes.getNewParamTypes();
            Class<?> newRetType = changes.getReturnType();
            var mv = super.visitMethod(access,
                    name,
                    Type.getMethodDescriptor(
                            newRetType != null ? Type.getType(newRetType) : Type.getReturnType(descriptor),
                            newParamTypes != null ? Arrays.stream(newParamTypes)
                                    .map(Pair::getLeft)
                                    .map(Type::getType)
                                    .toArray(Type[]::new) : Type.getArgumentTypes(descriptor)),
                    signature,
                    exceptions);
            if (newRetType != null && changes.isGenerateReturnConverters()) {
                var av = mv
                        .visitAnnotation(Type.getDescriptor(ServiceCallAfterInterceptor.class), true);
                var av1 = av.visitArray("value");
                av1.visit("value",
                        Type.getType(SPRToPlainConverterAdvice.class));
                av1.visitEnd();
                av.visitEnd();
            }
            if (newParamTypes != null) {
                for (var i = 0; i < newParamTypes.length; i++) {
                    if (Boolean.TRUE.equals(newParamTypes[i].getValue())) {
                        var av = mv.visitParameterAnnotation(i,
                                Type.getDescriptor(BeanToSpreadsheetResultConvert.class),
                                true);
                        av.visitEnd();
                    }
                }
            }
            return mv;
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
                                                                  boolean provideRuntimeContext) {
        return processInterface(null,
                serviceClass,
                false,
                classLoader,
                serviceTarget,
                provideRuntimeContext);
    }

    public static Class<?> buildInterfaceForService(IOpenClass openClass,
                                                    Class<?> serviceClass,
                                                    ClassLoader classLoader,
                                                    Object serviceTarget,
                                                    boolean provideRuntimeContext) {
        return processInterface(openClass,
                serviceClass,
                true,
                classLoader,
                serviceTarget,
                provideRuntimeContext);
    }

    private static Class<?> processInterface(IOpenClass openClass,
                                             Class<?> serviceClass,
                                             boolean toServiceClass,
                                             ClassLoader classLoader,
                                             Object serviceTarget,
                                             boolean provideRuntimeContext) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");

        var methodsWithSignatureNeedsChange = new HashMap<Method, MethodSignatureChanges>();
        var methodsToRemove = new HashSet<Method>();
        for (Method method : serviceClass.getMethods()) {
            if (ITableProperties.class.isAssignableFrom(method.getReturnType())
                    || (!toServiceClass && method.isAnnotationPresent(ServiceExtraMethod.class))) {
                methodsToRemove.add(method);
            } else {
                var changes = getMethodSignatureChanges(method, openClass, classLoader, toServiceClass, serviceTarget, provideRuntimeContext);
                if (changes != null) {
                    methodsWithSignatureNeedsChange.put(method, changes);
                }
            }
        }

        if (methodsWithSignatureNeedsChange.isEmpty() && methodsToRemove.isEmpty()) {
            return serviceClass;
        }
        var classWriter = new ClassWriter(0);
        var classVisitor = new RuleServiceInterceptorsSupportClassVisitor(classWriter,
                methodsWithSignatureNeedsChange,
                methodsToRemove);
        var className = serviceClass.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
        InterfaceTransformer transformer = toServiceClass ? new InterfaceTransformer(serviceClass, className)
                : new InterfaceTransformer(serviceClass,
                className,
                InterfaceTransformer.IGNORE_PARAMETER_ANNOTATIONS);
        transformer.accept(classVisitor);
        classWriter.visitEnd();
        try {
            // Create class object.
            //
            return ClassLoaderUtils.defineClass(className, classWriter.toByteArray(), classLoader);
        } catch (Exception e) {
            throw new OpenlNotCheckedException(e);
        }
    }

    private static Class<? extends ServiceMethodAfterAdvice<?>> getLastServiceMethodAfterAdvice(
            ServiceCallAfterInterceptor serviceCallAfterInterceptor) {
        var interceptors = serviceCallAfterInterceptor.value();
        var i = interceptors.length - 1;
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
                                                       boolean toServiceClass) {
        if (toServiceClass && method.isAnnotationPresent(RulesType.class)) {
            var rulesType = method.getAnnotation(RulesType.class);
            Class<?> originType = method.getReturnType();
            return findOrLoadType(openClass, classLoader, rulesType, originType);
        }
        var serviceCallAfterInterceptor = method
                .getAnnotation(ServiceCallAfterInterceptor.class);
        if (serviceCallAfterInterceptor != null) {
            var lastServiceMethodAfterAdvice = getLastServiceMethodAfterAdvice(
                    serviceCallAfterInterceptor);
            if (lastServiceMethodAfterAdvice != null) {
                return extractReturnTypeForMethod(openMember, toServiceClass, lastServiceMethodAfterAdvice);
            }
        }
        var serviceCallAroundInterceptor = method
                .getAnnotation(ServiceCallAroundInterceptor.class);
        if (serviceCallAroundInterceptor != null) {
            Class<? extends ServiceMethodAroundAdvice<?>> serviceMethodAroundAdvice = serviceCallAroundInterceptor
                    .value();
            return extractReturnTypeForMethod(openMember, toServiceClass, serviceMethodAroundAdvice);
        }
        return null;
    }

    private static Class<?> findOrLoadType(IOpenClass openClass, ClassLoader classLoader, RulesType rulesType, Class<?> originType) {
        try {
            var loadedType = findOrLoadType(rulesType, openClass, classLoader);
            Class<?> t = originType;
            while (t.isArray()) {
                t = t.getComponentType();
                loadedType = Array.newInstance(loadedType, 0).getClass();
            }
            return loadedType;
        } catch (ClassNotFoundException e) {
            throw new InstantiationException(
                    "Failed to load type '%s' that used in @RulesType annotation.".formatted(rulesType.value()));
        }
    }

    public static Class<?> findOrLoadType(RulesType rulesType,
                                          IOpenClass openClass,
                                          ClassLoader classLoader) throws ClassNotFoundException {
        var typeName = rulesType.value();
        try {
            return classLoader.loadClass(typeName);
        } catch (ClassNotFoundException e) {
            return findModuleType(openClass, typeName, e);
        }
    }

    /**
     * Finds a module type or a spreadsheet result bean class with the given name. Throws the given exception when
     * nothing matches.
     */
    private static Class<?> findModuleType(IOpenClass openClass,
                                           String typeName,
                                           ClassNotFoundException notFound) throws ClassNotFoundException {
        for (IOpenClass type : openClass.getTypes()) {
            if (Objects.equals(type.getName(), typeName)) {
                return type.getInstanceClass();
            }
        }
        var sprTypes = openClass.getTypes()
                .stream()
                .filter(CustomSpreadsheetResultOpenClass.class::isInstance)
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .toList();

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
        throw notFound;
    }

    static Map<Method, Method> getMethodMap(Class<?> serviceClass, Class<?> serviceTargetClass, Object serviceTarget, ClassLoader serviceClassLoader, IOpenClass openClass) {
        var methodMap = new HashMap<Method, Method>();
        for (Method method : serviceClass.getMethods()) {
            var openMemberResolved = findIOpenMember(serviceTarget, method, serviceClassLoader, openClass);
            var openMember = openMemberResolved.getLeft();
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

    private static Pair<IOpenMember, Class<?>[]> findIOpenMember(Object serviceTarget, Method method, ClassLoader serviceClassLoader, IOpenClass openClass) {
        for (Class<?> clazz : serviceTarget.getClass().getInterfaces()) {
            try {
                var parameterTypes = method.getParameterTypes();
                var i = 0;
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(ExternalParam.class)) {
                        parameterTypes[i] = null;
                    } else if (parameter.isAnnotationPresent(BeanToSpreadsheetResultConvert.class)) {
                        parameterTypes[i] = RuleServiceOpenLServiceInstantiationHelper.spreadsheetResultTypeOf(parameterTypes[i]);
                    } else if (parameter.isAnnotationPresent(RulesType.class)) {
                        parameterTypes[i] = findOrLoadType(openClass, serviceClassLoader, parameter.getAnnotation(RulesType.class), parameterTypes[i]);
                    }
                    i++;
                }
                parameterTypes = Arrays.stream(parameterTypes).filter(Objects::nonNull).toArray(Class<?>[]::new);
                var m = clazz.getMethod(method.getName(), parameterTypes);
                return Pair.of(RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, serviceTarget),
                        parameterTypes);
            } catch (NoSuchMethodException ignored) {
                // method not found on this candidate class; continue searching
            }
        }
        return Pair.of(null, null);
    }

    private static Class<?> extractReturnTypeForMethod(IOpenMember openMember,
                                                       boolean toServiceClass,
                                                       Class<? extends ServiceMethodAdvice> serviceMethodAdvice) {
        if (toServiceClass) {
            var useOpenMethodReturnType = serviceMethodAdvice
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
        var returnType = openMember.getType();
        switch (typeResolver) {
            case ORIGINAL -> {
                return returnType.getInstanceClass();
            }
            case IF_SPR_TO_PLAIN -> {
                var type = returnType;
                var dim = 0;
                while (type.isArray()) {
                    type = type.getComponentClass();
                    dim++;
                }
                return switch (type) {
                    case CustomSpreadsheetResultOpenClass class2 -> {
                        Class<?> t = class2.getBeanClass();
                        yield dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                    }
                    case SpreadsheetResultOpenClass class1 -> {
                        Class<?> t;
                        // Check: custom spreadsheet is enabled
                        if (class1.getModule() != null) {
                            t = class1.toCustomSpreadsheetResultOpenClass().getBeanClass();
                        } else {
                            t = type.getInstanceClass();
                        }
                        yield dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                    }
                    default -> returnType.getInstanceClass();
                };
            }
            default -> throw new IllegalStateException();
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
                                                                    boolean provideRuntimeContext) {
        MethodSignatureChanges changes;
        IOpenMember openMember = null;
        if (toServiceClass && !method.isAnnotationPresent(ServiceExtraMethod.class)) {
            openMember = findServiceOpenMember(method, openClass, classLoader, serviceTarget);
        }
        Pair<Class<?>, Boolean>[] newParamTypes = resolveNewMethodParamTypes(method,
                openClass,
                classLoader,
                openMember,
                toServiceClass,
                provideRuntimeContext);
        var newReturnType = resolveNewMethodReturnType(openClass,
                method,
                openMember,
                classLoader,
                toServiceClass);
        if (newReturnType != null) {
            changes = new MethodSignatureChanges(newParamTypes, newReturnType, false);
        } else if (openMember != null && !isTypeChangingAnnotationPresent(method)) {
            changes = getOpenMemberTypeChanges(openMember, newParamTypes);
        } else if (newParamTypes != null) {
            changes = new MethodSignatureChanges(newParamTypes, null, false);
        } else {
            changes = null;
        }
        return changes;
    }

    private static IOpenMember findServiceOpenMember(Method method,
                                                     IOpenClass openClass,
                                                     ClassLoader classLoader,
                                                     Object serviceTarget) {
        var parameterTypes = new ArrayList<Class<?>>();
        for (Parameter parameter : method.getParameters()) {
            if (!parameter.isAnnotationPresent(ExternalParam.class)) {
                var rulesType = parameter.getAnnotation(RulesType.class);
                Class<?> originType = parameter.getType();
                if (rulesType != null) {
                    var type = RuleServiceInstantiationFactoryHelper
                            .findOrLoadType(openClass, classLoader, rulesType, originType);
                    parameterTypes.add(type);
                } else {
                    parameterTypes.add(originType);
                }
            }
        }
        var openMember = RuleServiceOpenLServiceInstantiationHelper
                .getOpenMember(method.getName(), parameterTypes.toArray(new Class<?>[0]), serviceTarget);
        if (openMember == null) {
            throw new IllegalStateException("Open member is not found.");
        }
        return openMember;
    }

    /**
     * Returns the signature changes that expose the spreadsheet result or {@code Object} array type of the open
     * member. Returns only the new parameter types when the return type stays as is, or {@code null} when nothing
     * changes.
     */
    private static @Nullable MethodSignatureChanges getOpenMemberTypeChanges(IOpenMember openMember,
                                                                             Pair<Class<?>, Boolean>[] newParamTypes) {
        MethodSignatureChanges changes;
        var type = openMember.getType();
        var dim = 0;
        while (type.isArray()) {
            type = type.getComponentClass();
            dim++;
        }
        if (type instanceof CustomSpreadsheetResultOpenClass || type instanceof SpreadsheetResultOpenClass
                || type instanceof AnySpreadsheetResultOpenClass) {
            Class<?> t = switch (type) {
                case CustomSpreadsheetResultOpenClass class2 -> class2.getBeanClass();
                case SpreadsheetResultOpenClass class1 when class1.getModule() != null ->
                        class1.toCustomSpreadsheetResultOpenClass().getBeanClass();
                default -> Map.class;
            };
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
        return changes;
    }

    @SuppressWarnings("unchecked")
    private static Pair<Class<?>, Boolean>[] resolveNewMethodParamTypes(Method method,
                                                                        IOpenClass openClass,
                                                                        ClassLoader classLoader,
                                                                        IOpenMember openMember,
                                                                        boolean toServiceClass,
                                                                        boolean provideRuntimeContext) {
        var methodParamTypes = new ArrayList<Pair<Class<?>, Boolean>>();
        var f = false;
        var i = 0;
        for (Parameter parameter : method.getParameters()) {
            if (!toServiceClass && parameter.isAnnotationPresent(ExternalParam.class)) {
                f = true;
            } else if (parameter.getType().equals(Object.class) && parameter.isAnnotationPresent(RulesType.class)) {
                var loadedType = findOrLoadType(openClass,
                        classLoader,
                        parameter.getAnnotation(RulesType.class),
                        method.getParameterTypes()[i]);
                methodParamTypes.add(Pair.of(loadedType, Boolean.FALSE));
                f = true;
            } else {
                var methodParamType = resolveSpreadsheetResultParamType(method,
                        i,
                        parameter,
                        openMember,
                        toServiceClass,
                        provideRuntimeContext);
                if (Boolean.TRUE.equals(methodParamType.getValue())) {
                    f = true;
                }
                methodParamTypes.add(methodParamType);
            }
            i++;
        }
        return f ? (Pair<Class<?>, Boolean>[]) methodParamTypes.toArray(new Pair[0]) : null;
    }

    /**
     * Returns the type of the parameter and whether a spreadsheet result is converted from or to a bean for it.
     */
    private static Pair<Class<?>, Boolean> resolveSpreadsheetResultParamType(Method method,
                                                                             int i,
                                                                             Parameter parameter,
                                                                             @Nullable IOpenMember openMember,
                                                                             boolean toServiceClass,
                                                                             boolean provideRuntimeContext) {
        Class<?> baseParameterType = parameter.getType();
        var dim = 0;
        while (baseParameterType.isArray()) {
            baseParameterType = baseParameterType.getComponentType();
            dim++;
        }
        if (toServiceClass && openMember instanceof IOpenMethod openMethod && baseParameterType.isAssignableFrom(
                SpreadsheetResult.class) && !parameter.isAnnotationPresent(NoTypeConversion.class)) {
            return resolveBeanParamType(method, i, parameter, openMethod, dim, provideRuntimeContext);
        } else if (!toServiceClass && !parameter
                .isAnnotationPresent(NoTypeConversion.class) && baseParameterType
                .isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
            Class<?> methodParamType = dim > 0 ? Array.newInstance(SpreadsheetResult.class, dim).getClass()
                    : SpreadsheetResult.class;
            return Pair.of(methodParamType, Boolean.TRUE);
        }
        return Pair.of(method.getParameterTypes()[i], Boolean.FALSE);
    }

    /**
     * Returns the bean class of the spreadsheet result that the open method expects for the parameter. Returns the
     * type of the parameter when the open method expects no spreadsheet result or when the parameter is the bean
     * class already.
     */
    private static Pair<Class<?>, Boolean> resolveBeanParamType(Method method,
                                                                int i,
                                                                Parameter parameter,
                                                                IOpenMethod openMethod,
                                                                int dim,
                                                                boolean provideRuntimeContext) {
        var baseOpenParameterType = getBaseOpenParameterType(method, i, openMethod, dim, provideRuntimeContext);
        if (baseOpenParameterType instanceof CustomSpreadsheetResultOpenClass
                || baseOpenParameterType instanceof SpreadsheetResultOpenClass) {
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;
            if (baseOpenParameterType instanceof CustomSpreadsheetResultOpenClass class1) {
                customSpreadsheetResultOpenClass = class1;
            } else {
                customSpreadsheetResultOpenClass = ((SpreadsheetResultOpenClass) baseOpenParameterType)
                        .toCustomSpreadsheetResultOpenClass();
            }
            if (parameter.getType() != customSpreadsheetResultOpenClass.getBeanClass()) {
                Class<?> t = customSpreadsheetResultOpenClass.getBeanClass();
                return Pair.of(dim > 0 ? Array.newInstance(t, dim).getClass() : t, Boolean.TRUE);
            }
        }
        return Pair.of(method.getParameterTypes()[i], Boolean.FALSE);
    }

    /**
     * Returns the component type of the open method parameter that matches the parameter, or {@code null} when the
     * open method has no such parameter.
     *
     * @throws InstantiationException if the array dimensions of the parameters differ
     */
    private static @Nullable IOpenClass getBaseOpenParameterType(Method method,
                                                                 int i,
                                                                 IOpenMethod openMethod,
                                                                 int dim,
                                                                 boolean provideRuntimeContext) {
        if ((!provideRuntimeContext || i > 0) && i - (provideRuntimeContext ? 1 : 0) < openMethod
                .getSignature()
                .getNumberOfParameters()) {
            var baseOpenParameterType = openMethod.getSignature()
                    .getParameterType(i - (provideRuntimeContext ? 1 : 0));
            var d = 0;
            while (baseOpenParameterType.isArray()) {
                baseOpenParameterType = baseOpenParameterType.getComponentClass();
                d++;
            }
            if (dim != d) {
                throw new InstantiationException("Unexpected array dimension size for '%s' method parameter '%s'. Expected dimension size is '%s', but found '%s'.".formatted(
                        MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                        i,
                        d,
                        dim));
            }
            return baseOpenParameterType;
        }
        return null;
    }

    private static class MethodSignatureChanges {
        @Getter(AccessLevel.PRIVATE)
        boolean generateReturnConverters;
        @Getter(AccessLevel.PRIVATE)
        Pair<Class<?>, Boolean>[] newParamTypes;
        @Getter(AccessLevel.PRIVATE)
        Class<?> returnType;

        private MethodSignatureChanges(Pair<Class<?>, Boolean>[] newParamTypes,
                                      Class<?> returnType,
                                      boolean generateReturnConverters) {
            this.newParamTypes = newParamTypes;
            this.generateReturnConverters = generateReturnConverters;
            this.returnType = returnType;
        }
    }

}
