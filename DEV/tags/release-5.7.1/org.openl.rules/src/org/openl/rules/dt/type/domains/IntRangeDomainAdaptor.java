package org.openl.rules.dt.type.domains;

import org.openl.domain.IntRangeDomain;

import org.openl.ie.constrainer.IntVar;

public class IntRangeDomainAdaptor implements IDomainAdaptor {

    private IntRangeDomain irange;

    public IntRangeDomainAdaptor(IntRangeDomain irange) {
        this.irange = irange;
    }

    public int getIndex(Object value) {
        return ((Integer) value);// - irange.getMin();
    }

    public int getIntVarDomainType() {
        return IntVar.DOMAIN_PLAIN;
    }

    public int getMax() {
        return irange.getMax();
    }

    public int getMin() {
        return irange.getMin();
    }

    public Object getValue(int index) {
        return index; // irange.getMin();// + index;
    }

}
