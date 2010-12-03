package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;

public class EqualsWriter extends MethodWriter {
    
    public EqualsWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }

    public void write(ClassWriter classWriter) {
        CodeVisitor codeVisitor;
        codeVisitor = classWriter.visitMethod(Constants.ACC_PUBLIC, "equals", String.format("(%s)%s",
                ByteCodeGeneratorHelper.getJavaType(Object.class), ByteCodeGeneratorHelper.getJavaType(boolean.class)), null, null);

        // create EqualsBuilder
        codeVisitor.visitTypeInsn(Constants.NEW, Type.getInternalName(EqualsBuilder.class));
        codeVisitor.visitInsn(Constants.DUP);
        codeVisitor
                .visitMethodInsn(Constants.INVOKESPECIAL, Type.getInternalName(EqualsBuilder.class), "<init>", "()V");

        Label comparingLabel = new Label();

        // check "instance of" object
        codeVisitor.visitVarInsn(Constants.ALOAD, 1);
        codeVisitor.visitTypeInsn(Constants.INSTANCEOF, getBeanNameWithPackage());
        codeVisitor.visitJumpInsn(Constants.IFNE, comparingLabel);
        codeVisitor.visitLdcInsn(Boolean.FALSE);
        codeVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(boolean.class));

        // cast
        codeVisitor.visitLabel(comparingLabel);
        codeVisitor.visitVarInsn(Constants.ALOAD, 1);
        codeVisitor.visitTypeInsn(Constants.CHECKCAST, getBeanNameWithPackage());
        codeVisitor.visitVarInsn(Constants.ASTORE, 2);

        // comparing by fields
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            pushFieldToStack(codeVisitor, 0, field.getKey());
            pushFieldToStack(codeVisitor, 2, field.getKey());

            Class<?> fieldType = FieldDescription.getJavaClass(field.getValue());
            ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, EqualsBuilder.class, "append", new Class<?>[] { fieldType, fieldType });
        }

        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, EqualsBuilder.class, "isEquals", new Class<?>[] {});

        codeVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(boolean.class));
        if (getTwoStackElementFieldsCount() > 0) {
            codeVisitor.visitMaxs(5, 3);
        } else {
            codeVisitor.visitMaxs(3, 3);
        }
    }
}
