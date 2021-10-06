package org.openl.rules.ruleservice.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
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
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.ModuleSpecificType;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
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
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.variation.VariationsResult;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                        Class<?>[] newParamTypes = entry.getValue().getParamTypes();
                        Class<?> newRetType = entry.getValue().getReturnType();
                        MethodVisitor mv = super.visitMethod(access,
                            name,
                            Type.getMethodDescriptor(
                                newRetType != null ? Type.getType(newRetType) : Type.getReturnType(descriptor),
                                newParamTypes != null ? Arrays.stream(newParamTypes)
                                    .map(Type::getType)
                                    .toArray(Type[]::new) : Type.getArgumentTypes(descriptor)),
                            signature,
                            exceptions);
                        if (newRetType != null && entry.getValue().isGenerateConverters()) {
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
            boolean provideRuntimeContext,
            boolean provideVariations) {
        return processInterface(null, serviceClass, true, false, classLoader, provideRuntimeContext, provideVariations);
    }

    public static Class<?> buildInterfaceForService(IOpenClass openClass,
            Class<?> serviceClass,
            ClassLoader classLoader,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        return processInterface(openClass,
            serviceClass,
            false,
            true,
            classLoader,
            provideRuntimeContext,
            provideVariations);
    }

    public static Class<?> processInterface(IOpenClass openClass,
            Class<?> serviceClass,
            boolean removeServiceExtraMethods,
            boolean toServiceClass,
            ClassLoader classLoader,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");

        Map<Method, MethodSignatureChanges> methodsWithSignatureNeedsChange = getMethodsWithSignatureNeedsChange(
            openClass,
            serviceClass,
            classLoader,
            toServiceClass,
            provideRuntimeContext,
            provideVariations);

        Set<Method> methodsToRemove = getMethodsToRemove(serviceClass, removeServiceExtraMethods);

        if (methodsWithSignatureNeedsChange.isEmpty() && methodsToRemove.isEmpty()) {
            return serviceClass;
        } else {
            ClassWriter classWriter = new ClassWriter(0);
            ClassVisitor classVisitor = new RuleServiceInterceptorsSupportClassVisitor(classWriter,
                methodsWithSignatureNeedsChange,
                methodsToRemove);
            String className = serviceClass.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
            InterfaceTransformer transformer = new InterfaceTransformer(serviceClass, className);
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
            ClassLoader classLoader,
            boolean toServiceClass,
            boolean provideRuntimeContext,
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
                return extractReturnTypeForMethod(openClass,
                    method,
                    toServiceClass,
                    lastServiceMethodAfterAdvice,
                    provideRuntimeContext,
                    provideVariations);
            }
        }
        ServiceCallAroundInterceptor serviceCallAroundInterceptor = method
            .getAnnotation(ServiceCallAroundInterceptor.class);
        if (serviceCallAroundInterceptor != null && (!provideVariations || !method.getReturnType()
            .equals(VariationsResult.class))) {
            Class<? extends ServiceMethodAroundAdvice<?>> serviceMethodAroundAdvice = serviceCallAroundInterceptor
                .value();
            return extractReturnTypeForMethod(openClass,
                method,
                toServiceClass,
                serviceMethodAroundAdvice,
                provideRuntimeContext,
                provideVariations);
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

    private static Class<?> extractReturnTypeForMethod(IOpenClass openClass,
            Method method,
            boolean toServiceClass,
            Class<? extends ServiceMethodAdvice> serviceMethodAdvice,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        if (toServiceClass) {
            UseOpenMethodReturnType useOpenMethodReturnType = serviceMethodAdvice
                .getAnnotation(UseOpenMethodReturnType.class);
            if (useOpenMethodReturnType != null) {
                return extractOpenMethodReturnType(openClass,
                    method,
                    serviceMethodAdvice,
                    useOpenMethodReturnType.value(),
                    provideRuntimeContext,
                    provideVariations);
            }
            return null;
        } else {
            return Object.class;
        }
    }

    private static Class<?> extractOpenMethodReturnType(IOpenClass openClass,
            Method method,
            Class<?> serviceClass,
            TypeResolver typeResolver,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        IOpenMember openMember = MethodUtils
            .findRulesMember(openClass, method, provideRuntimeContext, provideVariations);
        if (openMember == null) {
            logWarn(method, serviceClass);
            return null;
        }
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
                XlsModuleOpenClass module = (XlsModuleOpenClass) openClass;
                if (type instanceof CustomSpreadsheetResultOpenClass) {
                    Class<?> t = ((CustomSpreadsheetResultOpenClass) module.findType(type.getName())).getBeanClass();
                    return dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                } else if (type instanceof SpreadsheetResultOpenClass) {
                    Class<?> t;
                    // Check: custom spreadsheet is enabled
                    if (module.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
                        t = module.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                            .toCustomSpreadsheetResultOpenClass()
                            .getBeanClass();
                    } else {
                        t = type.getInstanceClass();
                    }
                    return dim > 0 ? Array.newInstance(t, dim).getClass() : t;

                } else {
                    if (type instanceof ModuleSpecificType) {
                        Class<?> t = module.findType(type.getName()).getInstanceClass();
                        return dim > 0 ? Array.newInstance(t, dim).getClass() : t;
                    } else {
                        return returnType.getInstanceClass();
                    }
                }
            default:
                throw new IllegalStateException();
        }
    }

    private static void logWarn(Method method, Class<?> interceptorClass) {
        Logger log = LoggerFactory.getLogger(RuleServiceInstantiationFactoryHelper.class);

        if (log.isWarnEnabled()) {
            log.warn(
                "Method return type is not found for '{}.{}'. Please, make sure that @OpenMethodReturnType is used correctly in '{}' interceptor class.",
                method.getClass().getTypeName(),
                MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                interceptorClass.getTypeName());
        }
    }

    private static boolean isMethodWithServiceExtraMethodAnnotation(Method method) {
        return method.getAnnotation(ServiceExtraMethod.class) != null;
    }

    private static boolean isTypeChangingAnnotationPresent(Method method) {
        return method.isAnnotationPresent(ServiceCallAfterInterceptor.class) || method
            .isAnnotationPresent(ServiceCallAroundInterceptor.class);
    }

    /**
     * Look through all methods of the specified class in order to find all methods annotated by
     * {@link ServiceCallAfterInterceptor}.
     *
     * @param serviceClass Class to be analyzed.
     * @return Methods which have after interceptors.
     */
    private static Map<Method, MethodSignatureChanges> getMethodsWithSignatureNeedsChange(IOpenClass openClass,
            Class<?> serviceClass,
            ClassLoader classLoader,
            boolean toServiceClass,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        Map<Method, MethodSignatureChanges> ret = new HashMap<>();
        for (Method method : serviceClass.getMethods()) {
            Class<?>[] newParamTypes = resolveNewMethodParamTypes(method, openClass, classLoader);
            Class<?> newReturnType = resolveNewMethodReturnType(openClass,
                method,
                classLoader,
                toServiceClass,
                provideRuntimeContext,
                provideVariations);
            if (newReturnType != null) {
                ret.put(method, new MethodSignatureChanges(newParamTypes, newReturnType, false));
            } else if (toServiceClass && !isTypeChangingAnnotationPresent(method) && !method
                .isAnnotationPresent(ServiceExtraMethod.class)) {
                IOpenMember openMember = MethodUtils
                    .findRulesMember(openClass, method, provideRuntimeContext, provideVariations);
                if (openMember == null) {
                    throw new IllegalStateException("Open member is not found.");
                }
                IOpenClass type = openMember.getType();
                int dim = 0;
                while (type.isArray()) {
                    type = type.getComponentClass();
                    dim++;
                }
                if (provideVariations && method.getReturnType().equals(VariationsResult.class)) {
                    ret.put(method, new MethodSignatureChanges(newParamTypes, VariationsResult.class, true));
                } else if (type instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) type;
                    XlsModuleOpenClass module = (XlsModuleOpenClass) openClass;
                    CustomSpreadsheetResultOpenClass csrt = (CustomSpreadsheetResultOpenClass) module
                        .findType(customSpreadsheetResultOpenClass.getName());
                    Class<?> t = csrt.getBeanClass();
                    if (dim > 0) {
                        t = Array.newInstance(t, new int[dim]).getClass();
                    }
                    ret.put(method, new MethodSignatureChanges(newParamTypes, t, true));
                } else if (type instanceof SpreadsheetResultOpenClass) {
                    XlsModuleOpenClass module = (XlsModuleOpenClass) openClass;
                    // Check: custom spreadsheet is enabled
                    if (module.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
                        Class<?> t = module.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                            .toCustomSpreadsheetResultOpenClass()
                            .getBeanClass();
                        if (dim > 0) {
                            t = Array.newInstance(t, new int[dim]).getClass();
                        }
                        ret.put(method, new MethodSignatureChanges(newParamTypes, t, true));
                    }
                } else if (JavaOpenClass.OBJECT.equals(type) && !JavaOpenClass.OBJECT.equals(openMember.getType())) {
                    ret.put(method,
                        new MethodSignatureChanges(newParamTypes, openMember.getType().getInstanceClass(), true));
                }
            } else if (toServiceClass && newParamTypes != null) {
                ret.put(method, new MethodSignatureChanges(newParamTypes, null, false));
            }
        }
        return ret;
    }

    private static Class<?>[] resolveNewMethodParamTypes(Method method, IOpenClass openClass, ClassLoader classLoader) {
        Class<?>[] methodParamTypes = new Class<?>[method.getParameterCount()];
        boolean f = false;
        int i = 0;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            methodParamTypes[i] = method.getParameterTypes()[i];
            for (Annotation a : annotations) {
                if (a instanceof RulesType) {
                    try {
                        Class<?> loadedType = findOrLoadType((RulesType) a, openClass, classLoader);
                        Class<?> t = method.getParameterTypes()[i];
                        while (t.isArray()) {
                            t = t.getComponentType();
                            loadedType = Array.newInstance(loadedType, 0).getClass();
                        }
                        methodParamTypes[i] = loadedType;
                        f = true;
                    } catch (ClassNotFoundException e) {
                        throw new InstantiationException(
                            String.format("Failed to load type '%s' that used in @RulesType annotation.",
                                ((RulesType) a).value()));
                    }
                }
            }
            i++;
        }
        return f ? methodParamTypes : null;
    }

    /**
     * Look through all methods of the specified class in order to find all methods which must be excluded from
     * interface
     *
     * @param serviceClass Class to be analyzed.
     * @param removeServiceExtraMethods {@code true} if methods annotated by {@link ServiceExtraMethod} must be excluded
     * @return Methods which have after interceptors.
     */
    private static Set<Method> getMethodsToRemove(Class<?> serviceClass, boolean removeServiceExtraMethods) {
        Set<Method> ret = new HashSet<>();
        for (Method method : serviceClass.getMethods()) {
            if (ITableProperties.class.isAssignableFrom(method
                .getReturnType()) || (removeServiceExtraMethods && isMethodWithServiceExtraMethodAnnotation(method))) {
                ret.add(method);
            }
        }
        return ret;
    }

    private static class MethodSignatureChanges {
        boolean generateConverters;
        Class<?>[] paramTypes;
        Class<?> returnType;

        public MethodSignatureChanges(Class<?>[] paramTypes, Class<?> returnType, boolean generateConverters) {
            this.paramTypes = paramTypes;
            this.generateConverters = generateConverters;
            this.returnType = returnType;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        public boolean isGenerateConverters() {
            return generateConverters;
        }

        public Class<?> getReturnType() {
            return returnType;
        }
    }

}
