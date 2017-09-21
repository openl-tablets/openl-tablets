package org.openl.binding.impl.ce;

import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

public class ArrayArgumentsMethodBinder extends org.openl.binding.impl.ArrayArgumentsMethodBinder {
    public ArrayArgumentsMethodBinder(String methodName, IOpenClass[] argumentsTypes, IBoundNode[] children) {
        super(methodName, argumentsTypes, children);
    }

    @Override
    protected IBoundNode makeMultiCallMethodBoundNode(ISyntaxNode node,
            IBoundNode[] children,
            List<Integer> arrayArgArgumentList,
            IMethodCaller singleParameterMethodCaller) {
        return new MultiCallMethodBoundNodeMT(node, children, singleParameterMethodCaller, arrayArgArgumentList);
    }
}
