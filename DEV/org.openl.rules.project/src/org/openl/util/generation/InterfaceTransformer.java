package org.openl.util.generation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;

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
@Slf4j
public class InterfaceTransformer {
    public static final IntUnaryOperator IGNORE_PARAMETER_ANNOTATIONS = index -> -1;
    private final Class<?> classToTransform;
    private final String className;
    private final IntUnaryOperator methodParameterAdaptor;

    private static final Comparator<Method> METHOD_COMPARATOR = Comparator.comparing(Method::getName)
            .thenComparingInt(Method::getParameterCount)
            .thenComparing(Method::getParameterTypes, InterfaceTransformer::compareNames);

    private static int compareNames(Class<?>[] p1, Class<?>[] p2) {
        for (var i = 0; i < p1.length; i++) {
            var name1 = p1[i].getName();
            var name2 = p2[i].getName();
            var cmp = name1.compareTo(name2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    /**
     * @param interfaceToTransform Base class for generations.
     * @param className            Name for new class(java notation: with .(dot) as the delimiter).
     */
    public InterfaceTransformer(Class<?> interfaceToTransform, String className) {
        this.classToTransform = interfaceToTransform;
        this.className = className;
        this.methodParameterAdaptor = IntUnaryOperator.identity();
    }

    public InterfaceTransformer(Class<?> interfaceToTransform,
                                String className,
                                IntUnaryOperator methodParameterAdaptor) {
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
            var av = classVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
            processAnnotation(annotation, av);
        }

        var usedFields = new HashSet<String>();
        var usedClasses = new HashSet<Class<?>>();
        var usedMethods = new HashSet<MethodKey>();
        Queue<Class<?>> queue = new ArrayDeque<>();
        var interfacesQueue = new ArrayDeque<Class<?>>();
        queue.add(classToTransform);
        while (!queue.isEmpty()) {
            Class<?> x = queue.poll();
            if (x.isSynthetic() || usedClasses.contains(x)) {
                continue;
            }
            usedClasses.add(x);
            visitFields(classVisitor, x, usedFields);
            visitMethods(classVisitor, x, usedMethods);
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
            visitConstructors(classVisitor);
        }
    }

    private static void visitFields(ClassVisitor classVisitor, Class<?> x, Set<String> usedFields) {
        var declaredFields = x.getDeclaredFields();
        Arrays.sort(declaredFields, Comparator.comparing(Field::getName));
        for (Field field : declaredFields) {
            if (!field.isSynthetic() && !usedFields.contains(field.getName())) {
                usedFields.add(field.getName());
                visitField(classVisitor, field);
            }
        }
    }

    private static void visitField(ClassVisitor classVisitor, Field field) {
        try {
            var fieldVisitor = classVisitor.visitField(field.getModifiers(),
                    field.getName(),
                    Type.getDescriptor(field.getType()),
                    null,
                    isConstantField(field) ? field.get(null) : null);
            if (fieldVisitor != null) {
                for (Annotation annotation : field.getAnnotations()) {
                    var av = fieldVisitor
                            .visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
                    processAnnotation(annotation, av);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process field '{}'.", field.getName(), e);
        }
    }

    private void visitMethods(ClassVisitor classVisitor, Class<?> x, Set<MethodKey> usedMethods) {
        var declaredMethods = x.getDeclaredMethods();
        Arrays.sort(declaredMethods, METHOD_COMPARATOR);
        for (Method method : declaredMethods) {
            if (!method.isSynthetic()) {
                var methodKey = new MethodKey(method.getName(),
                        Arrays.stream(method.getParameterTypes())
                                .map(JavaOpenClass::getOpenClass)
                                .toArray(JavaOpenClass[]::new));
                if (!usedMethods.contains(methodKey)) {
                    usedMethods.add(methodKey);
                    var ruleName = method.getName();
                    var methodVisitor = classVisitor.visitMethod(
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
    }

    private void visitConstructors(ClassVisitor classVisitor) {
        for (Constructor<?> constructor : classToTransform.getDeclaredConstructors()) {
            if (!constructor.isSynthetic()) {
                var mg = new GeneratorAdapter(constructor.getModifiers(),
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
                var i = 1;
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

    private void processAnnotationsOnExecutable(MethodVisitor methodVisitor, Executable executable) {
        if (methodVisitor != null) {
            for (Annotation annotation : executable.getAnnotations()) {
                var av = methodVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()),
                        true);
                processAnnotation(annotation, av);
            }
            var index = 0;
            for (Annotation[] annotations : executable.getParameterAnnotations()) {
                var i = methodParameterAdaptor.applyAsInt(index);
                if (i >= 0 && i < executable.getParameterCount()) {
                    for (Annotation annotation : annotations) {
                        String descriptor = Type.getDescriptor(annotation.annotationType());
                        var av = methodVisitor.visitParameterAnnotation(i, descriptor, true);
                        processAnnotation(annotation, av);
                    }
                }
                index++;
            }
        }
    }

    private static boolean isConstantField(Field field) {
        var modifiers = field.getModifiers();
        return Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    public static void processAnnotation(Annotation annotation, AnnotationVisitor av) {
        if (av != null) {
            for (Method m : annotation.annotationType().getDeclaredMethods()) {
                try {
                    var attributeValue = m.invoke(annotation);
                    Class<? extends Object> attributeType = attributeValue.getClass();
                    if (attributeType.isArray()) {
                        var arrayVisitor = av.visitArray(m.getName());
                        var array = (Object[]) attributeValue;
                        for (Object o : array) {
                            visitNonArrayAnnotationAttribute(arrayVisitor, null, o);
                        }
                        arrayVisitor.visitEnd();
                    } else {
                        visitNonArrayAnnotationAttribute(av, m.getName(), attributeValue);
                    }
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    // Skip inaccessible annotation attributes
                }
            }
            av.visitEnd();
        }
    }

    private static void visitNonArrayAnnotationAttribute(AnnotationVisitor av,
                                                         String attributeName,
                                                         Object attributeValue) {
        Class<? extends Object> attributeType = attributeValue.getClass();
        if (attributeValue instanceof Class<?> class1) {
            av.visit(attributeName, Type.getType(class1));
        } else if (attributeType.isEnum()) {
            av.visitEnum(attributeName, Type.getDescriptor(attributeType), attributeValue.toString());
        } else if (attributeValue instanceof Annotation annotation) {
            var av1 = av.visitAnnotation(attributeName, Type.getDescriptor(annotation.annotationType()));
            processAnnotation(annotation, av1);
        } else {
            av.visit(attributeName, attributeValue);
        }
    }

    /**
     * @return The name for new generated class.
     */
    public String getClassName() {
        return className;
    }

}
