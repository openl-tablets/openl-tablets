package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.gen.FieldDescription;
import org.openl.util.ClassUtils;

public class SettersWriter extends DefaultBeanByteCodeWriter {

    public SettersWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, null, allFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            generateSetter(classWriter, field.getKey(), field.getValue());
        }
    }

    /**
     * Generates setter for the fieldEntry.
     *
     * @param classWriter
     * @param fieldName
     * @param fieldDescription
     */
    protected void generateSetter(ClassWriter classWriter, String fieldName, FieldDescription fieldDescription) {
        final String fieldType = fieldDescription.getTypeDescriptor();

        String setterName = ClassUtils.setter(fieldName);
        String methodDescriptor = "(" + fieldType + ")V";
        MethodVisitor methodVisitor = classWriter
            .visitMethod(Opcodes.ACC_PUBLIC, setterName, methodDescriptor, null, null);

        Label l0 = new Label();
        methodVisitor.visitLabel(l0);

        // this.fieldName = arg0
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(getConstantForVarInsn(fieldDescription), 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), fieldName, fieldType);
        methodVisitor.visitInsn(Opcodes.RETURN);

        // Add variable name to DEBUG
        Label l2 = new Label();
        methodVisitor.visitLabel(l2);
        methodVisitor.visitLocalVariable(fieldName, fieldType, null, l0, l2, 1);

        methodVisitor.visitMaxs(0, 0);
    }

}
