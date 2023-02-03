package org.openl.rules.serialization;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;

class SpreadsheetResultBeanClassMixInAnnotationsWriter extends ClassVisitor {
    private final String className;
    private final Class<?> originalMixInClass;
    private final String rootName;

    public SpreadsheetResultBeanClassMixInAnnotationsWriter(ClassVisitor delegatedClassVisitor,
            String className,
            Class<?> originalMixInClass,
            String rootName) {
        super(Opcodes.ASM5, delegatedClassVisitor);
        this.className = className;
        this.originalMixInClass = originalMixInClass;
        this.rootName = rootName;
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
            av.visitEnum("value", Type.getDescriptor(JsonInclude.Include.class), JsonInclude.Include.NON_NULL.name());
            av.visitEnd();
        }
        if (StringUtils.isNotBlank(rootName) && !originalMixInClass.isAnnotationPresent(JsonRootName.class)) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonRootName.class), true);
            av.visit("value", rootName);
            av.visitEnd();
        }
    }
}
