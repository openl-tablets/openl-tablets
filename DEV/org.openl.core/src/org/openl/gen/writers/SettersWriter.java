package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.gen.FieldDescription;
import org.openl.util.ClassUtils;

public class SettersWriter extends MethodWriter {

    public SettersWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, allFields);        
    }
    
    public void write(ClassWriter classWriter) {
        for (Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            if (validField(field.getKey(), field.getValue())) {
                generateSetter(classWriter, field);
            }
        }
    }
    
    /**
     * Generates setter for the fieldEntry.
     * 
     * @param classWriter
     * @param fieldEntry
     */
    protected void generateSetter(ClassWriter classWriter, Map.Entry<String, FieldDescription> fieldEntry) {
        String fieldName = fieldEntry.getKey();
        FieldDescription field = fieldEntry.getValue();
        
        MethodVisitor methodVisitor;
        
        methodVisitor = writeMethodSignature(classWriter, field, fieldName);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(getConstantForVarInsn(field), 1);
        
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), fieldName, field.getTypeDescriptor());
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(0, 0);
    }

    protected MethodVisitor writeMethodSignature(ClassWriter classWriter, FieldDescription fieldType, String fieldName) {
        String setterName = ClassUtils.setter(fieldName);
        final String javaType = fieldType.getTypeDescriptor();
        final String format = new StringBuilder(64).append('(').append(javaType).append(")V").toString();
        return classWriter.visitMethod(Opcodes.ACC_PUBLIC,  setterName, format, null, null);
    }

}
