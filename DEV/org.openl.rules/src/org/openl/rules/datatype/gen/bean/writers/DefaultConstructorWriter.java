package org.openl.rules.datatype.gen.bean.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
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
        // creates a MethodWriter for the (implicit) constructor
        MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        // pushes the 'this' variable
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        // invokes the super class constructor
        String parentName = getParentInternalName();
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentName, "<init>", "()V");

        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            FieldDescription fieldDescription = field.getValue();

            if (fieldDescription.hasDefaultValue()) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);

                TypeWriter typeWriter = ByteCodeGeneratorHelper.getTypeWriter(fieldDescription);
                typeWriter.writeFieldValue(mv, fieldDescription);

                String fieldTypeName = ByteCodeGeneratorHelper.getJavaType(fieldDescription);
                mv.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), field.getKey(), fieldTypeName);
            }
        }

        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(0, 0);
    }

}
