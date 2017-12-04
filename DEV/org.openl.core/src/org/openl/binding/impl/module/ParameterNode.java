package org.openl.binding.impl.module;

import org.openl.binding.BindingDependencies;
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

    ParameterNode(ISyntaxNode syntaxNode, String name, IOpenClass type) {
        super(syntaxNode);
        this.name = name;
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return name;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addTypeDependency(type, this);
    }

}
