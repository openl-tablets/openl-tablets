package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.MethodUtil;
import org.openl.classloader.ClassLoaderUtils;
import org.openl.rules.datatype.gen.ASMUtils;
import org.openl.rules.ruleservice.core.InstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationFactoryHelper;
import org.openl.rules.ruleservice.core.annotations.ExternalParam;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.types.IOpenClass;
import org.openl.util.generation.InterfaceTransformer;

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
            return tmp.toArray(new Method[]{});
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
                        Type[] typesInTemplateMethod = Arrays.stream(method.getParameters())
                                .filter(e -> !e.isAnnotationPresent(ExternalParam.class)) // Skip parameters with
                                // @ExternalParam annotation
                                .map(e -> Type.getType(e.getType()))
                                .toArray(Type[]::new);
                        Type[] typesInCurrentMethod = Type.getArgumentTypes(descriptor);
                        if (typesInCurrentMethod.length == typesInTemplateMethod.length) {
                            boolean isCompatible = true;
                            for (int i = 0; i < typesInCurrentMethod.length; i++) {
                                if (!typesInCurrentMethod[i].equals(typesInTemplateMethod[i])) {
                                    Parameter parameter = method.getParameters()[i];
                                    boolean isCompatibleParameter = false;
                                    RulesType rulesType = parameter.getAnnotation(RulesType.class);
                                    if (rulesType != null) {
                                        try {
                                            Class<?> type = RuleServiceInstantiationFactoryHelper
                                                    .findOrLoadType(rulesType, openClass, classLoader);
                                            String d = typesInCurrentMethod[i].getDescriptor();
                                            while (d.startsWith("[")) {
                                                d = d.substring(1);
                                            }
                                            if (Objects.equals(Type.getType(type), Type.getType(d))) {
                                                isCompatibleParameter = true;
                                            }
                                        } catch (ClassNotFoundException e) {
                                            throw new InstantiationException(String.format(
                                                    "Failed to apply annotation template class to the service class. Failed to load type '%s' that used in @RulesType annotation.",
                                                    rulesType.value()));
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
                    Type[] argumentTypes = Type.getArgumentTypes(templateMethod);
                    Type[] originalMethodArgumentTypes = Type.getArgumentTypes(descriptor);
                    int i = 0;
                    int j = 0;
                    for (Parameter parameter : templateMethod.getParameters()) {
                        if (!parameter.isAnnotationPresent(ExternalParam.class)) {
                            if (!parameter.isAnnotationPresent(RulesType.class) || isObjectType(parameter.getType())) {
                                argumentTypes[i] = originalMethodArgumentTypes[j];
                            }
                            j++;
                        }
                        i++;
                    }
                    MethodVisitor mv = super.visitMethod(access,
                            name,
                            Type.getMethodDescriptor(Type.getType(templateMethod.getReturnType()), argumentTypes),
                            signature,
                            exceptions);
                    Annotation[] annotations = templateMethod.getAnnotations();
                    for (Annotation annotation : annotations) {
                        AnnotationVisitor annotationVisitor = mv
                                .visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
                        InterfaceTransformer.processAnnotation(annotation, annotationVisitor);
                    }
                    i = 0;
                    for (Parameter parameter : templateMethod.getParameters()) {
                        for (Annotation annotation : parameter.getAnnotations()) {
                            AnnotationVisitor annotationVisitor = mv
                                    .visitParameterAnnotation(i, Type.getDescriptor(annotation.annotationType()), true);
                            InterfaceTransformer.processAnnotation(annotation, annotationVisitor);
                        }
                        i++;
                    }
                    return mv;
                }
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        private static boolean isObjectType(Class<?> type) {
            while (type.isArray()) {
                type = type.getComponentType();
            }
            return type.equals(Object.class);
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
        if (!templateClass.isInterface() && !Modifier.isAbstract(templateClass.getModifiers())) {
            throw new InstantiationException("Only interfaces or abstract classes are supported");
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
        return ClassLoaderUtils.defineClass(enhancedClassName, cw.toByteArray(), classLoader);
    }

    private static void logMissedMethods(
            DynamicInterfaceAnnotationEnhancerClassVisitor dynamicInterfaceAnnotationEnhancerClassVisitor) {
        final Logger log = LoggerFactory.getLogger(DynamicInterfaceAnnotationEnhancerHelper.class);
        if (log.isWarnEnabled()) {
            for (Method method : dynamicInterfaceAnnotationEnhancerClassVisitor.getMissedMethods()) {
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }
                log.warn("Method '{}' from annotation template {} is not found in the service class.",
                        MethodUtil.printQualifiedMethodName(method),
                        method.getDeclaringClass().isInterface() ? "interface" : "class");
            }
        }
    }
}
