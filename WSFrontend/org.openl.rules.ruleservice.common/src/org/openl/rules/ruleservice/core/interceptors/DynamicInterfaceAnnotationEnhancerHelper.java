package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.rules.datatype.gen.ASMUtils;
import org.openl.rules.ruleservice.core.InstantiationException;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.types.IOpenClass;
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
        private final IOpenClass openClass;
        private final ClassLoader classLoader;
        private final Map<String, List<Method>> templateClassMethodsByName;

        public DynamicInterfaceAnnotationEnhancerClassVisitor(ClassVisitor arg0,
                Class<?> templateClass,
                IOpenClass openClass,
                ClassLoader classLoader) {
            super(Opcodes.ASM5, arg0);
            this.templateClass = templateClass;
            this.openClass = openClass;
            this.classLoader = classLoader;
            this.templateClassMethodsByName = ASMUtils.buildMap(templateClass);
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
        public MethodVisitor visitMethod(final int access,
                final String name,
                final String descriptor,
                final String signature,
                final String[] exceptions) {
            if (templateClass != null) {
                Method templateMethod = null;
                List<Method> methods = templateClassMethodsByName.get(name);
                if (methods != null) {
                    for (Method method : methods) {
                        Type[] typesInTemplateMethod = Type.getArgumentTypes(method);
                        Type[] typesInCurrentMethod = Type.getArgumentTypes(descriptor);
                        if (typesInCurrentMethod.length == typesInTemplateMethod.length) {
                            boolean isCompatible = true;
                            for (int i = 0; i < typesInCurrentMethod.length; i++) {
                                if (!typesInCurrentMethod[i].equals(typesInTemplateMethod[i])) {
                                    Annotation[] annotations = method.getParameterAnnotations()[i];
                                    boolean isCompatibleParameter = false;
                                    for (Annotation annotation : annotations) {
                                        if (annotation instanceof AnyType) {
                                            AnyType anyTypeAnnotation = (AnyType) annotation;
                                            String pattern = anyTypeAnnotation.value();
                                            if (pattern.isEmpty()) {
                                                isCompatibleParameter = true;
                                            } else {
                                                if (Pattern.matches(pattern, typesInCurrentMethod[i].getClassName())) {
                                                    isCompatibleParameter = true;
                                                }
                                            }
                                        } else if (annotation instanceof RulesType) {
                                            RulesType rulesType = (RulesType) annotation;
                                            Class<?> type = findOrLoadType(rulesType);
                                            String d = typesInCurrentMethod[i].getDescriptor();
                                            while (d.length() > 0 && d.startsWith("[")) {
                                                d = d.substring(1);
                                            }
                                            if (Objects.equals(Type.getType(type), Type.getType(d))) {
                                                isCompatibleParameter = true;
                                            }
                                        }
                                    }
                                    if (!isCompatibleParameter) {
                                        isCompatible = false;
                                        break;
                                    }
                                }
                            }
                            if (isCompatible) {
                                if (templateMethod == null) {
                                    templateMethod = method;
                                } else {
                                    throw new InstantiationException(String.format(
                                        "Failed to apply annotation template class to the service class. It is a non-obvious choice of '%s' method.",
                                        MethodUtil.printMethod(method.getName(), method.getParameterTypes())));
                                }
                            }
                        }
                    }
                }
                if (templateMethod != null) {
                    foundMethods.add(templateMethod);
                    Type returnType;
                    RulesType rulesType = templateMethod.getAnnotation(RulesType.class);
                    if (rulesType != null) {
                        Class<?> type = findOrLoadType(rulesType);
                        Class<?> t = templateMethod.getReturnType();
                        while (t.isArray()) {
                            t = t.getComponentType();
                            type = Array.newInstance(type, 0).getClass();
                        }
                        returnType = Type.getType(type);
                    } else {
                        returnType = Type.getReturnType(descriptor);
                    }
                    final Type[] methodParameterTypes = Type.getArgumentTypes(descriptor);
                    for (int i = 0; i < templateMethod.getParameterCount(); i++) {
                        for (Annotation annotation : templateMethod.getParameterAnnotations()[i]) {
                            if (annotation instanceof RulesType) {
                                RulesType paramRulesType = (RulesType) annotation;
                                Class<?> type = findOrLoadType(paramRulesType);
                                String d = methodParameterTypes[i].getDescriptor();
                                while (d.length() > 0 && d.startsWith("[")) {
                                    d = d.substring(1);
                                    type = Array.newInstance(type, 0).getClass();
                                }
                                methodParameterTypes[i] = Type.getType(type);
                            }
                        }
                    }
                    MethodVisitor mv = super.visitMethod(access,
                        name,
                        Type.getMethodDescriptor(returnType, methodParameterTypes),
                        signature,
                        exceptions);
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
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        private Class<?> findOrLoadType(RulesType rulesType) {
            String typeName = rulesType.value();
            try {
                return classLoader.loadClass(typeName);
            } catch (ClassNotFoundException e) {
                for (IOpenClass type : openClass.getTypes()) {
                    if (Objects.equals(type.getName(), typeName)) {
                        return type.getInstanceClass();
                    }
                }
                throw new InstantiationException(
                    "Failed to apply annotation template class to the service class. Failed to load type '%s' that used in @RulesType annotation.");
            }
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
            IOpenClass openClass,
            ClassLoader classLoader) throws Exception {
        if (!templateClass.isInterface()) {
            throw new InstantiationException("Only interface is supported");
        }
        final String enhancedClassName = originalClass
            .getName() + DynamicInterfaceAnnotationEnhancerClassVisitor.DECORATED_CLASS_NAME_SUFFIX;

        ClassWriter cw = new ClassWriter(0);
        DynamicInterfaceAnnotationEnhancerClassVisitor dynamicInterfaceAnnotationEnhancerClassVisitor = new DynamicInterfaceAnnotationEnhancerClassVisitor(
            cw,
            templateClass,
            openClass,
            classLoader);
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
                log.warn("Method '{}' from annotation template interface is not found in the service class.",
                    MethodUtil.printQualifiedMethodName(method));
            }
        }
    }
}
