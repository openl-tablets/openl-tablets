package org.openl.ie.constrainer;

import org.jspecify.annotations.Nullable;

/**
 * A step of the search.
 */
@FunctionalInterface
interface Goal {

    /**
     * Executes the step.
     *
     * @return the goal to execute next, or {@code null} when the step is complete
     * @throws Failure if the step violates a constraint
     */
    @Nullable Goal execute() throws Failure;
}
