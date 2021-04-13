package org.openl.binding.impl;

import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ShortConstructorNamedParamsNode extends ABoundNode implements IBoundMethodNode, ShortConstructor {

    private final int localFrameSize;
    private final ILocalVar tempVar;
    private final MethodBoundNode constructor;


    public ShortConstructorNamedParamsNode(ILocalVar tempVar, ISyntaxNode node, int localFrameSize, MethodBoundNode constructor, IBoundNode... children) {
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
        return evaluate;
    }

    @Override
    public int getLocalFrameSize() {
        return localFrameSize;
    }

    @Override
    public int getParametersSize() {
        return 0;
    }

    @Override
    public IOpenClass getType() {
        return tempVar.getType();
    }

    @Override
    public MethodBoundNode getConstructor() {
        return constructor;
    }
}