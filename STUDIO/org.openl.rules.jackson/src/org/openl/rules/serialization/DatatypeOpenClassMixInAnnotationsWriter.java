package org.openl.rules.serialization;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.fasterxml.jackson.annotation.JsonInclude;

public class DatatypeOpenClassMixInAnnotationsWriter extends ClassVisitor {
    private final String className;
    private final Class<?> originalMixInClass;

    public DatatypeOpenClassMixInAnnotationsWriter(ClassVisitor delegatedClassVisitor,
            String className,
            Class<?> originalMixInClass) {
        super(Opcodes.ASM5, delegatedClassVisitor);
        this.className = className;
        this.originalMixInClass = originalMixInClass;
    }

    @Override
    public void visit(final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        super.visit(version, access, className.replace('.', '/'), signature, superName, interfaces);
        if (!originalMixInClass.isAnnotationPresent(JsonInclude.class)) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonInclude.class), true);
            av.visitEnum("value", Type.getDescriptor(JsonInclude.Include.class), JsonInclude.Include.NON_EMPTY.name());
            av.visitEnd();
        }
    }
}
