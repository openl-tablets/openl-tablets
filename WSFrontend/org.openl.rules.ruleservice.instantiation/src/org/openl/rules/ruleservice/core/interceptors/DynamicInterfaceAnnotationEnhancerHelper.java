package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.InstantiationException;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DynamicInterfaceAnnotationEnhancerHelper {

    private DynamicInterfaceAnnotationEnhancerHelper() {
    }

    private static class DynamicInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DECORATED_CLASS_NAME_SUFFIX = "$Intercepted";

        private final Class<?> templateClass;
        private final Set<Method> foundMethods = new HashSet<>();

        public DynamicInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0, Class<?> templateClass) {
            super(Opcodes.ASM5, arg0);
            this.templateClass = templateClass;

        }

        public Method[] getMissedMethods() {
            Set<Method> tmp = new HashSet<>(Arrays.asList(templateClass.getMethods()));
            tmp.removeAll(foundMethods);
            return tmp.toArray(new Method[] {});
        }

        @Override
        public void visit(int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            Annotation[] annotations = templateClass.getAnnotations();
            for (Annotation annotation : annotations) {
                AnnotationVisitor annotationVisitor = this
                    .visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
                InterfaceTransformer.processAnnotation(annotation, annotationVisitor);
            }
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            if (templateClass != null) {
                Method templateMethod = null;
                for (Method method : templateClass.getMethods()) {
                    if (arg1.equals(method.getName())) {
                        Type[] typesInTemplateMethod = Type.getArgumentTypes(method);
                        Type[] typesInCurrentMethod = Type.getArgumentTypes(arg2);
                        if (typesInCurrentMethod.length == typesInTemplateMethod.length) {
                            boolean isCompatible = true;
                            for (int i = 0; i < typesInCurrentMethod.length; i++) {
                                if (!typesInCurrentMethod[i].equals(typesInTemplateMethod[i])) {
                                    Annotation[] annotations = method.getParameterAnnotations()[i];
                                    boolean isAnyTypeParameter = false;
                                    for (Annotation annotation : annotations) {
                                        if (annotation instanceof AnyType) {
                                            AnyType anyTypeAnnotation = (AnyType) annotation;
                                            String pattern = anyTypeAnnotation.value();
                                            if (pattern.isEmpty()) {
                                                isAnyTypeParameter = true;
                                            } else {
                                                if (Pattern.matches(pattern, typesInCurrentMethod[i].getClassName())) {
                                                    isAnyTypeParameter = true;
                                                }
                                            }
                                        }
                                    }
                                    if (!isAnyTypeParameter) {
                                        isCompatible = false;
                                        break;
                                    }
                                }
                            }
                            if (isCompatible) {
                                if (templateMethod == null) {
                                    templateMethod = method;
                                } else {
                                    throw new InstantiationException(
                                        "Template class is wrong. It is a non-obvious choice of method. Please, check the template class.");
                                }
                            }
                        }
                    }
                }
                if (templateMethod != null) {
                    foundMethods.add(templateMethod);
                    MethodVisitor mv = super.visitMethod(arg0, arg1, arg2, arg3, arg4);
                    Annotation[] annotations = templateMethod.getAnnotations();
                    for (Annotation annotation : annotations) {
                        AnnotationVisitor annotationVisitor = mv
                            .visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
                        InterfaceTransformer.processAnnotation(annotation, annotationVisitor);
                    }
                    int i = 0;
                    for (Annotation[] parameterAnnotations : templateMethod.getParameterAnnotations()) {
                        if (parameterAnnotations.length > 0) {
                            for (Annotation annotation : parameterAnnotations) {
                                AnnotationVisitor annotationVisitor = mv
                                    .visitParameterAnnotation(i, Type.getDescriptor(annotation.annotationType()), true);
                                InterfaceTransformer.processAnnotation(annotation, annotationVisitor);
                            }
                        }
                        i++;
                    }
                    return mv;
                }
            }
            return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
        }
    }

    private static void processServiceExtraMethods(ClassVisitor classVisitor, Class<?> templateClass) {
        for (Method method : templateClass.getMethods()) {
            if (method.isAnnotationPresent(ServiceExtraMethod.class)) {
                classVisitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    method.getName(),
                    Type.getMethodDescriptor(method),
                    null,
                    null);
            }
        }
    }

    public static Class<?> decorate(Class<?> originalClass,
            Class<?> templateClass,
            ClassLoader classLoader) throws Exception {
        if (!templateClass.isInterface()) {
            throw new InstantiationException("Interface is expected.");
        }
        final String enhancedClassName = originalClass
            .getName() + DynamicInterfaceAnnotationEnhancerClassVisitor.DECORATED_CLASS_NAME_SUFFIX;

        ClassWriter cw = new ClassWriter(0);
        DynamicInterfaceAnnotationEnhancerClassVisitor dynamicInterfaceAnnotationEnhancerClassVisitor = new DynamicInterfaceAnnotationEnhancerClassVisitor(
            cw,
            templateClass);
        processServiceExtraMethods(dynamicInterfaceAnnotationEnhancerClassVisitor, templateClass);

        InterfaceTransformer transformer = new InterfaceTransformer(originalClass, enhancedClassName);
        transformer.accept(dynamicInterfaceAnnotationEnhancerClassVisitor);
        cw.visitEnd();
        logMissedMethods(dynamicInterfaceAnnotationEnhancerClassVisitor);
        return ClassUtils.defineClass(enhancedClassName, cw.toByteArray(), classLoader);
    }

    private static void logMissedMethods(
            DynamicInterfaceAnnotationEnhancerClassVisitor dynamicInterfaceAnnotationEnhancerClassVisitor) {
        final Logger log = LoggerFactory.getLogger(DynamicInterfaceAnnotationEnhancerHelper.class);
        if (log.isWarnEnabled()) {
            for (Method method : dynamicInterfaceAnnotationEnhancerClassVisitor.getMissedMethods()) {
                log.warn("Annotation template method '{}' is not found in the service class.",
                    MethodUtil.printQualifiedMethodName(method));
            }
        }
    }

    public static Class<?> decorate(Class<?> originalClass, Class<?> templateClass) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return decorate(originalClass, templateClass, classLoader);
    }
}
