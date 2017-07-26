package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.asm.invoker.Invokers;
import org.openl.rules.asm.invoker.StringBuilderInvoker;

import static  org.openl.rules.datatype.gen.ByteCodeGeneratorHelper.*;

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
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);

        // create StringBuilder
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor
                .visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "()V");

        // write ClassName
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        Invokers.GET_CLASS.invoke(methodVisitor);
        Invokers.GET_CLASS_NAME.invoke(methodVisitor);
        StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);

        // write fields
        methodVisitor.visitLdcInsn("{ ");
        StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);

        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            methodVisitor.visitLdcInsn(field.getKey() + "=");
            StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);

            pushFieldToStack(methodVisitor, 0, field.getKey());

            FieldDescription fd = field.getValue();
            Class<?> type = fd.getType();
            if (fd.isArray()) {
                String descriptor = Type.getMethodDescriptor(Type.getType(String.class), Type.getType(type));
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ArrayUtils.class), "toString", descriptor);
                StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);
            } else if (short.class.equals(type) || byte.class.equals(type)){
                String descriptor = Type.getMethodDescriptor(Type.getType(Integer.class), Type.getType(type));
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", descriptor);
                Invokers.INT_VALUE.invoke(methodVisitor);
                StringBuilderInvoker.getAppend(int.class).invoke(methodVisitor);
            }
            else {
                StringBuilderInvoker.getAppend(type).invoke(methodVisitor);
            }
            
            methodVisitor.visitLdcInsn(" ");
            StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);
        }
        methodVisitor.visitLdcInsn("}");
        StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);

        // return
        StringBuilderInvoker.getToString().invoke(methodVisitor);
        methodVisitor.visitInsn(getConstantForReturn(String.class));
        methodVisitor.visitMaxs(0, 0);
    }
}
