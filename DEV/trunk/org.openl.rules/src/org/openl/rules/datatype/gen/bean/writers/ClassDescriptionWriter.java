package org.openl.rules.datatype.gen.bean.writers;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Constants;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.util.generation.JavaClassGeneratorHelper;

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
        String sourceFileName = JavaClassGeneratorHelper.getClassFileName((beanNameWithPackage));
        if (parentClass == null) {
            classWriter.visit(Constants.V1_5, Constants.ACC_PUBLIC + Constants.ACC_SUPER, beanNameWithPackage,
                ByteCodeGeneratorHelper.JAVA_LANG_OBJECT, null, sourceFileName);
        } else {
            classWriter.visit(Constants.V1_5, Constants.ACC_PUBLIC + Constants.ACC_SUPER, beanNameWithPackage, Type
                    .getInternalName(parentClass), null, sourceFileName);
        }
    }
}
