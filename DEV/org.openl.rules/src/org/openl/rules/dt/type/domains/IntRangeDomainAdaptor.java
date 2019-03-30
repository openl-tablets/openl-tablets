package org.openl.rules.dt.type.domains;

import org.openl.domain.IntRangeDomain;
import org.openl.ie.constrainer.IntVar;

public class IntRangeDomainAdaptor implements IDomainAdaptor {

    private IntRangeDomain irange;

    public IntRangeDomainAdaptor(IntRangeDomain irange) {
        this.irange = irange;
    }

    public int getIndex(Object value) {
        return (Integer) value;// - irange.getMin();
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

    public IDomainAdaptor merge(IDomainAdaptor adaptor) {
        IntRangeDomainAdaptor a = (IntRangeDomainAdaptor)adaptor;
        
        int min1 = irange.getMin();
        int max1 = irange.getMax();
        int min2 = a.irange.getMin();
        int max2 = a.irange.getMax();
        
        int min = min1 < min2 ? min1 : min2;
        int max = max1 > max2 ? max1 : max2;
        
        return new IntRangeDomainAdaptor(new IntRangeDomain(min, max));
    }

}
