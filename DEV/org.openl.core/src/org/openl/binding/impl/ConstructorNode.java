package org.openl.binding.impl;

/**
 * used to identify the constructor nodes
 *
 * @author Eugene Biruk
 */
public interface ConstructorNode {

    /**
     * Return constructor.
     *
     * @return {@link MethodBoundNode} constructor.
     */
    MethodBoundNode getConstructor();


    /**
     * Return constructor description.
     *
     * @return description.
     */
    String getDescription();
}
