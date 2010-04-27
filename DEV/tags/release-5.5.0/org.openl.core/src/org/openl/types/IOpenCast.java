/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

/**
 * @author snshor IOpenCast is responsible for casting operations.
 */
public interface IOpenCast {
    public Object convert(Object from);

    /**
     *
     * @return
     */
    // public IOpenClass getFrom();
    // public IOpenClass getTo();
    public int getDistance(IOpenClass from, IOpenClass to);

    /**
     *
     * @return true if cast can be performed automatically, i.e. without
     *         explicit casting.
     */
    public boolean isImplicit();

}
