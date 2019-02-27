package org.openl.rules.dt.type;

/**
 * @author snshor
 * 
 */
public interface IRangeAdaptor<T, C extends Comparable<?>> {

    /**
     * Gets max value of range.
     * 
     * @param param
     * @return the max bound of the expression min <= X && X <= max. Returns null if needs maximum max posible value.
     */
    C getMax(T param);

    /**
     * Gets min value of range.
     * 
     * @param param
     * @return the min bound of the expression min <= X && X <= max. Returns null if needs maximum min posible value.
     */
    C getMin(T param);
    
    /**
     * Adapts value type to the type of specific <code>IRangeAdaptor</code> implementation.
     * To have the possibility to compare these values.
     * 
     * @param value that is going to be compared with values returned by {@link #getMax(Object)} and {@link #getMin(Object)}
     * values.
     * @return value, casted to the type of specific <code>IRangeAdaptor</code> implementation.
     */
    C adaptValueType(Object value);

	boolean useOriginalSource();

}
