package org.openl.util.generation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.runtime.InterfaceGenerator;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * 
 * This class is similar to {@link ClassReader} from ASM framework. But it can
 * be used only for interface generation.
 * 
 * {@link InterfaceTransformer} uses base class that will be transformed,
 * classname for new class and {@link ClassVisitor} that will handle class
 * creation.
 * 
 * {@link InterfaceTransformer} reads methods with
 * signatures,constants,annotations and passes them to {@link ClassVisitor}.
 * 
 * 
 * @author PUdalau
 */
public class InterfaceTransformer {
    private final Log log = LogFactory.getLog(InterfaceTransformer.class);
    private Class<?> interfaceToTransform;
    private String className;

    /**
     * 
     * @param interfaceToTransform Base class for generations.
     * @param className Name for new class(java notation: with .(dot) as the
     *            delimiter).
     */
    public InterfaceTransformer(Class<?> interfaceToTransform, String className) {
        this.interfaceToTransform = interfaceToTransform;
        this.className = className;
    }

    /**
     * Reads class and passes class generation instructions to
     * <code>classVisitor</code>. Similar to
     * org.objectweb.asm.ClassReader.accept(...)
     * 
     * @param classVisitor Visitor to consume writing instructions.
     */
    public void accept(ClassVisitor classVisitor) {
        classVisitor.visit(Opcodes.V1_5,
            InterfaceGenerator.PUBLIC_ABSTRACT_INTERFACE,
            className.replace('.', '/'),
            null,
            InterfaceGenerator.JAVA_LANG_OBJECT,
            null);
        for (Annotation annotation : interfaceToTransform.getAnnotations()) {
            AnnotationVisitor av = classVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
            processAnnotation(annotation, av);
        }

        for (Field field : interfaceToTransform.getFields()) {
            if (isConstantField(field)) {
                try {
                    FieldVisitor fieldVisitor = classVisitor.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL,
                        field.getName(),
                        Type.getDescriptor(field.getType()),
                        null,
                        field.get(null));
                    if (fieldVisitor != null) {
                        for (Annotation annotation : field.getAnnotations()) {
                            AnnotationVisitor av = fieldVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()),
                                true);
                            processAnnotation(annotation, av);
                        }
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()){
                        log.error("Failed to process field '" + field.getName() + "'.",e);
                    }
                }
            }
        }

        for (Method method : interfaceToTransform.getMethods()) {
            String ruleName = method.getName();
            MethodVisitor methodVisitor = classVisitor.visitMethod(InterfaceGenerator.PUBLIC_ABSTRACT,
                ruleName,
                Type.getMethodDescriptor(method),
                null,
                null);
            if (methodVisitor != null) {
                for (Annotation annotation : method.getAnnotations()) {
                    AnnotationVisitor av = methodVisitor.visitAnnotation(Type.getDescriptor(annotation.annotationType()),
                        true);
                    processAnnotation(annotation, av);
                }
            }

        }
    }

    private static boolean isConstantField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    private void processAnnotation(Annotation annotation, AnnotationVisitor av) {
        Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(annotation);
        if (av != null) {
            for (Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {
                Object attributeValue = annotationAttribute.getValue();
                Class<? extends Object> attributeType = attributeValue.getClass();
                if (attributeType.isArray()) {
                    AnnotationVisitor arrayVisitor = av.visitArray(annotationAttribute.getKey());
                    Object[] array = (Object[]) attributeValue;
                    for (int i = 0; i < array.length; i++) {
                        visitNonArrayAnnotationAttribute(arrayVisitor, null, array[i]);
                    }
                    arrayVisitor.visitEnd();
                } else {
                    visitNonArrayAnnotationAttribute(av, annotationAttribute.getKey(), annotationAttribute.getValue());
                }
            }
            av.visitEnd();
        }
    }

    private void visitNonArrayAnnotationAttribute(AnnotationVisitor av, String attributeName, Object attributeValue) {
        Class<? extends Object> attributeType = attributeValue.getClass();
        if (attributeValue instanceof Class) {
            av.visit(attributeName, Type.getType((Class<?>) attributeValue));
        } else if (attributeType.isEnum()) {
            av.visitEnum(attributeName, Type.getDescriptor(attributeType), attributeValue.toString());
        } else {
            av.visit(attributeName, attributeValue);
        }
    }

    /**
     * @return Base class for generations.
     */
    public Class<?> getInterfaceToTransform() {
        return interfaceToTransform;
    }

    /**
     * @return The name for new generated class.
     */
    public String getClassName() {
        return className;
    }

}
