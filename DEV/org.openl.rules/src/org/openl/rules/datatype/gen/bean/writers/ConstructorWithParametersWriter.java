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

    public static final String INIT = "<init>";
    public static final String V = "()V";
    private Map<String, FieldDescription> parentFields;
    private Map<String, FieldDescription> allFields;
    
    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
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
        this.parentFields = new LinkedHashMap<String, FieldDescription>(parentFields);
        this.allFields = new LinkedHashMap<String, FieldDescription>(allFields);
    }
    
    public void write(ClassWriter classWriter) {
        MethodVisitor methodVisitor;

        Constructor<?> parentConstructor = null;
        if (getParentClass() != null) {
            // Find the parent constructor with the appropriate number of fields
            //
            parentConstructor =
                    JavaClassGeneratorHelper.getConstructorByFieldsCount(getParentClass(), parentFields.size());
        }
        int i = 1;
        if (parentConstructor == null) {
            methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, INIT,
                    ByteCodeGeneratorHelper.getMethodSignatureForByteCode(getBeanFields(), null), null, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            String parentName = getParentInternalName();
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, parentName, INIT, V);
        }
        else {
            methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, INIT,
                    ByteCodeGeneratorHelper.getMethodSignatureForByteCode(allFields, null), null, null);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);            

            // push to stack all parameters for parent constructor
            for (Map.Entry<String, FieldDescription> fieldEntry : parentFields.entrySet()) {
                FieldDescription field = fieldEntry.getValue();
                methodVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(field), i);
                if (long.class.equals(field.getType()) || double.class.equals(field.getType())) {
                    i += 2;
                }
                else {
                    i++;
                }
            }

            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(getParentClass()),
                    INIT, ByteCodeGeneratorHelper.getMethodSignatureForByteCode(parentFields, null));
        }

        // Set all fields that is not presented in parent
        //
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            String fieldName = field.getKey();
            if (getParentClass() == null || parentFields.get(fieldName) == null) {
                // there is no such field in parent class
                FieldDescription fieldType = field.getValue();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(ByteCodeGeneratorHelper.getConstantForVarInsn(fieldType), i);
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), fieldName,
                        ByteCodeGeneratorHelper.getJavaType(fieldType));
                if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
                    i += 2;
                }
                else {
                    i++;
                }
            }
        }
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(0,0);
    }

}
