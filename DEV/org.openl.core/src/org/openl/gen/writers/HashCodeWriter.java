package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.gen.FieldDescription;

/**
 * Generates a hashCode() method. This method calculates a hashcode using the following expression:
 *
 * <pre>
 * {@code
 *     int hash = 5;
 *     hash = 31 * hash + hashCode(field)
 * }
 * </pre>
 *
 * @author Yury Molchan
 */
public class HashCodeWriter extends DefaultBeanByteCodeWriter {

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public HashCodeWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, null, allFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        MethodVisitor mv;
        mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);

        // hash = 5
        mv.visitInsn(Opcodes.ICONST_5);

        // generating hash code by fields
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            String fieldName = field.getKey();
            FieldDescription fd = field.getValue();
            String typeDescriptor = fd.getTypeDescriptor();
            String typeName = fd.getTypeName();

            // hash *= 31
            mv.visitIntInsn(Opcodes.BIPUSH, 31);
            mv.visitInsn(Opcodes.IMUL);

            // getField
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, getBeanNameWithPackage(), fieldName, typeDescriptor);

            // c = ?
            calculateHashCode(mv, typeName);

            // hash += c
            mv.visitInsn(Opcodes.IADD);
        }
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
    }

    private void calculateHashCode(MethodVisitor mv, String type) {
        if ("double".equals(type)) {
            invoke(mv, "java/lang/Double", "hashCode", "(D)I");
        } else if ("float".equals(type)) {
            invoke(mv, "java/lang/Float", "hashCode", "(F)I");
        } else if ("long".equals(type)) {
            invoke(mv, "java/lang/Long", "hashCode", "(J)I");
        } else if ("int".equals(type) || "short".equals(type) || "byte".equals(type) || "char".equals(type)) {
            // No conversions
        } else if ("boolean".equals(type)) {
            invoke(mv, "java/lang/Boolean", "hashCode", "(Z)I");
        } else if (type.charAt(0) == '[' && type.length() == 2) { // Array of primitives
            invoke(mv, "java/util/Arrays", "hashCode", "(" + type + ")I");
        } else if (type.startsWith("[L")) { // Array of objects
            invoke(mv, "java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I");
        } else if (type.startsWith("[[")) { // Multi array
            invoke(mv, "java/util/Arrays", "deepHashCode", "([Ljava/lang/Object;)I");
        } else {
            invoke(mv, "java/util/Objects", "hashCode", "(Ljava/lang/Object;)I");
        }
    }

    private static void invoke(MethodVisitor mv, String clazz, String methodName, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, clazz, methodName, descriptor, false);
    }
}
