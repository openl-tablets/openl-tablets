package org.openl.runtime;

/**
 * This marker interface represent the runtime context abstraction what can be accessed by user.
 * 
 * Runtime context used by OpenL tablets engine for rules overload support.
 * 
 * @author Alexey Gamanovich
 */
public interface IRuntimeContext extends Cloneable {
    IRuntimeContext clone() throws CloneNotSupportedException;
}
