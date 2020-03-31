package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;

class WrapperBeanClassBuilder extends JavaBeanClassBuilder {

    private String methodName;

    public WrapperBeanClassBuilder(String beanName, String methodName) {
        super(beanName);
        this.methodName = Objects.requireNonNull(methodName);
        if (StringUtils.isEmpty(this.methodName)) {
            throw new IllegalArgumentException("Method name cannot be empty.");
        }
    }

    @Override
    public byte[] byteCode() {
        return new WrapperBeanClassGenerator(beanName, fields, parentClass, parentFields, methodName).byteCode();
    }
}
