package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.types.writers.TypeWriter;

public class DefaultConstructorWriter extends DefaultBeanByteCodeWriter {
    
    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param parentClass class descriptor for super class.
     * @param beanFields fields of generating class.
     */
    public DefaultConstructorWriter(String beanNameWithPackage, Class<?> parentClass, Map<String, FieldDescription> beanFields) {
        super(beanNameWithPackage, parentClass, beanFields);
    }
    
    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;
        
        methodVisitor = writeDefaultConstructorDefinition(classWriter);
        // invokes the super class constructor
        if (getParentClass() == null) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ByteCodeGeneratorHelper.JAVA_LANG_OBJECT, "<init>", "()V");
        } else {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(getParentClass()), "<init>", "()V");
        }

        int stackVariable = writeDefaultFieldValues(methodVisitor);

        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(stackVariable, 1);
    }

    protected MethodVisitor writeDefaultConstructorDefinition(ClassWriter classWriter) {
        MethodVisitor methodVisitor;
        // creates a MethodWriter for the (implicit) constructor
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        // pushes the 'this' variable
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        return methodVisitor;
    }
    
    private int writeDefaultFieldValues(MethodVisitor methodVisitor) {
        int result = 1; // the default stack element variable value (if there is no any default values)
        
        if (isAnyDefaultValue()) {
            result = writeAtLeast1DefaultValue(methodVisitor);
        } 
        return result;
    }
    
    private int writeAtLeast1DefaultValue(MethodVisitor methodVisitor) {
        int minStackVariable = 2; // as there are at least 1 default value, stack trace variable value will be at least 2.
        
        int[] stackVariables = new int[getBeanFields().entrySet().size()];
        int index = 0;
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            FieldDescription fieldDescription = field.getValue();            
            
            if (fieldDescription.hasDefaultValue()) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                
                TypeWriter typeWriter = ByteCodeGeneratorHelper.getTypeWriter(fieldDescription);
                stackVariables[index] = typeWriter.writeFieldValue(methodVisitor, fieldDescription);

                String fieldTypeName = ByteCodeGeneratorHelper.getJavaType(fieldDescription);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), field.getKey(), fieldTypeName);
            } else {
            	stackVariables[index] = minStackVariable;
            }
            index++;
        }
        return NumberUtils.max(stackVariables);
    }
    
    /**
     * 
     * @return true if there is any default value for any field.
     */
    private boolean isAnyDefaultValue() {
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            Object defaultValue = field.getValue().getDefaultValue();
            if (defaultValue != null) {
                return true;
            }
        }
        return false;
    }
}
