package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;

public class HashCodeWriter extends MethodWriter {
    
    public HashCodeWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }
 
    public void write(ClassWriter classWriter) {
        CodeVisitor codeVisitor;
        codeVisitor = classWriter.visitMethod(Constants.ACC_PUBLIC, "hashCode", String.format("()%s",
                ByteCodeGeneratorHelper.getJavaType(int.class)), null, null);

        // create HashCodeBuilder
        codeVisitor.visitTypeInsn(Constants.NEW, Type.getInternalName(HashCodeBuilder.class));
        codeVisitor.visitInsn(Constants.DUP);
        codeVisitor.visitMethodInsn(Constants.INVOKESPECIAL, Type.getInternalName(HashCodeBuilder.class), "<init>",
                "()V");

        // generating hash code by fields
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            pushFieldToStack(codeVisitor, 0, field.getKey());
            ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, HashCodeBuilder.class, "append",
                    new Class<?>[] { FieldDescription.getJavaClass(field.getValue()) });
        }
        ByteCodeGeneratorHelper.invokeVirtual(codeVisitor, HashCodeBuilder.class, "toHashCode", new Class<?>[] {});

        codeVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(int.class));
        if (getTwoStackElementFieldsCount() > 0) {
            codeVisitor.visitMaxs(3, 1);
        } else {
            codeVisitor.visitMaxs(2, 2);
        }

    }

}
