package org.openl.rules.datatype.gen.bean.writers;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.StringTool;

public class GettersAndSettersWriter implements BeanByteCodeWriter {
    
    private String beanNameWithPackage;
    private Map<String, FieldDescription> beanFields;
    
    public GettersAndSettersWriter(String beanNameWithPackage, Map<String, FieldDescription> beanFields) {
        this.beanNameWithPackage = beanNameWithPackage;
        this.beanFields = new HashMap<String, FieldDescription>(beanFields);
    }
    
    /**
     * Writes getters and setters to the generated bean class.
     * 
     * @param beanNameWithPackage
     * @param classWriter
     */
    @Override
    public void write(ClassWriter classWriter) {
        for(Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            generateGetter(beanNameWithPackage, classWriter, field);
            generateSetter(beanNameWithPackage, classWriter, field);
        }

    }
    
    /**
     * Generates setter for the field.
     * 
     * @param beanNameWithPackage
     * @param classWriter
     * @param field
     */
    private void generateSetter(String beanNameWithPackage, ClassWriter classWriter, Map.Entry<String, FieldDescription> field) {
        CodeVisitor codeVisitor;
        String fieldName = field.getKey();
        FieldDescription fieldType = field.getValue();        
        String setterName = StringTool.getSetterName(field.getKey());
        
        codeVisitor = classWriter.visitMethod(Constants.ACC_PUBLIC,  setterName, String.format("(%s)V", 
            ByteCodeGeneratorHelper.getJavaType(fieldType)), null, null);
        codeVisitor.visitVarInsn(Constants.ALOAD, 0);
        codeVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(fieldType), 1);
        
        codeVisitor.visitFieldInsn(Constants.PUTFIELD, beanNameWithPackage, fieldName, ByteCodeGeneratorHelper.getJavaType(fieldType));
        codeVisitor.visitInsn(Constants.RETURN);
        
        // long and double types are the biggest ones, so they use a maximum of three stack  
        // elements and three local variables for setter method.
        if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
            codeVisitor.visitMaxs(3, 3);
        } else {
            codeVisitor.visitMaxs(2, 2);
        }
    }
    
    /**
     * Generates getter for the field.
     * 
     * @param beanNameWithPackage
     * @param classWriter
     * @param field
     */
    private void generateGetter(String beanNameWithPackage, ClassWriter classWriter, Map.Entry<String, FieldDescription> field) {
        CodeVisitor codeVisitor;
        String fieldName = field.getKey();
        FieldDescription fieldType = field.getValue();
        String getterName = StringTool.getGetterName(fieldName);
        
        codeVisitor = classWriter.visitMethod(Constants.ACC_PUBLIC,  getterName, String.format("()%s",
            ByteCodeGeneratorHelper.getJavaType(fieldType)), null, null);
        codeVisitor.visitVarInsn(Constants.ALOAD, 0);
        codeVisitor.visitFieldInsn(Constants.GETFIELD, beanNameWithPackage, fieldName, ByteCodeGeneratorHelper.getJavaType(fieldType));
        codeVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(fieldType));
        
        // long and double types are the biggest ones, so they use a maximum of two stack  
        // elements and one local variable for getter method.
        if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
            codeVisitor.visitMaxs(2, 1);
        } else {
            codeVisitor.visitMaxs(1, 1);
        }
    }

}
