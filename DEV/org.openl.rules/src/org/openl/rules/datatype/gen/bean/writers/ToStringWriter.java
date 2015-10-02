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

    public static final String METHOD_NAME_TO_STRING = "toString";
    public static final String METHOD_NAME_VALUE_OF = "valueOf";

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
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME_TO_STRING, String.format("()%s",
                getJavaType(String.class)), null, null);

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

            if (field.getValue().isArray()) { 
                invokeStatic(
                        methodVisitor, ArrayUtils.class, METHOD_NAME_TO_STRING,
                        new Class<?>[] { field.getValue().getType() });
            }
            if (short.class.equals(field.getValue().getType()) || byte.class.equals(field.getValue().getType())){
            	invokeStatic(methodVisitor, Integer.class, METHOD_NAME_VALUE_OF,
                        new Class<?>[] { field.getValue().getType() });
                Invokers.INT_VALUE.invoke(methodVisitor);
                StringBuilderInvoker.getAppend(int.class).invoke(methodVisitor);
            }
            else {
                StringBuilderInvoker.getAppend(field.getValue().getType()).invoke(methodVisitor);
            }
            
            methodVisitor.visitLdcInsn(" ");
            StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);
        }
        methodVisitor.visitLdcInsn("}");
        StringBuilderInvoker.getAppend(String.class).invoke(methodVisitor);

        // return
        StringBuilderInvoker.getToString().invoke(methodVisitor);
        methodVisitor.visitInsn(getConstantForReturn(String.class));
        if (getTwoStackElementFieldsCount() > 0) {
            methodVisitor.visitMaxs(3, 1);
        } else {
            methodVisitor.visitMaxs(2, 1);
        }
    }
}
