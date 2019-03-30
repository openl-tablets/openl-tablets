package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.openl.gen.FieldDescription;

/**
 * Generates a equals(Object) method. This method uses only JRE classes for deep comparing.
 *
 * @author Yury Molchan
 */
public class EqualsWriter extends MethodWriter {

    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public EqualsWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        MethodVisitor mv;
        mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);

        trueIfTheSame(mv);
        falseIfNull(mv);
        falseIfDifferentClassNames(mv);
        doCast(mv);

        Label retFalse = new Label();
        // comparing by fields
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            pushFieldToStack(mv, 0, field.getKey());
            pushFieldToStack(mv, 2, field.getKey());

            final String type = field.getValue().getTypeName();
            if ("double".equals(type)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "compare", "(DD)I");
                mv.visitJumpInsn(Opcodes.IFNE, retFalse);
            } else if ("float".equals(type)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "compare", "(FF)I");
                mv.visitJumpInsn(Opcodes.IFNE, retFalse);
            } else if ("long".equals(type)) {
                mv.visitInsn(Opcodes.LCMP);
                mv.visitJumpInsn(Opcodes.IFNE, retFalse);
            } else if ("int".equals(type) || "boolean".equals(type) || "short".equals(type) || "byte"
                .equals(type) || "char".equals(type)) {
                mv.visitJumpInsn(Opcodes.IF_ICMPNE, retFalse);
                // No conversions
            } else if (type.charAt(0) == '[' && type.length() == 2) { // Array of primitives
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "equals", "(" + type + type + ")Z");
                mv.visitJumpInsn(Opcodes.IFEQ, retFalse);
            } else if (type.startsWith("[L")) { // Array of objects
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/util/Arrays",
                    "equals",
                    "([Ljava/lang/Object;[Ljava/lang/Object;)Z");
                mv.visitJumpInsn(Opcodes.IFEQ, retFalse);
            } else if (type.startsWith("[[")) { // Multi array
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/util/Arrays",
                    "deepEquals",
                    "([Ljava/lang/Object;[Ljava/lang/Object;)Z");
                mv.visitJumpInsn(Opcodes.IFEQ, retFalse);
            } else {
                Label endif = new Label();
                Label isNull = new Label();
                mv.visitInsn(Opcodes.DUP_X1);
                mv.visitJumpInsn(Opcodes.IFNULL, isNull);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
                mv.visitJumpInsn(Opcodes.IFEQ, retFalse);
                mv.visitJumpInsn(Opcodes.GOTO, endif);

                mv.visitLabel(isNull);
                String internalType = type.replace('.', '/');
                mv.visitFrame(Opcodes.F_FULL,
                    3,
                    new Object[] { getBeanNameWithPackage(), "java/lang/Object", getBeanNameWithPackage() },
                    2,
                    new Object[] { internalType, internalType });

                mv.visitJumpInsn(Opcodes.IF_ACMPNE, retFalse);
                mv.visitLabel(endif);

            }
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        mv.visitInsn(Opcodes.ICONST_1);// true
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitLabel(retFalse);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.ICONST_0);// false
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitMaxs(0, 0);
    }

    private void doCast(MethodVisitor mv) {
        // cast
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, getBeanNameWithPackage());
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { getBeanNameWithPackage() }, 0, new Object[] {});
    }

    private void falseIfDifferentClassNames(MethodVisitor mv) {
        Label endif = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, endif); // this.class != other.class
        mv.visitInsn(Opcodes.ICONST_0);// false
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(endif);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }

    private void falseIfNull(MethodVisitor mv) {
        Label endif = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IFNONNULL, endif); // other == null
        mv.visitInsn(Opcodes.ICONST_0);// false
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(endif);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }

    private void trueIfTheSame(MethodVisitor mv) {
        Label endif = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IF_ACMPNE, endif); // this == other
        mv.visitInsn(Opcodes.ICONST_1);// true
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(endif);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }
}
