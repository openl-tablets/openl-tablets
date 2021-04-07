package org.openl.binding.impl;

import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class NamedConstructorNode extends ABoundNode implements IBoundMethodNode {

    private final int localFrameSize;
    private final ILocalVar tempVar;
    private final IBoundNode constructor;

    public NamedConstructorNode(ILocalVar tempVar, ISyntaxNode node, int localFrameSize, IBoundNode constructor, IBoundNode... children) {
        super(node, children);
        this.localFrameSize = localFrameSize;
        this.tempVar = tempVar;
        this.constructor = constructor;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object evaluate = constructor.evaluate(env);
        tempVar.set(null, evaluate, env);
        for (IBoundNode child : children) {
            child.evaluate(env);
        }
        return tempVar.get(null, env);
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
        return tempVar.getType();
    }

}