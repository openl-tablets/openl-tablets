package org.openl.util.generation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
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
    private final Logger log = LoggerFactory.getLogger(InterfaceTransformer.class);
    private final Class<?> classToTransform;
    private final String className;
    private final boolean processParamAnnotation;

    /**
     * @param interfaceToTransform Base class for generations.
     * @param className Name for new class(java notation: with .(dot) as the delimiter).
     */
    public InterfaceTransformer(Class<?> interfaceToTransform, String className) {
        this.classToTransform = interfaceToTransform;
        this.className = className;
        this.processParamAnnotation = true;
    }

    public InterfaceTransformer(Class<?> interfaceToTransform, String className, boolean processParamAnnotation) {
        this.classToTransform = interfaceToTransform;
        this.className = className;
        this.processParamAnnotation = processParamAnnotation;
    }

    /**
     * Reads class and passes class generation instructions to <code>classVisitor</code>. Similar to
     * org.objectweb.asm.ClassReader.accept(...)
     *
     * @param classVisitor Visitor to consume writing instructions.
     */
    public void accept(ClassVisitor classVisitor) {
        Class<?> superClass = classToTransform.getSuperclass();
        Constructor<?> superClassConstructor = null;
        if (!classToTransform.isInterface()) {
            for (Constructor<?> c : classToTransform.getDeclaredConstructors()) {
                if ((Modifier.isPublic(c.getModifiers()) || Modifier.isProtected(c.getModifiers())) && c
                    .getParameterCount() == 0) {
                    superClassConstructor = c;
                    break;
                }
            }
            if (superClassConstructor == null) {
                superClass = Object.class;
                try {
                    superClassConstructor = Object.class.getConstructor();
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        if (superClass == null) {
            superClass = Object.class;
        }
        classVisitor.visit(Opcodes.V1_8,
            classToTransform.isInterface() ? classToTransform.getModifiers()
                                           : classToTransform.getModifiers() | Modifier.ABSTRACT,
            className.replace('.', '/'),
            null,
            superClass.getName().replace('.', '/'),
            Arrays.stream(classToTransform.getInterfaces())
                .map(e -> e.getName().replace('.', '/'))
                .toArray(String[]::new));

        for (Annotation annotation : classToTransform.getAnnotations()) {
            AnnotationVisitor av = classVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
            processAnnotation(annotation, av);
        }

        for (Field field : classToTransform.getDeclaredFields()) {
            if (!field.isSynthetic()) {
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

        for (Method method : classToTransform.getDeclaredMethods()) {
            if (!method.isSynthetic()) {
                String ruleName = method.getName();
                MethodVisitor methodVisitor = classVisitor.visitMethod(
                    classToTransform.isInterface() ? method.getModifiers() : method.getModifiers() | Modifier.ABSTRACT,
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
                        org.objectweb.asm.commons.Method.getMethod(superClassConstructor));
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
            if (processParamAnnotation) {
                int index = 0;
                for (Annotation[] annotations : executable.getParameterAnnotations()) {
                    for (Annotation annotation : annotations) {
                        String descriptor = Type.getDescriptor(annotation.annotationType());
                        AnnotationVisitor av = methodVisitor.visitParameterAnnotation(index, descriptor, true);
                        processAnnotation(annotation, av);
                    }
                    index++;
                }
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
