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
        if (domain instanceof EnumDomain<?> enumDomain) {
            return new EnumDomainAdaptor(enumDomain);
        }
        if (domain instanceof IntRangeDomain rangeDomain) {
            return new IntRangeDomainAdaptor(rangeDomain);
        }
        if (domain instanceof DateRangeDomain rangeDomain) {
            return new DateRangeDomainAdaptor(rangeDomain);
        }
        if (domain instanceof JavaEnumDomain enumDomain) {
            return new JavaEnumDomainAdaptor(enumDomain);
        }
        return null;
    }
}
