/**
 * Created Feb 12, 2007
 */
package org.openl.rules.dt.type;

/**
 * @author snshor
 *
 */
public interface IDomainAdaptor {

    int getIndex(Object value);

    int getIntVarDomainType();

    int getMax();

    int getMin();

    Object getValue(int index);

}
