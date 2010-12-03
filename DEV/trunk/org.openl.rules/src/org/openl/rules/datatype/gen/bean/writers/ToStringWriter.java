package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;

public class ToStringWriter extends MethodWriter {
    
    public ToStringWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }
    
    @Override
    public void write(ClassWriter classWriter) {
        CodeVisitor codeVisitor;
        codeVisitor = classWriter.visitMethod(Constants.ACC_PUBLIC, "toString", String.format("()%s",
                ByteCodeGeneratorHelper.getJavaType(String.class)), null, null);

        // create StringBuilder
        codeVisitor.visitTypeInsn(Constants.NEW, Type.getInternalName(StringBuilder.class));
        codeVisitor.visitInsn(Constants.DUP);
        codeVisitor
                .visitMethodInsn(Constants.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "()V");

        // write ClassName
        codeVisitor.visitVarInsn(Constants.ALOAD, 0);
        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, Object.class, "getClass", new Class<?>[] {});
        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, Class.class, "getSimpleName", new Class<?>[] {});
        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });

        // write fields
        codeVisitor.visitLdcInsn("{ ");
        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            codeVisitor.visitLdcInsn(field.getKey() + "=");
            ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });

            pushFieldToStack(codeVisitor, 0, field.getKey());
            if (field.getValue().isArray()) { 
                ByteCodeGeneratorHelper.invokeStatic(codeVisitor, ArrayUtils.class, "toString", new Class<?>[] { FieldDescription.getJavaClass(field.getValue()) });
            }
            ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, StringBuilder.class, "append", new Class<?>[] { FieldDescription.getJavaClass(field.getValue()) });

            codeVisitor.visitLdcInsn(" ");
            ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });
        }
        codeVisitor.visitLdcInsn("}");
        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, StringBuilder.class, "append", new Class<?>[] { String.class });

        // return
        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, StringBuilder.class, "toString", new Class<?>[] {});
        codeVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(String.class));
        if (getTwoStackElementFieldsCount() > 0) {
            codeVisitor.visitMaxs(3, 1);
        } else {
            codeVisitor.visitMaxs(2, 1);
        }
    }
}
