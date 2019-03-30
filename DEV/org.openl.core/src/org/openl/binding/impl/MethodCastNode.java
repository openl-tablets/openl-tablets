package org.openl.binding.impl;

import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;

public class MethodCastNode extends CastNode implements IBoundMethodNode {

    public MethodCastNode(IBoundNode bnode, IOpenCast cast, IOpenClass castedType) {
        super(null, bnode, cast, castedType);
    }

    @Override
    public int getLocalFrameSize() {
        return ((IBoundMethodNode) children[0]).getLocalFrameSize();
    }

    @Override
    public int getParametersSize() {
        return ((IBoundMethodNode) children[0]).getParametersSize();
    }
}
