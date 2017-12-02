package org.openl.binding.impl.module;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ParameterNode extends ABoundNode {

    protected String name;
    protected IOpenClass type;

    /**
     * @param syntaxNode
     * @param children
     */
    public ParameterNode(ISyntaxNode syntaxNode, String name, IOpenClass type) {
        super(syntaxNode, new IBoundNode[0]);
        this.name = name;
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return
     */
    public String getName() {
        return name;
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

}
