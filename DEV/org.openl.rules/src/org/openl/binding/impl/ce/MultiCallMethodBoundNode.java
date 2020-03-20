package org.openl.binding.impl.ce;

import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public class MultiCallMethodBoundNode extends org.openl.binding.impl.MultiCallMethodBoundNode {

    public MultiCallMethodBoundNode(ISyntaxNode syntaxNode,
            IBoundNode[] children,
            IMethodCaller singleParameterMethod,
            List<Integer> arrayArgArgumentList) {
        super(syntaxNode, children, singleParameterMethod, arrayArgArgumentList);
    }

    @Override
    protected IMethodCaller getMethodCaller(IRuntimeEnv env) {
        return getMethodCaller();
    }

}
