package org.openl.rules.serialization;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

class SubtypeMixInClassWriter extends ClassVisitor {
    private final String className;
    private final Class<?> originalMixInClass;
    private final Class<?>[] subTypes;

    public SubtypeMixInClassWriter(ClassVisitor delegatedClassVisitor,
            String className,
            Class<?> originalMixInClass,
            Class<?>[] subTypes) {
        super(Opcodes.ASM5, delegatedClassVisitor);
        this.className = className;
        this.subTypes = subTypes;
        this.originalMixInClass = originalMixInClass;
    }

    @Override
    public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
        super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        if (subTypes.length > 0) {
            if (!originalMixInClass.isAnnotationPresent(JsonSubTypes.class)) {
                AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonSubTypes.class), true);
                AnnotationVisitor av1 = av.visitArray("value");
                for (Class<?> subTypeClass : subTypes) {
                    AnnotationVisitor av2 = av1.visitAnnotation(null, Type.getDescriptor(JsonSubTypes.Type.class));
                    av2.visit("value", Type.getType(subTypeClass));
                    av2.visitEnd();
                }
                av1.visitEnd();
                av.visitEnd();
            }
            if (!originalMixInClass.isAnnotationPresent(JsonTypeInfo.class)) {
                AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonTypeInfo.class), true);
                av.visitEnum("use", Type.getDescriptor(JsonTypeInfo.Id.class), JsonTypeInfo.Id.CLASS.name());
                av.visitEnd();
            }
        } else {
            if (!originalMixInClass.isAnnotationPresent(JsonTypeInfo.class)) {
                AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(JsonTypeInfo.class), true);
                av.visitEnum("use", Type.getDescriptor(JsonTypeInfo.Id.class), JsonTypeInfo.Id.NONE.name());
                av.visitEnd();
            }
        }
    }
}
