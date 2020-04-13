package org.openl.gen.writers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.gen.FieldDescription;
import org.openl.gen.TypeDescription;

public class ConstructorWithParametersWriter extends DefaultBeanByteCodeWriter {

    private Map<String, FieldDescription> parentFields;
    private Map<String, FieldDescription> allFields;

    /**
     *
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param parentType class descriptor for super class.
     * @param fields fields of generating class.
     * @param parentFields fields of super class.
     */
    public ConstructorWithParametersWriter(String beanNameWithPackage,
            TypeDescription parentType,
            Map<String, FieldDescription> parentFields,
            Map<String, FieldDescription> fields) {
        super(beanNameWithPackage, parentType, fields);
        this.parentFields = parentFields != null ? new LinkedHashMap<>(parentFields) : new LinkedHashMap<>();
        this.allFields = new LinkedHashMap<>(this.parentFields);
        this.allFields.putAll(fields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;

        int i = 1;
        String parentName = getParentType().getTypeName().replace('.', '/');
        if (parentFields.isEmpty()) {
            methodVisitor = classWriter
                .visitMethod(Opcodes.ACC_PUBLIC, "<init>", getMethodSignatureForByteCode(getBeanFields()), null, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, parentName, "<init>", "()V");
        } else {
            // Parent fields are not empty only if parent class is datatype and constructor exists in generated class.
            methodVisitor = classWriter
                .visitMethod(Opcodes.ACC_PUBLIC, "<init>", getMethodSignatureForByteCode(allFields), null, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);

            // push to stack all parameters for parent constructor
            for (Map.Entry<String, FieldDescription> fieldEntry : parentFields.entrySet()) {
                FieldDescription field = fieldEntry.getValue();
                methodVisitor.visitVarInsn(getConstantForVarInsn(field), i);
                if (long.class.getName().equals(field.getTypeName()) || double.class.getName()
                    .equals(field.getTypeName())) {
                    i += 2;
                } else {
                    i++;
                }
            }

            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                parentName,
                "<init>",
                getMethodSignatureForByteCode(parentFields));
        }

        // Set all fields that is not presented in parent
        //
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            String fieldName = field.getKey();
            if (parentFields.get(fieldName) == null) {
                // there is no such field in parent class
                FieldDescription fieldType = field.getValue();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(getConstantForVarInsn(fieldType), i);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                    getBeanNameWithPackage(),
                    fieldName,
                    fieldType.getTypeDescriptor());
                if (long.class.getName().equals(fieldType.getTypeName()) || double.class.getName()
                    .equals(fieldType.getTypeName())) {
                    i += 2;
                } else {
                    i++;
                }
            }
        }
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(0, 0);
    }

    private static String getMethodSignatureForByteCode(Map<String, FieldDescription> params) {
        StringBuilder signatureBuilder = new StringBuilder("(");
        for (Map.Entry<String, FieldDescription> field : params.entrySet()) {
            String javaType = field.getValue().getTypeDescriptor();
            signatureBuilder.append(javaType);
        }
        signatureBuilder.append(")");
        signatureBuilder.append("V");
        return signatureBuilder.toString();
    }
}
