package org.openl.binding.impl;

import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class BlockNode extends ABoundNode implements IBoundMethodNode {

    private int localFrameSize = 0;

    public BlockNode(ISyntaxNode node, int localFrameSize, IBoundNode... children) {
        super(node, children);
        this.localFrameSize = localFrameSize;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object res = null;
        for (IBoundNode child : children) {
            res = child.evaluate(env);
        }
        return res;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundMethodNode#getLocalFrameSize()
     */
    @Override
    public int getLocalFrameSize() {
        return localFrameSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundMethodNode#getParametersSize()
     */
    @Override
    public int getParametersSize() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    @Override
    public IOpenClass getType() {
        return children == null || children.length == 0 ? NullOpenClass.the : children[children.length - 1].getType();
    }

}
