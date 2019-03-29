package org.openl.gen.writers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.gen.FieldDescription;

public abstract class DefaultBeanByteCodeWriter implements BeanByteCodeWriter {
    
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
    public DefaultBeanByteCodeWriter(String beanNameWithPackage, Class<?> parentClass, Map<String, FieldDescription> beanFields) {
        this.beanNameWithPackage = beanNameWithPackage;
        this.parentClass = parentClass;
        this.beanFields = new LinkedHashMap<>(beanFields);
    }

    protected int getConstantForVarInsn(FieldDescription field) {
        String retClass = field.getTypeDescriptor();
        Type type = Type.getType(retClass);
        return type.getOpcode(Opcodes.ILOAD);
    }

    protected String getBeanNameWithPackage() {
        return beanNameWithPackage;
    }

    protected Class<?> getParentClass() {
        return parentClass;
    }

    protected Map<String, FieldDescription> getBeanFields() {
        return beanFields;
    }

    @Override
    public String toString() {
        // For debugging purpose
        StringBuilder strBuilder = new StringBuilder(128);
        strBuilder.append(this.getClass().getSimpleName());
        strBuilder.append(" for ");
        strBuilder.append(beanNameWithPackage);
        return strBuilder.toString();
    }
}
