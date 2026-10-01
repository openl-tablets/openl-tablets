package org.openl.ie.constrainer;

/**
 * Reacts to a change of the domain of an expression.
 */
@FunctionalInterface
interface Observer {

    /**
     * Reacts to the change.
     *
     * @throws Failure if the change violates a constraint
     */
    void update(IntEvent event) throws Failure;
}
