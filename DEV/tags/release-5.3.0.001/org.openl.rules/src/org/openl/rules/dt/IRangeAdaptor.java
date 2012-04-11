/**
 * Created Aug 28, 2007
 */
package org.openl.rules.dt;

import org.openl.rules.helpers.IntRange;

/**
 * @author snshor
 *
 */
public interface IRangeAdaptor<T, C> {

    /**
     * @author snshor
     *
     */
    public class IntRangeAdaptor implements IRangeAdaptor<IntRange, Integer> {

        public Integer getMax(IntRange range) {
            int max = range.getMax();
            if (max != Integer.MAX_VALUE) {
                max = max + 1;
            }

            return max;
        }

        public Integer getMin(IntRange range) {
            return range.getMin();
        }

    }

    /**
     *
     * @param param
     * @return the max bound of the expression min <= X && X < max
     */
    public Comparable<C> getMax(T param);

    /**
     *
     * @param param
     * @return the min bound of the expression min <= X && X < max
     */
    public Comparable<C> getMin(T param);

}
