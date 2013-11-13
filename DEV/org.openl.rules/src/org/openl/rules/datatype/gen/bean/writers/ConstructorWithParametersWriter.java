package org.openl.rules.datatype.gen.bean.writers;

import java.lang.reflect.Constructor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.generation.JavaClassGeneratorHelper;

public class ConstructorWithParametersWriter extends DefaultBeanByteCodeWriter {
    
    private Map<String, FieldDescription> parentFields;
    private Map<String, FieldDescription> allFields;
    
    /**
     * Number of fields that will take 2 stack elements(like a double and long)
     */
    private int twoStackElementFieldsCount;
    
    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param parentClass class descriptor for super class.
     * @param beanFields fields of generating class.
     * @param parentFields fields of super class.
     * @param allFields collection of fields for current class and parent`s ones.
     */
    public ConstructorWithParametersWriter(String beanNameWithPackage, Class<?> parentClass, Map<String, FieldDescription> beanFields, 
            Map<String, FieldDescription> parentFields, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, parentClass, beanFields);
        this.parentFields = new LinkedHashMap<String, FieldDescription>(parentFields);
        this.allFields = new LinkedHashMap<String, FieldDescription>(allFields);
        this.twoStackElementFieldsCount = ByteCodeGeneratorHelper.getTwoStackElementFieldsCount(allFields);
    }
    
    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;

        Constructor<?> parentConstructorWithFields = null;
        if(getParentClass() != null){
            parentConstructorWithFields = JavaClassGeneratorHelper.getBeanConstructorWithAllFields(getParentClass(), parentFields.size());
        }
        int i = 1;
        int stackSizeForParentConstructorCall = 0;
        if(parentConstructorWithFields == null){
            methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", ByteCodeGeneratorHelper.getMethodSignatureForByteCode(
                    getBeanFields(), null), null, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            if (getParentClass() == null) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ByteCodeGeneratorHelper.JAVA_LANG_OBJECT, "<init>", "()V");
            } else{
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(getParentClass()), "<init>", "()V");
            }
        }else{
            methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", ByteCodeGeneratorHelper.getMethodSignatureForByteCode(
                    allFields, null), null, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);            

            // push to stack all parameters for parent constructor
            for (Map.Entry<String, FieldDescription> field : parentFields.entrySet()) {
                FieldDescription fieldType = field.getValue();
                methodVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(fieldType), i);
                if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
                    i += 2;
                } else {
                    i++;
                }
            }

            // invoke parent constructor with fields
            stackSizeForParentConstructorCall = i;
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(getParentClass()),
                    "<init>", ByteCodeGeneratorHelper.getMethodSignatureForByteCode(parentFields, null));
        }

        // set all fields that is not presented in parent
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            String fieldName = field.getKey();
            if (getParentClass() == null || parentFields.get(fieldName) == null) {
                // there is no such field in parent class
                FieldDescription fieldType = field.getValue();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(fieldType), i);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), fieldName, ByteCodeGeneratorHelper.getJavaType(fieldType));
                if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
                    i += 2;
                } else {
                    i++;
                }
            }
        }
        methodVisitor.visitInsn(Opcodes.RETURN);
        if (twoStackElementFieldsCount > 0) {
            methodVisitor.visitMaxs(3 + stackSizeForParentConstructorCall, allFields.size() + 1
                    + twoStackElementFieldsCount);
        } else {
            methodVisitor.visitMaxs(2 + stackSizeForParentConstructorCall, allFields.size() + 1);
        }
        
    }

}
