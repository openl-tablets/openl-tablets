package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.variation.VariationsPack;
import org.openl.types.IOpenMethod;
import org.openl.util.generation.InterfaceTransformer;

/**
 * Utility class for generate JAXRS annotations for service interface.
 * 
 * @author Marat Kamalov
 *
 */
public class JAXRSInterfaceEnhancerHelper {

    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive();// || Integer.class == type || Boolean.class
                                  // == type || Character.class == type ||
                                  // String.class == type || Long.class == type
                                  // || Short.class == type || Double.class ==
                                  // type || Float.class == type;
    }

    private static boolean isNullablePrimitive(Class<?> type) {
        return type.isPrimitive() || Integer.class == type || Boolean.class == type || Character.class == type || String.class == type || Long.class == type || Short.class == type || Double.class == type || Float.class == type;
    }

    private static class JAXRSInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DECORATED_CLASS_NAME_SUFFIX = "$JAXRSAnnotated";

        private Class<?> originalClass;
        private OpenLService service;
        private boolean changeReturnTypes = true;

        public JAXRSInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0,
                Class<?> originalClass,
                OpenLService service,
                boolean changeReturnTypes) {
            super(Opcodes.ASM4, arg0);
            this.originalClass = originalClass;
            this.changeReturnTypes = changeReturnTypes;
            this.service = service;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            boolean requiredPathAnnotation = true;
            boolean consumesAnnotationRequired = true;
            boolean producesAnnotationRequired = true;

            for (Annotation annotation : originalClass.getAnnotations()) {

                if (annotation.annotationType().equals(Produces.class)) {
                    producesAnnotationRequired = false;
                }
                if (annotation.annotationType().equals(Consumes.class)) {
                    consumesAnnotationRequired = false;
                }

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

            // Consumes annotation
            if (consumesAnnotationRequired) {
                addConsumesAnnotation(this);
            }
            // Produces annotation
            if (producesAnnotationRequired) {
                addProducesAnnotation(this);
            }
        }

        private String changeReturnType(String signature) {
            int index = signature.lastIndexOf(')');
            return signature.substring(0, index + 1) + Type.getDescriptor(Response.class);
        }

        private String[] getParameterNames(Method method) {
            if (service != null && service.getOpenClass() != null) {
                for (IOpenMethod m : service.getOpenClass().getMethods()) {
                    if (m.getName().equals(method.getName())) {
                        int i = 0;
                        boolean f = true;
                        boolean skipRuntimeContextParameter = false;
                        boolean variationPackIsLastParameter = false;
                        for (Class<?> clazz : method.getParameterTypes()) {
                            if (service.isProvideRuntimeContext() && !skipRuntimeContextParameter) {
                                skipRuntimeContextParameter = true;
                                continue;
                            }
                            if (i == method.getParameterTypes().length - 1 && service.isProvideVariations() && clazz.isAssignableFrom(VariationsPack.class)) {
                                variationPackIsLastParameter = true;
                                continue;
                            }
                            if (i >= m.getSignature().getNumberOfParameters()) {
                                f = false;
                                break;
                            }
                            if (!clazz.equals(m.getSignature().getParameterType(i).getInstanceClass())) {
                                f = false;
                                break;
                            }
                            i++;
                        }
                        if (f) {
                            List<String> parameterNames = new ArrayList<String>();
                            if (service.isProvideRuntimeContext()) {
                                parameterNames.add("runtimeContext");
                            }
                            for (i = 0; i < m.getSignature().getNumberOfParameters(); i++) {
                                parameterNames.add(m.getSignature().getParameterName(i));
                            }
                            if (variationPackIsLastParameter) {
                                parameterNames.add("variationPack");
                            }
                            return parameterNames.toArray(new String[] {});
                        }
                    }
                }
            }
            String[] parameterNames = new String[method.getParameterTypes().length];
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                parameterNames[i] = "arg" + i;
            }
            return parameterNames;
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String methodName, String arg2, String arg3, String[] arg4) {
            Method originalMethod = findOriginalMethod(methodName, arg2);
            if (originalMethod == null) {
                throw new OpenLRuntimeException("Method not found in original class!");
            }

            Annotation[] annotations = originalMethod.getAnnotations();
            boolean skip = false;
            for (Annotation annotation : annotations) {
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
                boolean allPrimitivesBeforeLastArgument = true;
                int k = 0;
                if (originalMethod.getParameterTypes().length < 4) {
                    for (Class<?> parameterType : originalMethod.getParameterTypes()) {
                        if (k < originalMethod.getParameterTypes().length - 1) {
                            if (!isPrimitive(parameterType)) {
                                allPrimitivesBeforeLastArgument = false;
                                break;
                            }
                        }
                        k++;
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("/" + methodName);
                if (allPrimitivesBeforeLastArgument && originalMethod.getParameterTypes().length > 0 && isNullablePrimitive(originalMethod.getParameterTypes()[originalMethod.getParameterTypes().length - 1])) {
                    String[] parameterNames = getParameterNames(originalMethod);
                    int i = 0;
                    for (String paramName : parameterNames) {
                        sb.append("/{" + paramName + ": .*}");
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

        private void addProducesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(Produces.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visit(null, MediaType.APPLICATION_XML);
            av1.visit(null, MediaType.TEXT_XML);
            av1.visit(null, "application/*+xml");
            av1.visitEnd();
            av.visitEnd();
        }

        private void addConsumesAnnotation(ClassVisitor cv) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(Consumes.class), true);
            AnnotationVisitor av1 = av.visitArray("value");
            av1.visit(null, MediaType.APPLICATION_JSON);
            av1.visit(null, MediaType.APPLICATION_XML);
            av1.visit(null, MediaType.TEXT_XML);
            av1.visit(null, "application/*+xml");
            av1.visitEnd();
            av.visitEnd();
        }
    }

    public static Class<?> decorateInterface(Class<?> originalClass,
            ClassLoader classLoader,
            OpenLService service,
            boolean changeReturnTypes) throws Exception {
        if (originalClass == null) {
            throw new IllegalArgumentException("Original class is mandatory argument!");
        }
        if (!originalClass.isInterface()) {
            throw new IllegalArgumentException("Original class should be an interface!");
        }
        ClassWriter cw = new ClassWriter(0);
        JAXRSInterfaceAnnotationEnhancerClassVisitor jaxrsAnnotationEnhancerClassVisitor = new JAXRSInterfaceAnnotationEnhancerClassVisitor(cw,
            originalClass,
            service,
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
        return decorateInterface(originalClass, null, false);
    }

    public static Class<?> decorateInterface(Class<?> originalClass, OpenLService service) throws Exception {
        return decorateInterface(originalClass, service, false);
    }

    public static Class<?> decorateInterface(Class<?> originalClass, OpenLService service, boolean changeReturnTypes) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return decorateInterface(originalClass, classLoader, service, changeReturnTypes);
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
