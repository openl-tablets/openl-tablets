package org.openl.rules.datatype.gen;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.rules.datatype.gen.bean.writers.ClassDescriptionWriter;
import org.openl.rules.datatype.gen.bean.writers.ConstructorWithParametersWriter;
import org.openl.rules.datatype.gen.bean.writers.DefaultConstructorWriter;
import org.openl.rules.datatype.gen.bean.writers.EqualsWriter;
import org.openl.rules.datatype.gen.bean.writers.GettersWriter;
import org.openl.rules.datatype.gen.bean.writers.HashCodeWriter;
import org.openl.rules.datatype.gen.bean.writers.PrivateFieldsWriter;
import org.openl.rules.datatype.gen.bean.writers.SettersWriter;
import org.openl.rules.datatype.gen.bean.writers.ToStringWriter;

/**
 * Generates byte code for simple java bean.
 * 
 * @author DLiauchuk
 *
 */
public class SimpleBeanByteCodeGenerator extends BeanByteCodeGenerator {
    
    private Class<?> parentClass;
    private LinkedHashMap<String, FieldDescription> beanFields;
    private LinkedHashMap<String, FieldDescription> parentFields;
    private LinkedHashMap<String, FieldDescription> allFields;    
    
    /**
     * 
     * @param beanName name of the generated class, with namespace (e.g. <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     */
    public SimpleBeanByteCodeGenerator(String beanName, Map<String, FieldDescription> beanFields) {
        this(beanName, beanFields, null, new LinkedHashMap<String, FieldDescription>());
    }

    /**
     * 
     * @param beanName name of the generated class, with namespace (e.g.
     *            <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     * @param parentClass parent class
     */
    public SimpleBeanByteCodeGenerator(String beanName, Map<String, FieldDescription> beanFields, Class<?> parentClass, 
            Map<String, FieldDescription> parentFields) {
        super(beanName);        
        this.beanFields = new LinkedHashMap<String, FieldDescription>(beanFields);
        this.parentFields = new LinkedHashMap<String, FieldDescription>(parentFields);
        this.parentClass = parentClass;
        
        allFields = new LinkedHashMap<String, FieldDescription>();
        allFields.putAll(parentFields);
        allFields.putAll(beanFields); 
        
        initWriters();
    }

    private void initWriters() {
        addWriter(new ClassDescriptionWriter(getBeanNameWithPackage(), parentClass));
        addWriter(new PrivateFieldsWriter(beanFields));
        addWriter(new DefaultConstructorWriter(getBeanNameWithPackage(), parentClass, beanFields));
        addWriter(new ConstructorWithParametersWriter(getBeanNameWithPackage(), parentClass, beanFields, parentFields, allFields));
        addWriter(new GettersWriter(getBeanNameWithPackage(), beanFields));
        addWriter(new SettersWriter(getBeanNameWithPackage(), beanFields));
        addWriter(new ToStringWriter(getBeanNameWithPackage(), allFields));
        addWriter(new EqualsWriter(getBeanNameWithPackage(), allFields));
        addWriter(new HashCodeWriter(getBeanNameWithPackage(), allFields));
    }    
}
