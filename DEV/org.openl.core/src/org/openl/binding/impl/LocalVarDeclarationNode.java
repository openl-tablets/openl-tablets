package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public final class LocalVarDeclarationNode extends ABoundNode {

    ILocalVar var;
    IBoundNode initNode;

    public LocalVarDeclarationNode(ISyntaxNode syntaxNode, IBoundNode initNode, ILocalVar var) {
        super(syntaxNode, initNode);

        this.initNode = initNode;
        this.var = var;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object initObj = initNode == null ? null : initNode.evaluate(env);

        env.getLocalFrame()[var.getIndexInLocalFrame()] = initObj;
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    @Override
    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addFieldDependency(var, this);
    }

}
