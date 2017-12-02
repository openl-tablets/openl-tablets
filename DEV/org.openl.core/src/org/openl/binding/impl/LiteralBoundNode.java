package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
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

    /**
     * @param syntaxNode
     * @param children
     */
    public LiteralBoundNode(ISyntaxNode syntaxNode, Object value, IOpenClass type) {
        super(syntaxNode, new IBoundNode[0]);
        this.value = value;
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        return value;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return type;
    }

    /**
     * @return
     */
    public Object getValue() {
        return value;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {

    }

}
