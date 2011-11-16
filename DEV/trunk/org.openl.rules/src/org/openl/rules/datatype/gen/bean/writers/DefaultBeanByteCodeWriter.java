package org.openl.rules.datatype.gen.bean.writers;

import java.util.LinkedHashMap;
import java.util.Map;

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

    protected String getBeanNameWithPackage() {
        return beanNameWithPackage;
    }

    protected Class<?> getParentClass() {
        return parentClass;
    }

    protected Map<String, FieldDescription> getBeanFields() {
        return beanFields;
    }
}
