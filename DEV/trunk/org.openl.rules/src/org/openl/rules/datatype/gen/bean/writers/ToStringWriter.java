package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;

public class ToStringWriter extends MethodWriter {

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public ToStringWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }

    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "toString", String.format("()%s",
                ByteCodeGeneratorHelper.getJavaType(String.class)), null, null);

        // create StringBuilder
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor
                .visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "()V");

        // write ClassName
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, Object.class, "getClass", new Class<?>[] {});
        ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, Class.class, "getSimpleName", new Class<?>[] {});
        ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });

        // write fields
        methodVisitor.visitLdcInsn("{ ");
        ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            methodVisitor.visitLdcInsn(field.getKey() + "=");
            ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });

            pushFieldToStack(methodVisitor, 0, field.getKey());
            if (field.getValue().isArray()) { 
                ByteCodeGeneratorHelper.invokeStatic(methodVisitor, ArrayUtils.class, "toString", new Class<?>[] { FieldDescription.getJavaClass(field.getValue()) });
            }
            ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, StringBuilder.class, "append", new Class<?>[] { FieldDescription.getJavaClass(field.getValue()) });

            methodVisitor.visitLdcInsn(" ");
            ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });
        }
        methodVisitor.visitLdcInsn("}");
        ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });

        // return
        ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, StringBuilder.class, "toString", new Class<?>[] {});
        methodVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(String.class));
        if (getTwoStackElementFieldsCount() > 0) {
            methodVisitor.visitMaxs(3, 1);
        } else {
            methodVisitor.visitMaxs(2, 1);
        }
    }
}
