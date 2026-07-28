package org.openl.binding.impl;

import lombok.Getter;

import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class LiteralBoundNode extends ABoundNode {
    @Getter
    protected final Object value;
    @Getter
    protected final IOpenClass type;

    public LiteralBoundNode(ISyntaxNode syntaxNode, Object value, IOpenClass type) {
        super(syntaxNode);
        this.value = value;
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        return value;
    }
}
