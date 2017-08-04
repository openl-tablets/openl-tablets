package org.openl.rules.ruleservice.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptors;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.variation.VariationsResult;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;

public abstract class RuleServiceInstantiationFactoryHelper {

    /**
     * Special ClassVistor to generate interface with {@link Object} as the
     * return type for methods that have "after interceptors".
     * 
     * @author PUdalau
     */
    private static class ResultConvertorsSupportClassVisitor extends ClassVisitor {
        private Collection<Method> methods;

        /**
         * Constructs instanse with delegated {@link ClassVisitor} and set of
         * methods.
         * 
         * @param visitor delegated {@link ClassVisitor}.
         * @param methods Methods where to change return type.
         */
        public ResultConvertorsSupportClassVisitor(ClassVisitor visitor, Collection<Method> methods) {
            super(Opcodes.ASM4, visitor);
            this.methods = methods;
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            boolean contains = false;
            for (Method method : methods) {
                if (arg1.equals(method.getName()) && arg2.equals(Type.getMethodDescriptor(method))) {
                    contains = true;
                    break;
                }
            }

            if (contains) {
                return super.visitMethod(arg0, arg1, convertReturnType(arg2), arg3, arg4);
            } else {
                return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
            }
        }

        private String convertReturnType(String arg2) {
            int index = arg2.lastIndexOf(')');
            return arg2.substring(0, index + 1) + Type.getDescriptor(Object.class);
        }
    }

    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$Original";

    /**
     * Returns service class for instantiation strategy according to after
     * interceptors of methods in service class of service specified as the
     * argument.
     * 
     * @param instantiationStrategy instantiation strategy where returned
     *            interfaces will be set.
     * @param serviceClass Interface for service, which will be used for service
     *            class creation.
     * @return Service class for instantiation strategy based on service class
     *         for service.
     * @throws Exception
     */
    public static Class<?> getInterfaceForInstantiationStrategy(RulesInstantiationStrategy instantiationStrategy,
            Class<?> serviceClass) {
        boolean hasChangedReturnType = hasMethodsWithAfterInterceptors(serviceClass);
        if (!hasChangedReturnType) {
            return serviceClass;
        } else {
            Set<Method> methodsWithAfterInterceptors = getMethodsWithAfterInterceptors(serviceClass);
            ClassWriter classWriter = new ClassWriter(0);
            ClassVisitor classVisitor = new ResultConvertorsSupportClassVisitor(classWriter,
                    methodsWithAfterInterceptors);
            String className = serviceClass.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
            InterfaceTransformer transformer = new InterfaceTransformer(serviceClass, className);
            transformer.accept(classVisitor);
            classWriter.visitEnd();
            try {
                // Create class object.
                //
                ClassUtils.defineClass(className, classWriter.toByteArray(), instantiationStrategy.getClassLoader());

                // Return loaded to classpath class object.
                //
                return Class.forName(className, true, instantiationStrategy.getClassLoader());
            } catch (Exception e) {
                throw new OpenlNotCheckedException(e);
            }
        }
    }

    private static boolean isMethodsWithAfterInterceptor(Method method){
        if (method.getAnnotation(ServiceCallAfterInterceptor.class) != null
                && !method.getReturnType().equals(VariationsResult.class)) {
            return true;
        }
        
        ServiceCallAfterInterceptors serviceCallAfterInterceptors = method.getAnnotation(ServiceCallAfterInterceptors.class);
        if (serviceCallAfterInterceptors != null && serviceCallAfterInterceptors.value().length > 0 
                && !method.getReturnType().equals(VariationsResult.class)) {
            return true;
        }
        
        if (method.getAnnotation(ServiceCallAroundInterceptor.class) != null
                && !method.getReturnType().equals(VariationsResult.class)) {
            return true;
        }

        return false;
    }
    
    /**
     * Look through all methods (skip methods for variations) of the specified
     * class in order to find all methods annotated by
     * {@link ServiceCallAfterInterceptor}.
     * 
     * @param serviceClass Class to be analyzed.
     * @return returns true if class contains annotated method, otherwise
     *         returns false.
     */
    public static boolean hasMethodsWithAfterInterceptors(Class<?> serviceClass) {
        for (Method method : serviceClass.getMethods()) {
            if (isMethodsWithAfterInterceptor(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look through all methods of the specified class in order to find all
     * methods annotated by {@link ServiceCallAfterInterceptor}.
     * 
     * @param serviceClass Class to be analyzed.
     * @return Methods which have after interceptors.
     */
    public static Set<Method> getMethodsWithAfterInterceptors(Class<?> serviceClass) {
        Set<Method> changedReturnType = new HashSet<Method>();
        for (Method method : serviceClass.getMethods()) {
            if (isMethodsWithAfterInterceptor(method)) {
                changedReturnType.add(method);
            }
        }
        return changedReturnType;
    }

}
