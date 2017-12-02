/*
 * Created on May 20, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Defines bound node that cannot be bound successfully.
 * 
 * @author snshor
 * 
 */
public class ErrorBoundNode extends ABoundNode {

    public ErrorBoundNode(ISyntaxNode node) {
        super(node, new IBoundNode[0]);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#assign(java.lang.Object)
     */
    @Override
    public void assign(Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#evaluateRuntime(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) {
        throw new UnsupportedOperationException("You are trying to run openl code with a compile error in it");
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#getChild()
     */
    @Override
    public IBoundNode[] getChildren() {
        return new IBoundNode[0];
    }

    @Override
    public IBoundNode getTargetNode() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return NullOpenClass.the;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.IBoundNode#isLvalue()
     */
    @Override
    public boolean isLvalue() {
        return false;
    }

}
