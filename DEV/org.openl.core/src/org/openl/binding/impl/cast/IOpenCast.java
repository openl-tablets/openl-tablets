/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.cast;

/**
 * IOpenCast is responsible for casting operations.
 *
 * @author snshor
 */
public interface IOpenCast {

    /**
     * Performs a value conversion.
     *
     * @param from value to convert
     * @return converted value
     */
    Object convert(Object from);

    /**
     * Gets cast operation priority.
     *
     * @return priority of cast
     */
    int getDistance();

    /**
     * Checks that cast can be performed automatically.
     *
     * @return true if cast can be performed automatically, i.e. without explicit casting
     */
    boolean isImplicit();
}
