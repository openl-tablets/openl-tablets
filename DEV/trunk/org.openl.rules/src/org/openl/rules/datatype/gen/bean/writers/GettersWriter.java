package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.util.StringTool;

/**
 * Writes getters to the generated bean class.
 * 
 * @author DLiauchuk 
 */
public class GettersWriter extends MethodWriter {
    
    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param beanFields fields of generating class.
     */
    public GettersWriter(String beanNameWithPackage, Map<String, FieldDescription> beanFields) {
        super(beanNameWithPackage, beanFields);
    }
    
    public void write(ClassWriter classWriter) {
        /** ignore those fields that are of void type. In java it is impossible
        but possible in Openl, e.g. spreadsheet cell with void type.*/
        for(Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            if (!field.getValue().getCanonicalTypeName().equals(MethodWriter.VOID_CLASS_NAME)) {
                generateGetter(classWriter, field);
            }
        }
    }
    
    /**
     * Generates getter for the field.
     * 
     * @param beanNameWithPackage
     * @param classWriter
     * @param field
     */
    protected void generateGetter(ClassWriter classWriter, Map.Entry<String, FieldDescription> field) {
        MethodVisitor methodVisitor;
        String fieldName = field.getKey();
        FieldDescription fieldType = field.getValue();
        String getterName = StringTool.getGetterName(fieldName);
        
        methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC,  getterName, String.format("()%s",
            ByteCodeGeneratorHelper.getJavaType(fieldType)), null, null);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, getBeanNameWithPackage(), fieldName, ByteCodeGeneratorHelper.getJavaType(fieldType));
        methodVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(fieldType));
        
        // long and double types are the biggest ones, so they use a maximum of two stack  
        // elements and one local variable for getter method.
        if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
            methodVisitor.visitMaxs(2, 1);
        } else {
            methodVisitor.visitMaxs(1, 1);
        }
    }

}
