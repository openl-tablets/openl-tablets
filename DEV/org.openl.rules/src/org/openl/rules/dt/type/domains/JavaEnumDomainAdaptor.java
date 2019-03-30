package org.openl.rules.dt.type.domains;

import org.openl.types.java.JavaEnumDomain;

import org.openl.ie.constrainer.IntVar;

public class JavaEnumDomainAdaptor implements IDomainAdaptor {

    private JavaEnumDomain domain;

    public JavaEnumDomainAdaptor(JavaEnumDomain domain) {
        this.domain = domain;
    }

    public int getIndex(Object value) {
        if (value == null) {
            return -1;
        }
        return ((Enum<?>) value).ordinal();
    }

    public int getIntVarDomainType() {
        return IntVar.DOMAIN_BIT_FAST;
    }

    public int getMax() {
        return domain.size() - 1;
    }

    public int getMin() {
        return 0;
    }

    public Object getValue(int index) {
        return domain.getValue(index);
    }

    public IDomainAdaptor merge(IDomainAdaptor adaptor) {
        JavaEnumDomainAdaptor a = (JavaEnumDomainAdaptor) adaptor;

        if (domain.getEnumClass() != a.domain.getEnumClass()) {
            throw new RuntimeException("Wrong use of JavaEnumDomain for " + domain.getEnumClass().getName());
        }

        return this;
    }

}
