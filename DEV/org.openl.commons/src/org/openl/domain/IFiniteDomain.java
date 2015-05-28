/**
 * Created Apr 6, 2007
 */
package org.openl.domain;

/**
 * @author snshor
 * 
 * 
 */
@Deprecated
public interface IFiniteDomain<T> extends IDomain<T> {
    @Deprecated
    int UNKNOWN_SIZE = -1;

    @Deprecated
    int REALLY_BIG = Integer.MAX_VALUE;

    /**
     * @return the maximum size of the domain, guaranteed to be no less than
     *         actual size; in case when max size is not known or can not be
     *         presented as a positive integer, return REALLY_BIG
     * 
     */
    @Deprecated
    int maxSize();

    /**
     * @return the minimum size of the domain, guaranteed to be no more than
     *         actual size; in case when min size is not known return 0
     * 
     */
    @Deprecated
    int minSize();

    /**
     * 
     * @return the exact size of the domain, or UNKNOWN_SIZE
     */
    int size();

}
