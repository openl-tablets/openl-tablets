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

    Object convert(Object from);

    int getDistance(IOpenClass from, IOpenClass to);

    /**
     * Checks that cast can be performed automatically.
     * 
     * @return true if cast can be performed automatically, i.e. without
     *         explicit casting
     */
    boolean isImplicit();
}
