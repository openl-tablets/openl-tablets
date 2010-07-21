/**
 * Created Aug 28, 2007
 */
package org.openl.rules.dt.type;

/**
 * @author snshor
 * 
 */
public interface IRangeAdaptor<T, C> {

    /**
     * Gets max value of range.
     * 
     * @param param
     * @return the max bound of the expression min <= X && X < max
     */
    public Comparable<C> getMax(T param);

    /**
     * Gets min value of range.
     * 
     * @param param
     * @return the min bound of the expression min <= X && X < max
     */
    public Comparable<C> getMin(T param);

}
