/**
 * Created Apr 6, 2007
 */
package org.openl.domain;

/**
 * @author snshor
 */

public interface IDomain<T> {

    /**
     *
     * @return type that can be used with this domain
     */
    IType getElementType();

    /**
     *
     * @return true in case of ranges and enumerations
     */

    boolean isFinite();

    /**
     *
     * @param obj
     * @return true if object belongs to this domain
     */
    boolean selectObject(T obj);

    /**
     *
     * @param type
     * @return true if any object of the type belong to this domain; NOTE: this
     *         is strictly a type check, the method does not take into
     *         consideration the domain of the type
     */
    boolean selectType(IType type);

}
