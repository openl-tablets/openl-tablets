package org.openl.rules.datatype.gen.bean.writers;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.types.writers.TypeWriter;

public class DefaultConstructorWriter implements BeanByteCodeWriter {
    
    private String beanNameWithPackage;
    
    private Class<?> parentClass;
    
    private Map<String, FieldDescription> beanFields;
    
    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param parentClass class descriptor for super class.
     * @param beanFields fields of generating class.
     */
    public DefaultConstructorWriter(String beanNameWithPackage, Class<?> parentClass, Map<String, FieldDescription> beanFields) {
        this.beanNameWithPackage = beanNameWithPackage;
        this.parentClass = parentClass;
        this.beanFields = new HashMap<String, FieldDescription>(beanFields);
    }
    
    public void write(ClassWriter classWriter) {

        MethodVisitor methodVisitor;
        // creates a MethodWriter for the (implicit) constructor
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        // pushes the 'this' variable
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        // invokes the super class constructor
        if (parentClass == null) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ByteCodeGeneratorHelper.JAVA_LANG_OBJECT, "<init>", "()V");
        } else {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                Type.getInternalName(parentClass),
                "<init>",
                "()V");
        }

        int stackVariable = writeDefaultFieldValues(methodVisitor);

        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(stackVariable, 1);
    }
    
    private int writeDefaultFieldValues(MethodVisitor methodVisitor) {
        int result = 1; // the default stack element variable value (if there is no any default values)
        
        if (isAnyDefaultvalue()) {
            result = processWritingDefaultValues(methodVisitor);
        } 
        return result;
    }
    
    private int processWritingDefaultValues(MethodVisitor methodVisitor) {
        int stackVariable = 2; // if there is any default value, stack trace variable value will be 2.
        
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            FieldDescription fieldType = field.getValue();
            Object defaultValue = fieldType.getDefaultValue();
            
            if (defaultValue != null) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                
                if (isPrimitive(fieldType)) {
                    int stackVariableAfterPrimitive = stackVariable;
                    TypeWriter typeWriter = ByteCodeGeneratorHelper.getTypeWriter(fieldType);
                    if (typeWriter != null) {
                        stackVariableAfterPrimitive = typeWriter.writeFieldValue(methodVisitor, fieldType);
                    }
                    if (stackVariable < 5) {
                        stackVariable = stackVariableAfterPrimitive;
                    }                    
                } else if  (isTypesEquals(String.class, fieldType)){
                    // write String fields
                    methodVisitor.visitLdcInsn(defaultValue);
                } else {
                    TypeWriter typeWriter = ByteCodeGeneratorHelper.getTypeWriter(Object.class);
                    stackVariable = typeWriter.writeFieldValue(methodVisitor, fieldType);
                }
                String fieldTypeName = ByteCodeGeneratorHelper.getJavaType(fieldType);                
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, beanNameWithPackage, field.getKey(), fieldTypeName);
            }
        }
        return stackVariable;
    }
    
    /**
     * 
     * @return true if there is any default value for any field.
     */
    private boolean isAnyDefaultvalue() {
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            Object defaultValue = field.getValue().getDefaultValue();
            if (defaultValue != null) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isPrimitive(FieldDescription fieldType) {
        Class<?> fieldClass = FieldDescription.getJavaClass(fieldType);
        return fieldClass.isPrimitive();        
    }
    
    private boolean isTypesEquals(Class<?> clazz, FieldDescription fieldType) {
        return clazz.equals(FieldDescription.getJavaClass(fieldType));
    }
}
