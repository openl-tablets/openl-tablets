package org.openl.types.java;

import lombok.Getter;

import org.openl.domain.IDomain;

public class JavaOpenEnum extends JavaOpenClass {

    protected JavaOpenEnum(Class<?> instanceClass) {
        super(instanceClass, true);
        domain = new JavaEnumDomain(this);
    }

    @Getter
    private final IDomain<?> domain;

}
