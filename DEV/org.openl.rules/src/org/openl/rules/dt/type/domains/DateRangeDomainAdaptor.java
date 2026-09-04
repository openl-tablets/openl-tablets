package org.openl.rules.dt.type.domains;

import java.util.Date;

import lombok.RequiredArgsConstructor;

import org.openl.domain.DateRangeDomain;
import org.openl.ie.constrainer.IntVar;

/**
 * Adaptor for date ranges. Helps to access dates in range by index and retrieve index of date within the range.
 *
 * @author PUdalau
 */
@RequiredArgsConstructor
public class DateRangeDomainAdaptor implements IDomainAdaptor {
    private final DateRangeDomain domain;

    @Override
    public int getIndex(Object value) {
        return domain.getIndex((Date) value);
    }

    @Override
    public int getIntVarDomainType() {
        return IntVar.DOMAIN_PLAIN;
    }

    @Override
    public int getMax() {
        return domain.getIndex(domain.getMax());
    }

    @Override
    public int getMin() {
        return 0;
    }

    @Override
    public Object getValue(int index) {
        return domain.getValue(index);
    }

    @Override
    public String toString() {
        return "[" + getMin() + ";" + getMax() + "]";
    }

    @Override
    public IDomainAdaptor merge(IDomainAdaptor adaptor) {
        var a = (DateRangeDomainAdaptor) adaptor;

        Date min = domain.getMin().before(a.domain.getMin()) ? domain.getMin() : a.domain.getMin();
        Date max = domain.getMax().after(a.domain.getMax()) ? domain.getMax() : a.domain.getMax();

        return new DateRangeDomainAdaptor(new DateRangeDomain(min, max));
    }
}
