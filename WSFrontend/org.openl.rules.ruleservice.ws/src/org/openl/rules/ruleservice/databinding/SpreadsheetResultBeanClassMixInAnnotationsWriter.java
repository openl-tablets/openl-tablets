package org.openl.rules.ruleservice.databinding;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.fasterxml.jackson.annotation.JsonInclude;

class SpreadsheetResultBeanClassMixInAnnotationsWriter extends ClassVisitor {
    private final String className;
    private final Class<?> originalMixInClass;

    public SpreadsheetResultBeanClassMixInAnnotationsWriter(ClassVisitor delegatedClassVisitor,
            String className,
            Class<?> originalMixInClass) {
        super(Opcodes.ASM5, delegatedClassVisitor);
        this.className = className;
        this.originalMixInClass = originalMixInClass;
    }

    @Override
    public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
        super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        if (!originalMixInClass.isAnnotationPresent(JsonInclude.class)) {
            AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonInclude.class), true);
            av.visitEnum("value", Type.getDescriptor(JsonInclude.Include.class), JsonInclude.Include.NON_NULL.name());
            av.visitEnd();
        }
    }
}