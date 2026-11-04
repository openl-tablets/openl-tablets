/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.cast;

import org.jspecify.annotations.Nullable;

import org.openl.vm.IRuntimeEnv;

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
     * Converts a value during an execution, which may remember what it checked before.
     *
     * <p>The result is the one {@link #convert(Object)} gives; a cast that checks values may skip a check the
     * execution made already.
     *
     * @param from value to convert
     * @param env the environment of the execution; {@code null} out of an execution
     * @return converted value
     */
    default Object convert(Object from, @Nullable IRuntimeEnv env) {
        return convert(from);
    }

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
