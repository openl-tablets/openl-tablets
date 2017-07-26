package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.asm.invoker.HashCodeBuilderInvoker;

public class HashCodeWriter extends MethodWriter {
    
    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public HashCodeWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }
 
    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);

        // create HashCodeBuilder
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(HashCodeBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(HashCodeBuilder.class), "<init>",
                "()V");

        // generating hash code by fields
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            pushFieldToStack(methodVisitor, 0, field.getKey());
            final Class<?> type = field.getValue().getType();
            HashCodeBuilderInvoker.getAppend(type).invoke(methodVisitor);
        }
        HashCodeBuilderInvoker.getToHashCode().invoke(methodVisitor);

        methodVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(int.class));
        methodVisitor.visitMaxs(0, 0);
    }

}
