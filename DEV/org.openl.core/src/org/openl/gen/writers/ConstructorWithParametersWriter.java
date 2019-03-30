package org.openl.gen.writers;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.gen.FieldDescription;

public class ConstructorWithParametersWriter extends DefaultBeanByteCodeWriter {

    private Map<String, FieldDescription> parentFields;
    private Map<String, FieldDescription> allFields;

    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param parentClass class descriptor for super class.
     * @param beanFields fields of generating class.
     * @param parentFields fields of super class.
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public ConstructorWithParametersWriter(String beanNameWithPackage,
            Class<?> parentClass,
            Map<String, FieldDescription> beanFields,
            Map<String, FieldDescription> parentFields,
            Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, parentClass, beanFields);
        this.parentFields = new LinkedHashMap<>(parentFields);
        this.allFields = new LinkedHashMap<>(allFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;

        Constructor<?> parentConstructor = null;
        // Find the parent constructor with the appropriate number of fields
        //
        for (Constructor<?> constructor : getParentClass().getConstructors()) {
            if (constructor.getParameterTypes().length == parentFields.size()) {
                parentConstructor = constructor;
                break;
            }
        }
        int i = 1;
        String parentName = Type.getInternalName(getParentClass());
        if (parentConstructor == null) {
            methodVisitor = classWriter
                .visitMethod(Opcodes.ACC_PUBLIC, "<init>", getMethodSignatureForByteCode(getBeanFields()), null, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, parentName, "<init>", "()V");
        } else {
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
