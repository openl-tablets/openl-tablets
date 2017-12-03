package org.openl.binding.impl;

import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class LiteralBoundNode extends ABoundNode {
    protected Object value;
    protected IOpenClass type;

    public LiteralBoundNode(ISyntaxNode syntaxNode, Object value, IOpenClass type) {
        super(syntaxNode);
        this.value = value;
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        return value;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
