package org.openl.runtime;

/**
 * This marker interface represent the runtime context abstraction what can be accessed by user.
 * <p>
 * Runtime context used by OpenL tablets engine for rules overload support.
 *
 * @author Alexey Gamanovich
 */
public interface IRuntimeContext extends Cloneable {
    // Public API: clients clone runtime contexts; removing clone() would break them.
    @SuppressWarnings("java:S2975")
    IRuntimeContext clone() throws CloneNotSupportedException;
}
