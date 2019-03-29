package org.openl.gen.writers;

import java.util.Collections;
import java.util.HashMap;
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
public class ToStringWriter extends MethodWriter {
    private static final Map<String, String> PRIMITIVE_DESCRIPTORS = Collections
        .unmodifiableMap(new HashMap<String, String>(8, 1) {
            {
                put(byte.class, 'I');
                put(short.class, 'I');
                put(int.class, 'I');
                put(char.class, 'C');
                put(boolean.class, 'Z');
                put(long.class, 'J');
                put(float.class, 'F');
                put(double.class, 'D');
            }

            private void put(Class<?> clazz, char type) {
                put(clazz.getName(), "(" + type + ")Ljava/lang/StringBuilder;");
            }
        });

    private static final Map<String, String> ARRAY_OF_PRIMITIVES_DESCRIPTORS = Collections
        .unmodifiableMap(new HashMap<String, String>(8, 1) {
            {
                put(byte[].class);
                put(short[].class);
                put(int[].class);
                put(char[].class);
                put(boolean[].class);
                put(long[].class);
                put(float[].class);
                put(double[].class);
            }

            private void put(Class<?> clazz) {
                put(clazz.getName(), "(" + clazz.getName() + ")Ljava/lang/String;");
            }
        });

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public ToStringWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);
    }

    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);

        // create StringBuilder
        methodVisitor.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");

        // write ClassName
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        invokeAppendClassName(methodVisitor);

        // write fields
        invokeAppendValue(methodVisitor, "{ ");

        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            invokeAppendValue(methodVisitor, field.getKey() + "=");

            pushFieldToStack(methodVisitor, 0, field.getKey());

            String type = field.getValue().getTypeName();
            if (PRIMITIVE_DESCRIPTORS.containsKey(type)) {
                invokeAppendPrimitive(methodVisitor, type);
            } else if (type.charAt(0) == '[') {
                if (ARRAY_OF_PRIMITIVES_DESCRIPTORS.containsKey(type)) {
                    invokeAppendArrayOfPrimitives(methodVisitor, type);
                } else {
                    invokeAppendArrayOfObjects(methodVisitor);
                }
            } else {
                invokeAppendObject(methodVisitor);
            }

            invokeAppendValue(methodVisitor, " ");
        }
        invokeAppendValue(methodVisitor, "}");

        // return
        invokeVirtual(methodVisitor, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        methodVisitor.visitInsn(Opcodes.ARETURN);
        methodVisitor.visitMaxs(0, 0);
    }

    private void invokeAppendArrayOfObjects(MethodVisitor methodVisitor) {
        String desc = "([Ljava/lang/Object;)Ljava/lang/String;";
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "deepToString", desc);
        invokeAppendString(methodVisitor);
    }

    private void invokeAppendArrayOfPrimitives(MethodVisitor methodVisitor, String type) {
        String desc = ARRAY_OF_PRIMITIVES_DESCRIPTORS.get(type);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", desc);
        invokeAppendString(methodVisitor);
    }

    private void invokeAppendClassName(MethodVisitor methodVisitor) {
        invokeVirtual(methodVisitor, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        invokeVirtual(methodVisitor, "java/lang/Class", "getSimpleName", "()Ljava/lang/String;");
        invokeAppendString(methodVisitor);
    }

    private void invokeAppendValue(MethodVisitor methodVisitor, String str) {
        methodVisitor.visitLdcInsn(str);
        invokeAppendString(methodVisitor);
    }

    private void invokeAppendObject(MethodVisitor methodVisitor) {
        String desc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
        invokeAppend(methodVisitor, desc);
    }

    private void invokeAppendPrimitive(MethodVisitor methodVisitor, String type) {
        String desc = PRIMITIVE_DESCRIPTORS.get(type);
        invokeAppend(methodVisitor, desc);
    }

    private void invokeAppendString(MethodVisitor methodVisitor) {
        invokeAppend(methodVisitor, "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    }

    private void invokeAppend(MethodVisitor methodVisitor, String desc) {
        invokeVirtual(methodVisitor, "java/lang/StringBuilder", "append", desc);
    }

    private void invokeVirtual(MethodVisitor methodVisitor, String owner, String method, String desc) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, method, desc);
    }
}
