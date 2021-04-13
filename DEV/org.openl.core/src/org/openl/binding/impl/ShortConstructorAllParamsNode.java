package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.vm.IRuntimeEnv;

public class ShortConstructorAllParamsNode extends ABoundNode implements ShortConstructor {

    private final MethodBoundNode constructor;

    public ShortConstructorAllParamsNode(MethodBoundNode constructor, ISyntaxNode node, IBoundNode... children) {
        super(node, children);
        this.constructor = constructor;
    }

    @Override
    public MethodBoundNode getConstructor() {
        return constructor;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) throws Exception {
        return constructor.evaluate(env);
    }
}