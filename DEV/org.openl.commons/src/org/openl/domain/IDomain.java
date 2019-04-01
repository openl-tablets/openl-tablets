/**
 * Created Apr 6, 2007
 */
package org.openl.domain;

/**
 * @author snshor
 */

public interface IDomain<T> extends Iterable<T> {

    /**
     *
     * @return type that can be used with this domain
     */
    IType getElementType();

    /**
     *
     * @param obj
     * @return true if object belongs to this domain
     */
    boolean selectObject(T obj);

}
