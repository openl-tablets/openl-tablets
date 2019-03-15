package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class TypeBoundNode extends ABoundNode {
    private IOpenClass type;

    TypeBoundNode(ISyntaxNode syntaxNode, IOpenClass type) {
        super(syntaxNode);
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        // TODO probably create another class for static method access
        // throw new UnsupportedOperationException("TypeNode can not be
        // evaluated");
        return type;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean isStaticTarget() {
        return true;
    }
}
