package org.openl.rules.datatype.gen;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.gen.FieldDescription;
import org.openl.gen.POJOByteCodeGenerator;

/**
 * Generates Java byte code to create JavaBean classes with JAXB annotations.
 */
public class JavaBeanClassBuilder {

    protected final String beanName;
    protected Class<?> parentClass = Object.class;
    protected LinkedHashMap<String, FieldDescription> parentFields = new LinkedHashMap<>(0);
    protected LinkedHashMap<String, FieldDescription> fields = new LinkedHashMap<>(0);

    protected boolean additionalConstructor = true;
    protected boolean publicFields = false;
    protected boolean equalsHashCodeToStringMethods = true;

    public JavaBeanClassBuilder(String beanName) {
        this.beanName = beanName.replace('.', '/');
    }

    public JavaBeanClassBuilder setParentClass(Class<?> parentClass) {
        this.parentClass = parentClass;
        return this;
    }

    public JavaBeanClassBuilder addParentField(String name, String type) {
        Object put = parentFields.put(name, new FieldDescription(type));
        if (put != null) {
            throw new IllegalArgumentException(String.format("The same parent field '%s has been put.", name));
        }
        return this;
    }

    private JavaBeanClassBuilder addField(String name, FieldDescription type) {
        Object put = fields.put(name, type);
        if (put != null) {
            throw new IllegalArgumentException(String.format("The same parent field '%s has been put.", name));
        }
        return this;
    }

    public JavaBeanClassBuilder addField(String name, String type) {
        addField(name, new FieldDescription(type));
        return this;
    }

    public JavaBeanClassBuilder addFields(Map<String, FieldDescription> fields) {
        for (Map.Entry<String, FieldDescription> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
        return this;
    }

    public JavaBeanClassBuilder withAdditionalConstructor(boolean additionalConstructor) {
        this.additionalConstructor = additionalConstructor;
        return this;
    }

    public JavaBeanClassBuilder withPublicFields(boolean publicFields) {
        this.publicFields = publicFields;
        return this;
    }

    public JavaBeanClassBuilder withEqualsHashCodeToStringMethods(boolean equalsHashCodeToStringMethods) {
        this.equalsHashCodeToStringMethods = equalsHashCodeToStringMethods;
        return this;
    }

    /**
     * Creates JavaBean byte code for given fields.
     */
    public byte[] byteCode() {
        return new POJOByteCodeGenerator(beanName,
            fields,
            parentClass,
            parentFields,
            additionalConstructor,
            equalsHashCodeToStringMethods,
            publicFields).byteCode();
    }

}
