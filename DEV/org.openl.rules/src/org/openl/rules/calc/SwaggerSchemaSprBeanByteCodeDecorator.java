package org.openl.rules.calc;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.openl.gen.ByteCodeUtils;

final class SwaggerSchemaSprBeanByteCodeDecorator {

    private final boolean allOf;
    private final Set<CustomSpreadsheetResultOpenClass> targetClasses;

    public SwaggerSchemaSprBeanByteCodeDecorator(boolean allOf, Set<CustomSpreadsheetResultOpenClass> targetClasses) {
        this.targetClasses = new TreeSet<>(Comparator.comparing(CustomSpreadsheetResultOpenClass::getBeanClassName));
        this.targetClasses.addAll(targetClasses);
        this.allOf = allOf;
    }

    public byte[] decorate(byte[] classBytes) {
        ClassReader classReader = new ClassReader(classBytes);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
            @Override
            public void visitEnd() {
                AnnotationVisitor av = cv.visitAnnotation("Lio/swagger/v3/oas/annotations/media/Schema;", true);

                AnnotationVisitor allOfVisitor = av.visitArray(allOf ? "allOf" : "oneOf");
                for (var combinedOpenClass : targetClasses) {
                    allOfVisitor.visit(null, Type.getType(ByteCodeUtils.toTypeDescriptor(combinedOpenClass.getBeanClassName())));
                }
                allOfVisitor.visitEnd();

                av.visitEnd();

                super.visitEnd();
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // hide getter from swagger as they duplicate properties from allOf and oneOf classes
                boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
                boolean isGetter = (name.startsWith("get") || name.startsWith("is"))
                        && descriptor.startsWith("()") // no parameters
                        && !Type.getReturnType(descriptor).equals(Type.VOID_TYPE);
                if (isPublic && isGetter) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {

                        @Override
                        public void visitCode() {
                            var av = mv.visitAnnotation("Lio/swagger/v3/oas/annotations/media/Schema;", true);
                            av.visit("hidden", true);
                            av.visitEnd();
                            super.visitCode();
                        }
                    };
                }

                return mv;
            }
        };

        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }
}
