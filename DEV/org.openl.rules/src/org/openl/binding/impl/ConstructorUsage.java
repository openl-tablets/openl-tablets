package org.openl.binding.impl;

import org.openl.types.IOpenMethod;

/**
 * Constructor nodes require their own implementation of description
 *
 * @author Eugene Biruk
 */
public class ConstructorUsage extends MethodUsage {

    private final ConstructorNode constructorNode;

    public ConstructorUsage(ConstructorNode constructorNode, int startPos, int endPos, IOpenMethod method) {
        super(startPos, endPos, method);
        this.constructorNode = constructorNode;
    }

    @Override
    public String getDescription() {
        return constructorNode.getDescription();
    }
}
