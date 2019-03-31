package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;

public final class DynamicInterfaceAnnotationEnhancerHelper {

    private DynamicInterfaceAnnotationEnhancerHelper() {
    }

    private static class DynamicInterfaceAnnotationEnhancerClassVisitor extends ClassVisitor {
        private static final String DECORATED_CLASS_NAME_SUFFIX = "$Intercepted";

        private Class<?> templateClass;

        public DynamicInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0, Class<?> templateClass) {
            super(Opcodes.ASM5, arg0);
            this.templateClass = templateClass;

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
                                        if (annotation.annotationType().equals(AnyType.class)) {
                                            AnyType anyTypeAnnotation = (AnyType) annotation;
                                            String pattern = anyTypeAnnotation.value();
                                            if (pattern == null || pattern.isEmpty()) {
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
                                    throw new RuleServiceRuntimeException(
                                        "Template class is wrong. It is a non-obvious choice of method. Please, check the template class!");
                                }
                            }
                        }
                    }
                }
                if (templateMethod != null) {
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
                MethodVisitor mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
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
            throw new RuleServiceRuntimeException("Interface is expected!");
        }

        ClassWriter cw = new ClassWriter(0);
        DynamicInterfaceAnnotationEnhancerClassVisitor dynamicInterfaceAnnotationEnhancerClassVisitor = new DynamicInterfaceAnnotationEnhancerClassVisitor(
            cw,
            templateClass);

        processServiceExtraMethods(dynamicInterfaceAnnotationEnhancerClassVisitor, templateClass);

        String enchancedClassName = originalClass
            .getCanonicalName() + DynamicInterfaceAnnotationEnhancerClassVisitor.DECORATED_CLASS_NAME_SUFFIX;
        InterfaceTransformer transformer = new InterfaceTransformer(originalClass, enchancedClassName);
        transformer.accept(dynamicInterfaceAnnotationEnhancerClassVisitor);
        cw.visitEnd();
        return ClassUtils.defineClass(enchancedClassName, cw.toByteArray(), classLoader);
    }

    public static Class<?> decorate(Class<?> originalClass, Class<?> templateClass) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return decorate(originalClass, templateClass, classLoader);
    }
}
