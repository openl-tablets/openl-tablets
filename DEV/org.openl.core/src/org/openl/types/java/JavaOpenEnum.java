package org.openl.types.java;

import org.openl.domain.IDomain;

public class JavaOpenEnum extends JavaOpenClass {

    protected JavaOpenEnum(Class<?> instanceClass) {
        super(instanceClass, true);
        domain = new JavaEnumDomain(this);
    }

    private IDomain<?> domain;

    @Override
    public IDomain<?> getDomain() {
        return domain;
    }

}
