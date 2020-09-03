package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.gen.FieldDescription;

/**
 * Generates a toString() method. This method uses only JRE classes for building a string, such as
 * {@link java.util.Arrays} and {@link java.lang.StringBuilder}.
 *
 * @author Yury Molchan
 */
public class ToStringWriter extends DefaultBeanByteCodeWriter {
    private static final int MAX_FIELDS = 100;

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public ToStringWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, null, allFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);

        // create StringBuilder
        methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        String type = getBeanNameWithPackage().substring(getBeanNameWithPackage().lastIndexOf('/') + 1);
        // write fields
        invokeAppendValue(methodVisitor, type + "{");
        int i = 0;
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            String fieldName = field.getKey();
            FieldDescription fd = field.getValue();
            String typeName = fd.getTypeName();
            String typeDescriptor = fd.getTypeDescriptor();

            invokeAppendValue(methodVisitor, " " + field.getKey() + "=");

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getBeanNameWithPackage(), fieldName, typeDescriptor);

            appendValue(methodVisitor, typeName);

            i++;
            if (i > MAX_FIELDS) {
                invokeAppendValue(methodVisitor, "...");
                break;
            }
        }
        invokeAppendValue(methodVisitor, " }");

        // return
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false);
        methodVisitor.visitInsn(Opcodes.ARETURN);
        methodVisitor.visitMaxs(0, 0);
    }

    private void appendValue(MethodVisitor mv, String type) {
        if ("double".equals(type)) {
            invokeAppend(mv, "(D)Ljava/lang/StringBuilder;");
        } else if ("float".equals(type)) {
            invokeAppend(mv, "(F)Ljava/lang/StringBuilder;");
        } else if ("long".equals(type)) {
            invokeAppend(mv, "(J)Ljava/lang/StringBuilder;");
        } else if ("int".equals(type) || "short".equals(type) || "byte".equals(type)) {
            invokeAppend(mv, "(I)Ljava/lang/StringBuilder;");
        } else if ("char".equals(type)) {
            invokeAppend(mv, "(C)Ljava/lang/StringBuilder;");
        } else if ("boolean".equals(type)) {
            invokeAppend(mv, "(Z)Ljava/lang/StringBuilder;");
        } else if (type.charAt(0) == '[' && type.length() == 2) { // Array of primitives
            invoke(mv, "java/util/Arrays", "toString", "(" + type + ")Ljava/lang/String;");
            invokeAppend(mv, "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        } else if (type.startsWith("[L")) { // Array of objects
            invoke(mv, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;");
            invokeAppend(mv, "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        } else if (type.startsWith("[[")) { // Multi array
            invoke(mv, "java/util/Arrays", "deepToString", "([Ljava/lang/Object;)Ljava/lang/String;");
            invokeAppend(mv, "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        } else {
            invokeAppend(mv, "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        }

    }

    private void invokeAppendValue(MethodVisitor methodVisitor, String str) {
        methodVisitor.visitLdcInsn(str);
        invokeAppend(methodVisitor, "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    }

    private void invokeAppend(MethodVisitor methodVisitor, String desc) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", desc, false);
    }

    private static void invoke(MethodVisitor mv, String clazz, String methodName, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, clazz, methodName, descriptor, false);
    }
}
