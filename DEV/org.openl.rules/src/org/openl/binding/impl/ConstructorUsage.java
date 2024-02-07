package org.openl.binding.impl;

import org.openl.meta.IMetaInfo;
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
        StringBuilder buf = new StringBuilder();
        if (isDatatype()) {
            var metaInfo = getMethod().getDeclaringClass().getMetaInfo();
            buf.append(metaInfo.getDisplayName(IMetaInfo.REGULAR)).append('\n');
        }
        return buf.append(constructorNode.getDescription()).toString();
    }

    @Override
    public NodeType getNodeType() {
        if (isDatatype()) {
            return NodeType.DATATYPE;
        } else {
            return NodeType.OTHER;
        }
    }

    private boolean isDatatype() {
        var method = getMethod();
        return method instanceof DatatypeOpenConstructor && method.getDeclaringClass() instanceof DatatypeOpenClass;
    }
}
