package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ReturnNode extends ABoundNode {

    /**
     * @param syntaxNode
     * @param children
     */
    public ReturnNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        IBoundNode exprNode = children.length == 0 ? null : children[0];
        Object returnValue = exprNode == null ? null : exprNode.evaluate(env);

        throw new ControlSignalReturn(returnValue);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    @Override
    public IOpenClass getType() {
        return children.length == 0 ? NullOpenClass.the : children[0].getType();
    }

}
