package org.openl.rules.datatype.gen;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.gen.FieldDescription;
import org.openl.gen.POJOByteCodeGenerator;

/**
 * Generates Java byte code to create JavaBean classes with JAXB annotations.
 */
public class JavaBeanClassBuilder {

    private final String beanName;
    private Class<?> parentClass = Object.class;
    private LinkedHashMap<String, FieldDescription> parentFields = new LinkedHashMap<>(0);
    private LinkedHashMap<String, FieldDescription> fields = new LinkedHashMap<>(0);

    public JavaBeanClassBuilder(String beanName) {
        this.beanName = beanName.replace('.', '/');
    }

    public void setParentClass(Class<?> parentClass) {
        this.parentClass = parentClass;
    }

    public void addParentField(String name, String type) {
        Object put = parentFields.put(name, new FieldDescription(type));
        if (put != null) {
            throw new IllegalArgumentException("The same parent field '" + name + " has been put.");
        }
    }

    private void addField(String name, FieldDescription type) {
        Object put = fields.put(name, type);
        if (put != null) {
            throw new IllegalArgumentException("The same parent field '" + name + " has been put.");
        }
    }

    public void addField(String name, String type) {
        addField(name, new FieldDescription(type));
    }

    public void addFields(Map<String, FieldDescription> fields) {
        for (Map.Entry<String, FieldDescription> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    protected String getBeanName() {
        return beanName;
    }

    protected LinkedHashMap<String, FieldDescription> getFields() {
        return fields;
    }

    protected Class<?> getParentClass() {
        return parentClass;
    }

    protected LinkedHashMap<String, FieldDescription> getParentFields() {
        return parentFields;
    }

    /**
     * Creates JavaBean byte code for given fields.
     */
    public byte[] byteCode() {
        return new POJOByteCodeGenerator(beanName, fields, parentClass, parentFields, true).byteCode();
    }
}
