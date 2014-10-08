package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.cglib.core.ReflectUtils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.exception.OpenLRuntimeException;
import org.openl.util.generation.InterfaceTransformer;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

/**
 * Utility class for generate JAXRS annotations for service interface.
 * 
 * @author Marat Kamalov
 *
 */
public class JAXRSInterfaceEnhancerHelper {

    private static LocalVariableTableParameterNameDiscoverer DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() || Integer.class == type || Boolean.class == type || Character.class == type || String.class == type || Long.class == type || Short.class == type || Double.class == type || Float.class == type;
    }

    private static class JAXRSInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DECORATED_CLASS_NAME_SUFFIX = "$JAXRSAnnotated";

        private Class<?> originalClass;
        private boolean changeReturnTypes = true;

        public JAXRSInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0,
                Class<?> originalClass,
                boolean changeReturnTypes) {
            super(Opcodes.ASM4, arg0);
            this.originalClass = originalClass;
            this.changeReturnTypes = changeReturnTypes;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            boolean requiredPathAnnotation = true;
            for (Annotation annotation : originalClass.getAnnotations()) {
                if (annotation.annotationType().equals(Path.class)) {
                    requiredPathAnnotation = false;
                    break;
                }
            }
            if (requiredPathAnnotation) {
                AnnotationVisitor annotationVisitor = this.visitAnnotation(Type.getDescriptor(Path.class), true);
                annotationVisitor.visit("value", "/");
                annotationVisitor.visitEnd();
            }
        }

        private String changeReturnType(String signature) {
            int index = signature.lastIndexOf(')');
            return signature.substring(0, index + 1) + Type.getDescriptor(Response.class);
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String methodName, String arg2, String arg3, String[] arg4) {
            Method originalMethod = findOriginalMethod(methodName, arg2);
            if (originalMethod == null) {
                throw new OpenLRuntimeException("Method not found in original class!");
            }

            Annotation[] annotations = originalMethod.getAnnotations();
            boolean consumesAnnotationRequired = true;
            boolean producesAnnotationRequired = true;
            boolean skip = false;
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(Produces.class)) {
                    producesAnnotationRequired = false;
                }
                if (annotation.annotationType().equals(Consumes.class)) {
                    consumesAnnotationRequired = false;
                }
                if (annotation.annotationType().equals(Path.class)) {
                    skip = true;
                }
                if (annotation.annotationType().equals(POST.class)) {
                    skip = true;
                }
                if (annotation.annotationType().equals(GET.class)) {
                    skip = true;
                }
            }

            MethodVisitor mv = null;

            if (changeReturnTypes && !originalMethod.getReturnType().equals(Response.class)) {
                mv = super.visitMethod(arg0, methodName, changeReturnType(arg2), arg3, arg4);
            } else {
                mv = super.visitMethod(arg0, methodName, arg2, arg3, arg4);
            }

            if (!skip) {
                boolean allPrimitives = true;
                if (originalMethod.getParameterTypes().length < 4) {
                    for (Class<?> parameterType : originalMethod.getParameterTypes()) {
                        if (!isPrimitive(parameterType)) {
                            allPrimitives = false;
                            break;
                        }
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("/" + methodName);
                if (allPrimitives) {
                    String[] parameterNames = DISCOVERER.getParameterNames(originalMethod);
                    if (parameterNames == null) {
                        parameterNames = new String[originalMethod.getParameterTypes().length];
                        for (int i = 0; i < originalMethod.getParameterTypes().length; i++) {
                            parameterNames[i] = "arg" + i;
                        }
                    }
                    int i = 0;
                    for (String paramName : parameterNames) {
                        sb.append("/{" + paramName + "}");
                        addPathParamAnnotation(mv, i, paramName);
                        i++;
                    }
                    addGetAnnotation(mv);
                    addPathAnnotation(mv, sb.toString());
                } else {
                    addPostAnnotation(mv);
                    addPathAnnotation(mv, sb.toString());
                }
            }

            // Consumes annotation
            if (consumesAnnotationRequired) {
                addConsumesAnnotation(mv);
            }
            // Produces annotation
            if (producesAnnotationRequired) {
                addProducesAnnotation(mv);
            }
            return mv;
        }

        private Method findOriginalMethod(String methodName, String argumentTypes) {
            Method originalMethod = null;
            for (Method method : originalClass.getMethods()) {
                if (originalMethod == null && methodName.equals(method.getName())) {
                    Type[] typesInOriginalClassMethod = Type.getArgumentTypes(method);
                    Type[] typesInCurrentMethod = Type.getArgumentTypes(argumentTypes);
                    if (typesInCurrentMethod.length == typesInOriginalClassMethod.length) {
                        boolean f = true;
                        for (int i = 0; i < typesInCurrentMethod.length; i++) {
                            if (!typesInCurrentMethod[i].equals(typesInOriginalClassMethod[i])) {
                                f = false;
                            }
                        }
                        if (f) {
                            originalMethod = method;
                        }
                    }
                }
            }
            return originalMethod;
        }

        private void addPostAnnotation(MethodVisitor mv) {
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(POST.class), true);
            av.visitEnd();
        }

        private void addGetAnnotation(MethodVisitor mv) {
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(GET.class), true);
            av.visitEnd();
        }

        private void addPathAnnotation(MethodVisitor mv, String path) {
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Path.class), true);
            av.visit("value", path);
            av.visitEnd();
        }

        private void addPathParamAnnotation(MethodVisitor mv, int index, String paramName) {
            AnnotationVisitor av = mv.visitParameterAnnotation(index, Type.getDescriptor(PathParam.class), true);
            av.visit("value", paramName);
            av.visitEnd();
        }

        private void addProducesAnnotation(MethodVisitor mv) {
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Produces.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visit(null, MediaType.APPLICATION_XML);
            av1.visit(null, MediaType.TEXT_XML);
            av1.visit(null, "application/*+xml");
            av1.visitEnd();
            av.visitEnd();
        }

        private void addConsumesAnnotation(MethodVisitor mv) {
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Consumes.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visit(null, MediaType.APPLICATION_XML);
            av1.visit(null, MediaType.TEXT_XML);
            av1.visit(null, "application/*+xml");
            av1.visitEnd();
            av.visitEnd();
        }
    }

    public static Class<?> decorateInterface(Class<?> originalClass, ClassLoader classLoader, boolean changeReturnTypes) throws Exception {
        if (originalClass == null) {
            throw new IllegalArgumentException("Original class is mandatory argument!");
        }
        if (!originalClass.isInterface()) {
            throw new IllegalArgumentException("Original class should be an interface!");
        }
        ClassWriter cw = new ClassWriter(0);
        JAXRSInterfaceAnnotationEnhancerClassVisitor jaxrsAnnotationEnhancerClassVisitor = new JAXRSInterfaceAnnotationEnhancerClassVisitor(cw,
            originalClass,
            changeReturnTypes);
        String enchancedClassName = originalClass.getCanonicalName() + JAXRSInterfaceAnnotationEnhancerClassVisitor.DECORATED_CLASS_NAME_SUFFIX;
        // Fix an NPE issue JAXRSUtil with no package class
        if (originalClass.getPackage() == null) {
            enchancedClassName = "default." + enchancedClassName;
        }
        InterfaceTransformer transformer = new InterfaceTransformer(originalClass, enchancedClassName);
        transformer.accept(jaxrsAnnotationEnhancerClassVisitor);
        cw.visitEnd();
        Class<?> enchancedClass = ReflectUtils.defineClass(enchancedClassName, cw.toByteArray(), classLoader);
        return enchancedClass;
    }

    public static Class<?> decorateInterface(Class<?> originalClass) throws Exception {
        return decorateInterface(originalClass, false);
    }

    public static Class<?> decorateInterface(Class<?> originalClass, boolean changeReturnTypes) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return decorateInterface(originalClass, classLoader, changeReturnTypes);
    }

    public static Object decorateBean(Object targetBean, Class<?> proxyInterface, Class<?> targetInterface) throws Exception {
        Map<Method, Method> methodMap = new HashMap<Method, Method>();
        for (Method method : proxyInterface.getMethods()) {
            boolean f = false;
            for (Method targetMethod : targetInterface.getMethods()) {
                if (targetMethod.getName().equals(method.getName())) {
                    if (targetMethod.getParameterTypes().length == method.getParameterTypes().length) {
                        Class<?>[] targetParams = targetMethod.getParameterTypes();
                        Class<?>[] params = targetMethod.getParameterTypes();
                        boolean found = true;
                        for (int i = 0; i < targetParams.length; i++) {
                            if (!targetParams[i].equals(params[i])) {
                                found = false;
                                break;
                            }
                        }
                        if (found) {
                            methodMap.put(method, targetMethod);
                            f = true;
                            break;
                        }
                    }
                }
            }
            if (!f) {
                throw new IllegalStateException("Method doesn't exists in original interface!");
            }
        }

        return Proxy.newProxyInstance(targetInterface.getClassLoader(),
            new Class<?>[] { proxyInterface },
            new JAXRSInvocationHandler(targetBean, methodMap));
    }

}
