package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class LocalVarDeclarationNode extends ABoundNode {

    ILocalVar var;

    /**
     * @param syntaxNode
     * @param children
     */
    public LocalVarDeclarationNode(ISyntaxNode syntaxNode, IBoundNode[] children, ILocalVar var) {
        super(syntaxNode, children);

        this.var = var;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object[] init = evaluateChildren(env);

        Object initObj = init == null || init.length == 0 ? null : init[0];

        env.getLocalFrame()[var.getIndexInLocalFrame()] = initObj;
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return JavaOpenClass.VOID;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addFieldDependency(var, this);
    }

}
