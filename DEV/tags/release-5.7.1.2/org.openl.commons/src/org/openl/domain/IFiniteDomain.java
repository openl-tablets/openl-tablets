/**
 * Created Apr 6, 2007
 */
package org.openl.domain;

import java.util.Iterator;

/**
 * @author snshor
 *
 *
 */
public interface IFiniteDomain<T> extends IDomain<T> {
    int UNKNOWN_SIZE = -1;

    int REALLY_BIG = Integer.MAX_VALUE;

    /**
     * @return iterator over domain
     */
    Iterator<T> iterator();

    /**
     * @return the maximum size of the domain, guaranteed to be no less than
     *         actual size; in case when max size is not known or can not be
     *         presented as a positive integer, return REALLY_BIG
     *
     */
    int maxSize();

    /**
     * @return the minimum size of the domain, guaranteed to be no more than
     *         actual size; in case when min size is not known return 0
     *
     */
    int minSize();

    /**
     *
     * @return the exact size of the domain, or UNKNOWN_SIZE
     */
    int size();

}
