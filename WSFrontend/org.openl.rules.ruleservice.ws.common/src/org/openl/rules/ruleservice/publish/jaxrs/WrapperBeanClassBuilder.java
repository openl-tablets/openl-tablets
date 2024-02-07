package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.LinkedHashMap;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;

class WrapperBeanClassBuilder extends JavaBeanClassBuilder {

    private final String methodName;

    private final LinkedHashMap<String, FieldDescription> originalMethodTypeFields = new LinkedHashMap<>(0);

    public WrapperBeanClassBuilder(String beanName, String methodName) {
        super(beanName);
        this.methodName = Objects.requireNonNull(methodName);
        if (StringUtils.isEmpty(this.methodName)) {
            throw new IllegalArgumentException("Method name cannot be empty.");
        }
    }

    public JavaBeanClassBuilder setOriginalMethodTypeFields(
            LinkedHashMap<String, FieldDescription> originalMethodTypeFields) {
        if (originalMethodTypeFields != null) {
            this.originalMethodTypeFields.clear();
            this.originalMethodTypeFields.putAll(originalMethodTypeFields);
        }
        return this;
    }

    @Override
    public byte[] byteCode() {
        return new WrapperBeanClassGenerator(beanName,
                fields,
                parentType,
                parentFields,
                originalMethodTypeFields,
                methodName).byteCode();
    }
}
