package org.openl.rules.datatype.gen.bean.writers;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;

/**
 * Writer that supports writing class declaration in byte code.
 * 
 * @author DLiauchuk
 *
 */
public class ClassDescriptionWriter implements BeanByteCodeWriter {
    
    private String beanNameWithPackage;
    
    private Class<?> parentClass;
    
    /**
     * 
     * @param beanNameWithPackage name of the class with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param parentClass class descriptor for super class.
     */
    public ClassDescriptionWriter(String beanNameWithPackage, Class<?> parentClass) {
        this.beanNameWithPackage = beanNameWithPackage;
        this.parentClass = parentClass;
    }
    
    public void write(ClassWriter classWriter) {
        if (parentClass == null) {
            classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, beanNameWithPackage,
                    null, ByteCodeGeneratorHelper.JAVA_LANG_OBJECT, null);
        } else {
            classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, beanNameWithPackage,
                    null, Type.getInternalName(parentClass), null);
        }
    }
}
