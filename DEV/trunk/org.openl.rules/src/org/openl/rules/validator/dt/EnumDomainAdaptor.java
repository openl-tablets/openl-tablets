/**
 * Created Feb 12, 2007
 */
package org.openl.rules.validator.dt;

import org.openl.domain.EnumDomain;

import com.exigen.ie.constrainer.IntVar;

/**
 * @author snshor
 *
 */
public class EnumDomainAdaptor implements IDomainAdaptor {
    
    private Object[] values;

    public EnumDomainAdaptor(EnumDomain<?> d) {
        values = d.getEnum().getAllObjects();
    }

    public EnumDomainAdaptor(Object[] values) {
        this.values = values;
    }

    public int getIndex(Object value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public int getIntVarDomainType() {
        return IntVar.DOMAIN_BIT_FAST;
    }

    public int getMax() {
        return values.length - 1;
    }

    public int getMin() {
        return 0;
    }

    public Object getValue(int index) {
        return values[index];
    }

    public Object[] getValues() {
        return values;
    }

    public void setValues(Object[] values) {
        this.values = values;
    }

    public int size() {
        return values.length;
    }

}
