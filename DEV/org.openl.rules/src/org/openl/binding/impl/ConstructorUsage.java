package org.openl.binding.impl;

import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenConstructor;
import org.openl.types.IOpenMethod;

/**
 * Constructor nodes require their own implementation of description
 *
 * @author Eugene Biruk
 */
public class ConstructorUsage extends MethodUsage {

    private final ConstructorNode constructorNode;

    /**
     * @param endPos the ending index position, exclusive
     */
    public ConstructorUsage(ConstructorNode constructorNode, int startPos, int endPos, IOpenMethod method) {
        super(startPos, endPos, method);
        this.constructorNode = constructorNode;
    }

    public ConstructorNode getConstructorNode() {
        return constructorNode;
    }

    @Override
    public String getDescription() {
        return constructorNode.getDescription();
    }

    @Override
    public NodeType getNodeType() {
        var method = getMethod();
        if (method instanceof DatatypeOpenConstructor && method
                .getDeclaringClass() instanceof DatatypeOpenClass) {
            return NodeType.DATATYPE;
        } else {
            return NodeType.OTHER;
        }
    }
}
