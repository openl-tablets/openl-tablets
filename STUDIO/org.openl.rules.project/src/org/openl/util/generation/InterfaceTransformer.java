package org.openl.util.generation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * This class is similar to {@link ClassReader} from ASM framework. But it can be used only for interface generation.
 * <p/>
 * {@link InterfaceTransformer} uses base class that will be transformed, classname for new class and
 * {@link ClassVisitor} that will handle class creation.
 * <p/>
 * {@link InterfaceTransformer} reads methods with signatures,constants,annotations and passes them to
 * {@link ClassVisitor}.
 *
 * @author PUdalau
 */
public class InterfaceTransformer {
    public static final Function<Integer, Integer> IGNORE_PARAMETER_ANNOTATIONS = index -> -1;
    public static final Function<Integer, Integer> ADD_FIRST_PARAMETER = index -> index + 1;
    public static final Function<Integer, Integer> REMOVE_FIRST_PARAMETER = index -> index - 1;
    private final Logger log = LoggerFactory.getLogger(InterfaceTransformer.class);
    private final Class<?> classToTransform;
    private final String className;
    private final Function<Integer, Integer> methodParameterAdaptor;

    /**
     * @param interfaceToTransform Base class for generations.
     * @param className Name for new class(java notation: with .(dot) as the delimiter).
     */
    public InterfaceTransformer(Class<?> interfaceToTransform, String className) {
        this.classToTransform = interfaceToTransform;
        this.className = className;
        this.methodParameterAdaptor = (e) -> e;
    }

    public InterfaceTransformer(Class<?> interfaceToTransform,
            String className,
            Function<Integer, Integer> methodParameterAdaptor) {
        this.classToTransform = interfaceToTransform;
        this.className = className;
        this.methodParameterAdaptor = methodParameterAdaptor;
    }

    /**
     * Reads class and passes class generation instructions to <code>classVisitor</code>. Similar to
     * org.objectweb.asm.ClassReader.accept(...)
     *
     * @param classVisitor Visitor to consume writing instructions.
     */
    public void accept(ClassVisitor classVisitor) {
        classVisitor.visit(Opcodes.V1_8,
            classToTransform.isInterface() ? classToTransform.getModifiers()
                                           : classToTransform.getModifiers() | Modifier.ABSTRACT,
            className.replace('.', '/'),
            null,
            Object.class.getName().replace('.', '/'),
            Arrays.stream(classToTransform.getInterfaces())
                .map(e -> e.getName().replace('.', '/'))
                .toArray(String[]::new));

        for (Annotation annotation : classToTransform.getAnnotations()) {
            AnnotationVisitor av = classVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
            processAnnotation(annotation, av);
        }

        Set<String> usedFields = new HashSet<>();
        Set<Class<?>> usedClasses = new HashSet<>();
        Set<MethodKey> usedMethods = new HashSet<>();
        Queue<Class<?>> queue = new LinkedList<>();
        Queue<Class<?>> interfacesQueue = new LinkedList<>();
        queue.add(classToTransform);
        while (!queue.isEmpty()) {
            Class<?> x = queue.poll();
            if (x.isSynthetic() || usedClasses.contains(x)) {
                continue;
            }
            usedClasses.add(x);
            Field[] declaredFields = x.getDeclaredFields();
            Arrays.sort(declaredFields, Comparator.comparing(Field::getName));
            for (Field field : declaredFields) {
                if (!field.isSynthetic() && !usedFields.contains(field.getName())) {
                    usedFields.add(field.getName());
                    try {
                        field.setAccessible(true);
                        FieldVisitor fieldVisitor = classVisitor.visitField(field.getModifiers(),
                            field.getName(),
                            Type.getDescriptor(field.getType()),
                            null,
                            isConstantField(field) ? field.get(null) : null);
                        if (fieldVisitor != null) {
                            for (Annotation annotation : field.getAnnotations()) {
                                AnnotationVisitor av = fieldVisitor
                                    .visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
                                processAnnotation(annotation, av);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to process field '{}'.", field.getName(), e);
                    }
                }
            }
            List<Method> declaredMethods = Arrays.asList(x.getDeclaredMethods());
            declaredMethods = MethodUtils.sort(declaredMethods);
            for (Method method : declaredMethods) {
                if (!method.isSynthetic()) {
                    MethodKey methodKey = new MethodKey(method.getName(),
                        Arrays.stream(method.getParameterTypes())
                            .map(JavaOpenClass::getOpenClass)
                            .toArray(JavaOpenClass[]::new));
                    if (!usedMethods.contains(methodKey)) {
                        usedMethods.add(methodKey);
                        String ruleName = method.getName();
                        MethodVisitor methodVisitor = classVisitor.visitMethod(
                            x.isInterface() ? method.getModifiers() : method.getModifiers() | Modifier.ABSTRACT,
                            ruleName,
                            Type.getMethodDescriptor(method),
                            null,
                            null);
                        processAnnotationsOnExecutable(methodVisitor, method);
                        if (methodVisitor != null) {
                            methodVisitor.visitEnd();
                        }
                    }
                }
            }
            if (x.isInterface()) {
                queue.addAll(Arrays.asList(x.getInterfaces()));
            } else {
                if (x.getSuperclass() == Object.class) {
                    queue = interfacesQueue;
                } else {
                    queue.add(x.getSuperclass());
                    interfacesQueue.addAll(Arrays.asList(x.getInterfaces()));
                }
            }
        }
        if (!classToTransform.isInterface()) {
            for (Constructor<?> constructor : classToTransform.getDeclaredConstructors()) {
                if (!constructor.isSynthetic()) {
                    GeneratorAdapter mg = new GeneratorAdapter(constructor.getModifiers(),
                        org.objectweb.asm.commons.Method.getMethod(constructor),
                        null,
                        null,
                        classVisitor);
                    processAnnotationsOnExecutable(mg, constructor);
                    mg.visitCode();
                    mg.loadThis();
                    mg.invokeConstructor(Type.getType(classToTransform.getSuperclass()),
                        org.objectweb.asm.commons.Method.getMethod("void <init> ()"));
                    mg.visitInsn(Opcodes.RETURN);
                    int i = 1;
                    for (Class<?> paramType : constructor.getParameterTypes()) {
                        if (long.class == paramType || double.class == paramType) {
                            i += 2;
                        } else {
                            i++;
                        }
                    }
                    mg.visitMaxs(1, i);
                    mg.visitEnd();
                }
            }
        }
    }

    private void processAnnotationsOnExecutable(MethodVisitor methodVisitor, Executable executable) {
        if (methodVisitor != null) {
            for (Annotation annotation : executable.getAnnotations()) {
                AnnotationVisitor av = methodVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()),
                    true);
                processAnnotation(annotation, av);
            }
            int index = 0;
            for (Annotation[] annotations : executable.getParameterAnnotations()) {
                int i = methodParameterAdaptor.apply(index);
                if (i >= 0 && i < executable.getParameterCount()) {
                    for (Annotation annotation : annotations) {
                        String descriptor = Type.getDescriptor(annotation.annotationType());
                        AnnotationVisitor av = methodVisitor.visitParameterAnnotation(i, descriptor, true);
                        processAnnotation(annotation, av);
                    }
                }
                index++;
            }
        }
    }

    private static boolean isConstantField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    public static void processAnnotation(Annotation annotation, AnnotationVisitor av) {
        Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotation);
        if (av != null) {
            for (Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {
                Object attributeValue = annotationAttribute.getValue();
                Class<? extends Object> attributeType = attributeValue.getClass();
                if (attributeType.isArray()) {
                    AnnotationVisitor arrayVisitor = av.visitArray(annotationAttribute.getKey());
                    Object[] array = (Object[]) attributeValue;
                    for (Object o : array) {
                        visitNonArrayAnnotationAttribute(arrayVisitor, null, o);
                    }
                    arrayVisitor.visitEnd();
                } else {
                    visitNonArrayAnnotationAttribute(av, annotationAttribute.getKey(), annotationAttribute.getValue());
                }
            }
            av.visitEnd();
        }
    }

    private static void visitNonArrayAnnotationAttribute(AnnotationVisitor av,
            String attributeName,
            Object attributeValue) {
        Class<? extends Object> attributeType = attributeValue.getClass();
        if (attributeValue instanceof Class) {
            av.visit(attributeName, Type.getType((Class<?>) attributeValue));
        } else if (attributeType.isEnum()) {
            av.visitEnum(attributeName, Type.getDescriptor(attributeType), attributeValue.toString());
        } else if (attributeValue instanceof Annotation) {
            Annotation annotation = (Annotation) attributeValue;
            AnnotationVisitor av1 = av.visitAnnotation(attributeName, Type.getDescriptor(annotation.annotationType()));
            processAnnotation(annotation, av1);
        } else {
            av.visit(attributeName, attributeValue);
        }
    }

    /**
     * @return Base class for generations.
     */
    public Class<?> getClassToTransform() {
        return classToTransform;
    }

    /**
     * @return The name for new generated class.
     */
    public String getClassName() {
        return className;
    }

}
