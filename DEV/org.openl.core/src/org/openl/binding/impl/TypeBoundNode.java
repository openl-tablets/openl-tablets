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
public class TypeBoundNode extends ABoundNode {
    private IOpenClass type;

    /**
     * @param syntaxNode
     * @param children
     */
    public TypeBoundNode(ISyntaxNode syntaxNode, IOpenClass type) {
        super(syntaxNode, new IBoundNode[0]);
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        // TODO probably create another class for static method access
        // throw new UnsupportedOperationException("TypeNode can not be
        // evaluated");
        return type;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return type;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addTypeDependency(type, this);
    }

    public boolean isStaticTarget() {
        return true;
    }

    
}
