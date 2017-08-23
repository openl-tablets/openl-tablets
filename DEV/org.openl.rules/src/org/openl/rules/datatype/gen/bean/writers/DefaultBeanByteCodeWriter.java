package org.openl.rules.datatype.gen.bean.writers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

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
        this.beanFields = new LinkedHashMap<String, FieldDescription>(beanFields);
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

    /**
     * Returns an internal name of the parent class. If no parent classes are (parentClass is null)
     * then "java/lang/Object" will be returned.
     *
     * @return an internal name of the parent class.
     * @see Type#getInternalName(Class)
     */
    protected String getParentInternalName() {
        String internalName;
        if (parentClass == null) {
            internalName = "java/lang/Object";
        } else {
            internalName = Type.getInternalName(parentClass);
        }
        return internalName;
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
