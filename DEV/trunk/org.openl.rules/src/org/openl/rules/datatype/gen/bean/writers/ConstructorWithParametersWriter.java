package org.openl.rules.datatype.gen.bean.writers;

import java.lang.reflect.Constructor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.generation.JavaClassGeneratorHelper;

public class ConstructorWithParametersWriter implements BeanByteCodeWriter {
    
    private String beanNameWithPackage;
    private Class<?> parentClass;
    private Map<String, FieldDescription> beanFields;
    private Map<String, FieldDescription> parentFields;
    private Map<String, FieldDescription> allFields;
    
    /**
     * Number of fields that will take 2 stack elements(like a double and long)
     */
    private int twoStackElementFieldsCount;
    
    public ConstructorWithParametersWriter(String beanNameWithPackage, Class<?> parentClass, Map<String, FieldDescription> beanFields, 
            Map<String, FieldDescription> parentFields, Map<String, FieldDescription> allFields) {
        this.beanNameWithPackage = beanNameWithPackage;
        this.parentClass = parentClass;
        this.beanFields = new LinkedHashMap<String, FieldDescription>(beanFields);
        this.parentFields = new LinkedHashMap<String, FieldDescription>(parentFields);
        this.allFields = new LinkedHashMap<String, FieldDescription>(allFields);
        this.twoStackElementFieldsCount = ByteCodeGeneratorHelper.getTwoStackElementFieldsCount(allFields);
    }
    
    public void write(ClassWriter classWriter) {
        CodeVisitor codeVisitor;
        
        Constructor<?> parentConstructorWithFields = null;
        if(parentClass != null){
            parentConstructorWithFields = JavaClassGeneratorHelper.getBeanConstructorWithAllFields(parentClass, parentFields.size());
        }
        int i = 1;
        int stackSizeForParentConstructorCall = 0;
        if(parentConstructorWithFields == null){
            codeVisitor = classWriter.visitMethod(Constants.ACC_PUBLIC, "<init>", ByteCodeGeneratorHelper.getMethodSignatureForByteCode(
                    beanFields, null), null, null);
            codeVisitor.visitVarInsn(Constants.ALOAD, 0);
            if (parentClass == null) {
                codeVisitor.visitMethodInsn(Constants.INVOKESPECIAL, ByteCodeGeneratorHelper.JAVA_LANG_OBJECT, "<init>", "()V");
            }else{
                codeVisitor.visitMethodInsn(Constants.INVOKESPECIAL, Type.getInternalName(parentClass), "<init>", "()V");
            }
        }else{
            codeVisitor = classWriter.visitMethod(Constants.ACC_PUBLIC, "<init>", ByteCodeGeneratorHelper.getMethodSignatureForByteCode(
                    allFields, null), null, null);
            codeVisitor.visitVarInsn(Constants.ALOAD, 0);            

            // push to stack all parameters for parent constructor
            for (Map.Entry<String, FieldDescription> field : parentFields.entrySet()) {
                FieldDescription fieldType = field.getValue();
                codeVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(fieldType), i);
                if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
                    i += 2;
                } else {
                    i++;
                }
            }

            // invoke parent constructor with fields
            stackSizeForParentConstructorCall = i;
            codeVisitor.visitMethodInsn(Constants.INVOKESPECIAL, Type.getInternalName(parentClass),
                    "<init>", ByteCodeGeneratorHelper.getMethodSignatureForByteCode(parentFields, null));
        }

        // set all fields that is not presented in parent
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            String fieldName = field.getKey();
            if (parentClass == null || parentFields.get(fieldName) == null) {
                // there is no such field in parent class
                FieldDescription fieldType = field.getValue();
                codeVisitor.visitVarInsn(Constants.ALOAD, 0);
                codeVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(fieldType), i);
                codeVisitor.visitFieldInsn(Constants.PUTFIELD, beanNameWithPackage, fieldName, ByteCodeGeneratorHelper.getJavaType(fieldType));
                if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
                    i += 2;
                } else {
                    i++;
                }
            }
        }
        codeVisitor.visitInsn(Constants.RETURN);
        if (twoStackElementFieldsCount > 0) {
            codeVisitor.visitMaxs(3 + stackSizeForParentConstructorCall, allFields.size() + 1
                    + twoStackElementFieldsCount);
        } else {
            codeVisitor.visitMaxs(2 + stackSizeForParentConstructorCall, allFields.size() + 1);
        }
        
    }

}
