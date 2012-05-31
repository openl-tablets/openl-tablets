package org.openl.rules.ruleservice.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cglib.core.ReflectUtils;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.util.generation.InterfaceTransformer;

public abstract class RuleServiceInstantiationFactoryHelper {

    /**
     * Special ClassVistor to generate interface with {@link Object} as the
     * return type for methods that have "after interceptors".
     * 
     * @author PUdalau
     */
    private static class ResultConvertorsSupportClassVisitor extends ClassAdapter {
        private Set<String> methods;

        /**
         * Constructs instanse with delegated {@link ClassVisitor} and set of
         * methods to be changed in the form of method descriptors.
         * 
         * @param visitor delegated {@link ClassVisitor}.
         * @param methods String descriptions of methods where to change return
         *            type.{@see
         *            org.objectweb.asm.Type.getMethodDescriptor(Method)}
         */
        public ResultConvertorsSupportClassVisitor(ClassVisitor visitor, Set<String> methods) {
            super(visitor);
            this.methods = methods;
        }

        /**
         * Constructs instanse with delegated {@link ClassVisitor} and set of
         * methods.
         * 
         * @param visitor delegated {@link ClassVisitor}.
         * @param methods Methods where to change return type.
         */
        public ResultConvertorsSupportClassVisitor(ClassVisitor visitor, Collection<Method> methods) {
            this(visitor, convertToSignatureDescriptions(methods));
        }

        private static Set<String> convertToSignatureDescriptions(Collection<Method> methods) {
            Set<String> signatureDescriptions = new HashSet<String>();
            for (Method method : methods) {
                signatureDescriptions.add(Type.getMethodDescriptor(method));
            }
            return signatureDescriptions;
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            if (!methods.contains(arg2)) {
                return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
            } else {
                return super.visitMethod(arg0, arg1, convertReturnType(arg2), arg3, arg4);

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
            Class<?> serviceClass) throws Exception {
        Set<Method> changedReturnType = getMethodsWithAfterInterceptors(serviceClass);
        if (changedReturnType.isEmpty()) {
            return serviceClass;
        } else {
            ClassWriter classWriter = new ClassWriter(0);
            ClassVisitor classVisitor = new ResultConvertorsSupportClassVisitor(classWriter, changedReturnType);
            String className = serviceClass.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
            InterfaceTransformer transformer = new InterfaceTransformer(serviceClass, className);
            transformer.accept(classVisitor);
            classWriter.visitEnd();

            // Create class object.
            //
            ReflectUtils.defineClass(className, classWriter.toByteArray(), instantiationStrategy.getClassLoader());

            // Return loaded to classpath class object.
            //
            return Class.forName(className, true, instantiationStrategy.getClassLoader());
        }
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
            if (method.getAnnotation(ServiceCallAfterInterceptor.class) != null) {
                changedReturnType.add(method);
            }
        }
        return changedReturnType;
    }

}
