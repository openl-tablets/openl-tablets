package org.openl.rules.datatype.gen.bean.writers;

import java.io.Serializable;
import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * Writer that supports writing class declaration in byte code.
 * 
 * @author DLiauchuk
 *
 */
public class ClassDescriptionWriter extends DefaultBeanByteCodeWriter {

    /**
     * 
     * @param beanNameWithPackage name of the class with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * @param parentClass class descriptor for super class.
     */
    public ClassDescriptionWriter(String beanNameWithPackage, Class<?> parentClass) {
        super(beanNameWithPackage, parentClass, new HashMap<String, FieldDescription>());        
    }
    
    public void write(ClassWriter classWriter) {
        String parentName = Type.getInternalName(getParentClass());
        classWriter.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, getBeanNameWithPackage(), null, parentName, new String[]{Serializable.class.getName().replace(".", "/")});
    }
}
