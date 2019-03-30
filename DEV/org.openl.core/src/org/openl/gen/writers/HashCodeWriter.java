package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.openl.gen.FieldDescription;

/**
 * Generates a hashCode() method. This method calculates a hashcode using the following expression:
 * 
 * <pre>
 * {@code
 *     int hash = 5;
 *     hash = 31 * hash + field.hashCode()
 * }
 * </pre>
 * 
 * @author Yury Molchan
 */
public class HashCodeWriter extends MethodWriter {

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public HashCodeWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        MethodVisitor mv;
        mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);

        // hash = 5
        mv.visitInsn(Opcodes.ICONST_5);

        // generating hash code by fields
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            // hash *= 31
            mv.visitIntInsn(Opcodes.BIPUSH, 31);
            mv.visitInsn(Opcodes.IMUL);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { Opcodes.INTEGER });

            // getField
            pushFieldToStack(mv, 0, field.getKey());

            // c = ?
            final String type = field.getValue().getTypeName();
            if ("double".equals(type)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "doubleToLongBits", "(D)J");
                hash64bits(mv);
            } else if ("long".equals(type)) {
                hash64bits(mv);
            } else if ("float".equals(type)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "floatToIntBits", "(F)I");
            } else if ("int".equals(type) || "short".equals(type) || "byte".equals(type) || "char".equals(type)) {
                // No conversions
            } else if ("boolean".equals(type)) {
                Label zero = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, zero);
                mv.visitInsn(Opcodes.ICONST_1);
                Label end = new Label();
                mv.visitJumpInsn(Opcodes.GOTO, end);
                mv.visitLabel(zero);
                mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { Opcodes.INTEGER });
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitLabel(end);
            } else if (type.charAt(0) == '[' && type.length() == 2) { // Array of primitives
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "(" + type + ")I");
            } else if (type.startsWith("[L")) { // Array of objects
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I");
            } else if (type.startsWith("[[")) { // Multi array
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "deepHashCode", "([Ljava/lang/Object;)I");
            } else {
                mv.visitInsn(Opcodes.DUP);
                Label isNull = new Label();
                mv.visitJumpInsn(Opcodes.IFNULL, isNull);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
                Label end = new Label();
                mv.visitJumpInsn(Opcodes.GOTO, end);
                mv.visitLabel(isNull);
                mv.visitFrame(Opcodes.F_FULL,
                    1,
                    new Object[] { getBeanNameWithPackage() },
                    2,
                    new Object[] { Opcodes.INTEGER, type.replace('.', '/') });
                mv.visitInsn(Opcodes.POP);
                mv.visitInsn(Opcodes.ICONST_0); // Replace null with zero
                mv.visitLabel(end);
            }
            mv.visitFrame(Opcodes.F_FULL,
                1,
                new Object[] { getBeanNameWithPackage() },
                2,
                new Object[] { Opcodes.INTEGER, Opcodes.INTEGER });

            // hash += c
            mv.visitInsn(Opcodes.IADD);
        }
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
    }

    private void hash64bits(MethodVisitor mv) {
        mv.visitInsn(Opcodes.DUP2);
        mv.visitIntInsn(Opcodes.BIPUSH, 32);
        mv.visitInsn(Opcodes.LUSHR);
        mv.visitInsn(Opcodes.LXOR);
        mv.visitInsn(Opcodes.L2I);
    }
}
