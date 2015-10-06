package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.StringTool;

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
        methodVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(field), 1);
        
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), fieldName, ByteCodeGeneratorHelper.getJavaType(field));
        methodVisitor.visitInsn(Opcodes.RETURN);
        
        // long and double types are the biggest ones, so they use a maximum of three stack  
        // elements and three local variables for setter method.
        if (long.class.equals(field.getType()) || double.class.equals(field.getType())) {
            methodVisitor.visitMaxs(3, 3);
        } else {
            methodVisitor.visitMaxs(2, 2);
        }
    }

    protected MethodVisitor writeMethodSignature(ClassWriter classWriter, FieldDescription fieldType, String fieldName) {
        String setterName = StringTool.getSetterName(fieldName);
        final String javaType = ByteCodeGeneratorHelper.getJavaType(fieldType);
        final String format = new StringBuilder(64).append('(').append(javaType).append(")V").toString();
        return classWriter.visitMethod(Opcodes.ACC_PUBLIC,  setterName, format, null, null);
    }

}
