package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;
import static  org.openl.rules.datatype.gen.ByteCodeGeneratorHelper.*;

public class ToStringWriter extends MethodWriter {

    public static final String METHOD_NAME_APPEND = "append";
    public static final String METHOD_NAME_TO_STRING = "toString";
    public static final String METHOD_NAME_INT_VALUE = "intValue";
    public static final String METHOD_NAME_VALUE_OF = "valueOf";
    public static final String METHOD_NAME_GET_CLASS = "getClass";
    public static final String METHOD_NAME_GET_SIMPLE_NAME = "getSimpleName";

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
        invokeVirtual(methodVisitor, Object.class, METHOD_NAME_GET_CLASS, new Class<?>[] {});
        invokeVirtual(methodVisitor, Class.class, METHOD_NAME_GET_SIMPLE_NAME,
                new Class<?>[] {});
        invokeVirtual(
                methodVisitor, StringBuilder.class, METHOD_NAME_APPEND, new Class<?>[] { String.class });

        // write fields
        methodVisitor.visitLdcInsn("{ ");
        invokeVirtual(
                methodVisitor, StringBuilder.class, METHOD_NAME_APPEND, new Class<?>[] { String.class });

        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            methodVisitor.visitLdcInsn(field.getKey() + "=");
            invokeVirtual(
                    methodVisitor, StringBuilder.class, METHOD_NAME_APPEND, new Class<?>[] { String.class });

            pushFieldToStack(methodVisitor, 0, field.getKey());

            if (field.getValue().isArray()) { 
                invokeStatic(
                        methodVisitor, ArrayUtils.class, METHOD_NAME_TO_STRING,
                        new Class<?>[] { field.getValue().getType() });
            }
            if (short.class.equals(field.getValue().getType()) || byte.class.equals(field.getValue().getType())){
            	invokeStatic(methodVisitor, Integer.class, METHOD_NAME_VALUE_OF,
                        new Class<?>[] { field.getValue().getType() });
            	invokeVirtual(methodVisitor, Integer.class, METHOD_NAME_INT_VALUE,
                        new Class<?>[] {});
            	invokeVirtual(methodVisitor, StringBuilder.class, METHOD_NAME_APPEND,
                        new Class<?>[] { int.class });
            }
            else {
            	invokeVirtual(methodVisitor, StringBuilder.class, METHOD_NAME_APPEND,
                        new Class<?>[] { field.getValue().getType() });
            }
            
            methodVisitor.visitLdcInsn(" ");
            invokeVirtual(methodVisitor, StringBuilder.class, METHOD_NAME_APPEND,
                    new Class<?>[] { String.class });
        }
        methodVisitor.visitLdcInsn("}");
        invokeVirtual(methodVisitor, StringBuilder.class, METHOD_NAME_APPEND,
                new Class<?>[] { String.class });
		
        // return
        invokeVirtual(methodVisitor, StringBuilder.class, METHOD_NAME_TO_STRING,
                new Class<?>[] {});
        methodVisitor.visitInsn(getConstantForReturn(String.class));
        if (getTwoStackElementFieldsCount() > 0) {
            methodVisitor.visitMaxs(3, 1);
        } else {
            methodVisitor.visitMaxs(2, 1);
        }
    }
}
