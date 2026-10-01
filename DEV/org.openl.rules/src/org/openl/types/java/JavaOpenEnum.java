package org.openl.types.java;

import lombok.Getter;

import org.openl.domain.IDomain;

// A Java type is equal by its Java class; the state added here is derived from that class.
@SuppressWarnings("java:S2160")
public class JavaOpenEnum extends JavaOpenClass {

    protected JavaOpenEnum(Class<?> instanceClass) {
        super(instanceClass, true);
        domain = new JavaEnumDomain(this);
    }

    @Getter
    private final IDomain<?> domain;

}
