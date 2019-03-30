package org.openl.rules.dt.type.domains;

import org.openl.domain.DateRangeDomain;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.types.java.JavaEnumDomain;

public final class DomainAdaptorFactory {
    
    private DomainAdaptorFactory() {
    }
    
    public static IDomainAdaptor getAdaptor(IDomain<?> domain) {
        if (domain instanceof EnumDomain<?>) {
            return new EnumDomainAdaptor((EnumDomain<?>) domain);
        }
        if (domain instanceof IntRangeDomain) {
            return new IntRangeDomainAdaptor((IntRangeDomain) domain);
        }
        if (domain instanceof DateRangeDomain) {
            return new DateRangeDomainAdaptor((DateRangeDomain) domain);
        }
        if (domain instanceof JavaEnumDomain) {
            return new JavaEnumDomainAdaptor((JavaEnumDomain) domain);
        }
        return null;
    }
}
