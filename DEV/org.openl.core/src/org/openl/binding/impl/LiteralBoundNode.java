/*
 * Created on May 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(java.lang.Object,
     *      java.lang.Object[], org.openl.env.IRuntimeEnv)
     */
    // public Object evaluate(Object target, Object[] pars, IRuntimeEnv env)
    // {
    // return value;
    // }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) {
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
