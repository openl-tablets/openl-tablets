package org.openl.gen.writers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.gen.FieldDescription;
import org.openl.gen.TypeDescription;

public abstract class DefaultBeanByteCodeWriter implements BeanByteCodeWriter {

    private String beanNameWithPackage;

    private TypeDescription parentType;

    private Map<String, FieldDescription> beanFields;

    /**
     *
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param parentType class descriptor for super class.
     * @param beanFields fields of generating class.
     */
    public DefaultBeanByteCodeWriter(String beanNameWithPackage,
            TypeDescription parentType,
            Map<String, FieldDescription> beanFields) {
        this.beanNameWithPackage = beanNameWithPackage;
        this.parentType = parentType;
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

    protected TypeDescription getParentType() {
        return parentType;
    }

    protected Map<String, FieldDescription> getBeanFields() {
        return beanFields;
    }

    @Override
    public String toString() {
        // For debugging purpose
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(this.getClass().getSimpleName());
        strBuilder.append(" for ");
        strBuilder.append(beanNameWithPackage);
        return strBuilder.toString();
    }
}
