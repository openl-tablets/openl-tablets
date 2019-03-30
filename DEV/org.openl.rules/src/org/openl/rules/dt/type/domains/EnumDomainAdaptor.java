/**
 * Created Feb 12, 2007
 */
package org.openl.rules.dt.type.domains;

import java.util.HashSet;

import org.openl.domain.EnumDomain;

import org.openl.ie.constrainer.IntVar;

/**
 * @author snshor
 * 
 */
public class EnumDomainAdaptor implements IDomainAdaptor {

    private Object[] values;

    public EnumDomainAdaptor(EnumDomain<?> domain) {
        values = domain.getEnum().getAllObjects();
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

    public IDomainAdaptor merge(IDomainAdaptor adaptor) {
        EnumDomainAdaptor a = (EnumDomainAdaptor)adaptor;
        
        Object[] v1 = getValues();
        Object[] v2 = a.getValues();
        
        HashSet<Object> set = new HashSet<>(v1.length + v2.length);
        
        for (int i = 0; i < v1.length; i++) {
            set.add(v1[i]);
        }
        
        for (int i = 0; i < v2.length; i++) {
            set.add(v2[i]);
        }
        
        Object[] newValues = set.toArray();
        
        return new EnumDomainAdaptor(new EnumDomain<Object>(newValues));
        
    }

}
