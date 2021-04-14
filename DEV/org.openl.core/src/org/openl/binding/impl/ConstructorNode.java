package org.openl.binding.impl;

/**
 * used to identify the constructor nodes
 *
 * @author Eugene Biruk
 */
public interface ConstructorNode {

    MethodBoundNode getConstructor();

    String getDescription();

    boolean isShort();
}
