package org.openl.rules.validator.dt;

import org.openl.domain.IntRangeDomain;

import com.exigen.ie.constrainer.IntVar;

public class IntRangeDomainAdaptor implements IDomainAdaptor {

    IntRangeDomain irange;

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
